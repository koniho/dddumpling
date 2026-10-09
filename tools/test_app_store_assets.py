"""Contract checks for creative uploads; no Apple account or network access required."""
import base64
import importlib.util
import io
import json
from pathlib import Path
import struct
import subprocess
import tempfile
import unittest
from unittest.mock import MagicMock, Mock
from urllib.error import HTTPError
import zlib

spec = importlib.util.spec_from_file_location('app_store_assets', Path(__file__).with_name('app-store-assets.py'))
assets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assets)


def png(color_type=2, transparent=False):
    def chunk(name, data):
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', zlib.crc32(name + data))
    header = struct.pack('>IIBBBBB', 1, 1, 8, color_type, 0, 0, 0)
    transparency = chunk(b'tRNS', b'\0\0\0\0\0\0') if transparent else b''
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header) + transparency
            + chunk(b'IDAT', zlib.compress(b'\0\xff\xff\xff')) + chunk(b'IEND', b''))


CATALOG = {'data': [{'attributes': {'imageSpecs': [{
    'specId': 'universal-spec', 'dimensions': {'minWidth': 1, 'maxWidth': 1,
    'minHeight': 1, 'maxHeight': 1}, 'fileExtensions': ['.png'], 'maxFileSize': 1000,
    'compatiblePlacementTypes': ['PRODUCT_PAGE_HEADER_ASSET', 'APP_STORE_SEARCH_RESULTS_ASSET']}]}}]}


class AssetTests(unittest.TestCase):
    def test_png_alpha_palette_transparency_and_truncation_rejected(self):
        self.assertEqual(assets.png_dimensions(png()), (1, 1))
        for content in (png(6), png(2, True), png()[:-6], b'not a PNG'):
            with self.subTest(content=content[:10]), self.assertRaises(ValueError):
                assets.png_dimensions(content)

    def test_multipart_uses_exact_bytes_without_api_bearer_token(self):
        client = assets.AppStoreConnect.__new__(assets.AppStoreConnect)
        client.token = 'PRIVATE-TOKEN'
        client.opener = MagicMock()
        operations = [{'offset': offset, 'length': 3, 'method': 'PUT',
            'url': f'https://store-1.blobstore.apple.com/part{offset}',
            'requestHeaders': [{'name': 'Content-Type', 'value': 'image/png'}]}
            for offset in (3, 0)]
        client.upload_parts(operations, b'abcdef')
        requests = [call.args[0] for call in client.opener.open.call_args_list]
        self.assertEqual([request.data for request in requests], [b'def', b'abc'])
        self.assertTrue(all(request.get_method() == 'PUT' for request in requests))
        self.assertTrue(all(not request.has_header('Authorization') for request in requests))
        client.opener.reset_mock()
        with self.assertRaises(ValueError):
            client.upload_parts(operations[:1], b'abcdef')
        client.opener.open.assert_not_called()

    def test_upload_category_commit_and_private_receipt(self):
        content = png()
        client = Mock()
        client.images.return_value = []
        reservation = {'data': {'id': 'asset-1', 'attributes': {'uploadOperations': ['parts']}}}
        committed = {'data': {'id': 'asset-1', 'attributes': {'state': 'UPLOAD_COMPLETE'}}}
        client.call.side_effect = [{'data': {'id': 'library-1'}}, CATALOG, reservation, committed]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            image = root / 'creative.png'
            image.write_bytes(content)
            summary = assets.upload_image(client, 'app-1', image, 'Creative v2', root / 'state')
            create = client.call.call_args_list[2].args[2]['data']
            self.assertEqual(create['attributes']['category'], 'CREATIVE_ASSETS')
            self.assertEqual(create['attributes']['fileSize'], len(content))
            self.assertEqual(create['relationships']['assetLibrary']['data']['id'], 'library-1')
            client.upload_parts.assert_called_once_with(['parts'], content)
            commit = client.call.call_args_list[3].args[2]['data']['attributes']
            self.assertEqual(commit, {'uploaded': True})
            self.assertEqual(summary['expectedSpecId'], 'universal-spec')
            self.assertEqual((root / 'state/asset-1-reservation.json').stat().st_mode & 0o777, 0o600)

    def test_duplicate_name_does_not_create_or_replace_an_asset(self):
        client = Mock()
        client.images.return_value = [{'id': 'existing'}]
        client.call.side_effect = [{'data': {'id': 'library-1'}}, CATALOG]
        with tempfile.TemporaryDirectory() as directory:
            image = Path(directory) / 'creative.png'
            image.write_bytes(png())
            with self.assertRaisesRegex(ValueError, 'already exists'):
                assets.upload_image(client, 'app-1', image, 'Existing name', Path(directory) / 'state')
        self.assertTrue(all(call.args[0] == 'GET' for call in client.call.call_args_list))
        client.upload_parts.assert_not_called()

    def test_paginated_library_rejects_foreign_destinations(self):
        client = assets.AppStoreConnect.__new__(assets.AppStoreConnect)
        client.call = Mock(side_effect=[{'data': [{'id': 'a'}], 'links': {'next': assets.API + '/v1/page2'}},
                                       {'data': [{'id': 'b'}], 'links': {}}])
        self.assertEqual(client.images('library'), [{'id': 'a'}, {'id': 'b'}])
        client.call = Mock(return_value={'data': [], 'links': {'next': 'https://example.com/page2'}})
        with self.assertRaises(ValueError):
            client.images('library')
        self.assertEqual(client.call.call_count, 1)

    def test_connect_token_uses_connect_audience_and_local_key(self):
        with tempfile.TemporaryDirectory() as directory:
            key = Path(directory) / 'key.pem'
            subprocess.run(['openssl', 'ecparam', '-genkey', '-name', 'prime256v1',
                            '-noout', '-out', str(key)], capture_output=True, check=True)
            token = assets.access_token({'key_id': 'key-1', 'issuer_id': 'issuer-1', 'key_file': key})
            header, claims, signature = token.split('.')
            decode = lambda text: base64.urlsafe_b64decode(text + '=' * (-len(text) % 4))
            self.assertEqual(json.loads(decode(header))['kid'], 'key-1')
            values = json.loads(decode(claims))
            self.assertEqual(values['aud'], 'appstoreconnect-v1')
            self.assertEqual(values['iss'], 'issuer-1')
            self.assertEqual(values['exp'] - values['iat'], 900)
            self.assertEqual(len(decode(signature)), 64)

    def test_http_error_omits_response_body_and_authentication(self):
        client = assets.AppStoreConnect.__new__(assets.AppStoreConnect)
        client.token = 'PRIVATE-TOKEN'
        client.opener = Mock()
        client.opener.open.side_effect = HTTPError(assets.API, 401, 'Unauthorized', {},
                                                  io.BytesIO(b'PRIVATE-TOKEN'))
        with self.assertRaisesRegex(ValueError, 'HTTP 401') as caught:
            client.call('GET', '/v1/apps/app-1')
        self.assertNotIn('PRIVATE-TOKEN', str(caught.exception))


if __name__ == '__main__':
    unittest.main()
