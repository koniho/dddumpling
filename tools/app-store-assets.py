#!/usr/bin/env python3
"""Inspect or upload promotional PNGs to App Store Connect's Asset Library."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import ssl
import struct
import subprocess
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import HTTPSHandler, Request, build_opener

ROOT = Path(__file__).resolve().parents[1]
API = 'https://api.appstoreconnect.apple.com'
DEFAULT_CONFIG = ROOT / '.private/app-store-connect/config.json'
module_spec = importlib.util.spec_from_file_location('apple_ads', ROOT / 'tools/apple-ads.py')
signing = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(signing)


def load_config(path):
    config = json.loads(path.read_text()) if path.exists() else {}
    for key, name in (('key_id', 'IOS_APPSTORE_KEY_ID'), ('issuer_id', 'IOS_APPSTORE_ISSUER_ID'),
                      ('key_file', 'IOS_APPSTORE_KEY_FILE')):
        if os.environ.get(name):
            config[key] = os.environ[name]
    missing = [key for key in ('key_id', 'issuer_id', 'key_file') if not config.get(key)]
    if missing:
        raise ValueError('Set App Store Connect credentials in local config or environment: '
                         + ', '.join(missing))
    key_file = Path(config['key_file']).expanduser()
    config['key_file'] = key_file if key_file.is_absolute() else path.parent / key_file
    return config


def access_token(config):
    issued = int(time.time())
    header = {'alg': 'ES256', 'kid': config['key_id'], 'typ': 'JWT'}
    claims = {'iss': config['issuer_id'], 'iat': issued, 'exp': issued + 900,
              'aud': 'appstoreconnect-v1'}
    message = '.'.join(signing.b64(json.dumps(value, separators=(',', ':')).encode())
                       for value in (header, claims))
    signature = subprocess.run(['openssl', 'dgst', '-sha256', '-sign', str(config['key_file'])],
                               input=message.encode(), capture_output=True, check=True).stdout
    return message + '.' + signing.b64(signing.es256_signature(signature))


def png_dimensions(content):
    """Inspect PNG chunks without modifying the image; reject alpha and palette transparency."""
    if len(content) < 33 or content[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('Expected a PNG image')
    if content[8:16] != b'\x00\x00\x00\rIHDR':
        raise ValueError('Invalid PNG header')
    width, height, _, color_type = struct.unpack('>IIBB', content[16:26])
    if color_type in (4, 6):
        raise ValueError('App Store creative images cannot have an alpha channel')
    offset = 8
    while offset + 12 <= len(content):
        length = struct.unpack('>I', content[offset:offset + 4])[0]
        chunk = content[offset + 4:offset + 8]
        offset += 12 + length
        if offset > len(content):
            break
        if chunk == b'tRNS':
            raise ValueError('App Store creative images cannot include transparency')
        if chunk == b'IEND':
            return width, height
    raise ValueError('Truncated PNG image')


def matching_spec(catalog, content):
    width, height = png_dimensions(content)
    placements = {'PRODUCT_PAGE_HEADER_ASSET', 'APP_STORE_SEARCH_RESULTS_ASSET'}
    for item in catalog['data']:
        for spec in item['attributes']['imageSpecs']:
            bounds = spec['dimensions']
            if (bounds['minWidth'] <= width <= bounds['maxWidth']
                    and bounds['minHeight'] <= height <= bounds['maxHeight']
                    and '.png' in spec['fileExtensions']
                    and len(content) <= spec['maxFileSize']
                    and placements.intersection(spec['compatiblePlacementTypes'])):
                return spec
    raise ValueError('PNG does not match an accepted Header or Search Results specification')


def asset_summary(response):
    item = response['data']
    return {'id': item['id'], **{key: item['attributes'].get(key) for key in
            ('fileName', 'referenceName', 'category', 'state', 'stateDetails', 'specId', 'imageAsset')}}


class AppStoreConnect:
    def __init__(self, config):
        self.token = access_token(config)
        context = ssl.create_default_context()
        if Path('/etc/ssl/cert.pem').is_file():
            context.load_verify_locations('/etc/ssl/cert.pem')
        self.opener = build_opener(HTTPSHandler(context=context), signing.NoRedirects())

    def call(self, method, path, payload=None):
        if not path.startswith('/v1/') or '://' in path:
            raise ValueError('Expected an App Store Connect API path')
        request = Request(API + path, method=method,
                          data=None if payload is None else json.dumps(payload).encode(),
                          headers={'Authorization': 'Bearer ' + self.token,
                                   'Content-Type': 'application/json'})
        try:
            with self.opener.open(request, timeout=45) as response:
                return json.load(response)
        except HTTPError as error:
            raise ValueError(f'App Store Connect HTTP {error.code} for {method} {path.split("?")[0]}') from None

    def images(self, library_id, reference_name=None):
        query = {'limit': 200}
        if reference_name:
            query['filter[referenceName]'] = reference_name
        path = f'/v1/appAssetLibraries/{library_id}/images?' + urlencode(query)
        items = []
        while path:
            page = self.call('GET', path)
            items.extend(page['data'])
            next_url = page.get('links', {}).get('next')
            if next_url and not next_url.startswith(API + '/v1/'):
                raise ValueError('Unexpected pagination destination')
            path = next_url[len(API):] if next_url else None
        return items

    def upload_parts(self, operations, content):
        offset = 0
        for operation in sorted(operations, key=lambda value: value['offset']):
            if operation['offset'] != offset or operation['length'] <= 0:
                raise ValueError('Upload parts have a gap, overlap, or invalid length')
            offset += operation['length']
        if offset != len(content):
            raise ValueError('Upload parts do not cover the complete file')
        for operation in operations:
            url = urlsplit(operation['url'])
            if (url.scheme != 'https' or not (url.hostname or '').endswith('.apple.com')
                    or url.username or url.password or url.port not in (None, 443)):
                raise ValueError('Unexpected Apple upload destination')
            start, length = operation['offset'], operation['length']
            # Upload operations use temporary URLs, never the API's bearer token.
            headers = {item['name']: item['value'] for item in operation['requestHeaders']}
            if any(name.lower() == 'authorization' for name in headers):
                raise ValueError('Unexpected authorization header on an upload operation')
            request = Request(operation['url'], method=operation['method'],
                              data=content[start:start + length], headers=headers)
            try:
                with self.opener.open(request, timeout=45) as response:
                    response.read()
            except HTTPError as error:
                raise ValueError(f'Image part upload failed: HTTP {error.code}') from None


def upload_image(client, app_id, image, reference_name, state_dir):
    content = image.read_bytes()
    png_dimensions(content)
    library_id = client.call('GET', f'/v1/apps/{app_id}/assetLibrary')['data']['id']
    catalog = client.call('GET', '/v1/appAssetLibraryRefData?'
                          + urlencode({'fields[appAssetLibraryRefData]': 'imageSpecs'}))
    specification = matching_spec(catalog, content)
    if client.images(library_id, reference_name):
        raise ValueError('Reference name already exists; inspect the asset or choose a new version name')
    reservation = client.call('POST', '/v1/appAssetLibraryImages', {'data': {
        'type': 'appAssetLibraryImages', 'attributes': {'fileName': image.name,
            'fileSize': len(content), 'category': 'CREATIVE_ASSETS', 'referenceName': reference_name},
        'relationships': {'assetLibrary': {'data': {'type': 'appAssetLibraries', 'id': library_id}}}}})
    asset_id = reservation['data']['id']
    state_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    signing.write_private_json(state_dir / f'{asset_id}-reservation.json', reservation)
    client.upload_parts(reservation['data']['attributes']['uploadOperations'], content)
    committed = client.call('PATCH', f'/v1/appAssetLibraryImages/{asset_id}', {'data': {
        'type': 'appAssetLibraryImages', 'id': asset_id, 'attributes': {'uploaded': True}}})
    summary = asset_summary(committed)
    summary['expectedSpecId'] = specification['specId']
    signing.write_private_json(state_dir / f'{asset_id}-uploaded.json', summary)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['inspect', 'upload', 'poll'])
    parser.add_argument('--config', type=Path, default=DEFAULT_CONFIG)
    parser.add_argument('--app-id', default='6811478003')
    parser.add_argument('--image', type=Path)
    parser.add_argument('--name', help='Unique internal reference name for this version')
    parser.add_argument('--asset-id')
    parser.add_argument('--state-dir', type=Path, default=ROOT / '.private/app-store-assets')
    args = parser.parse_args()
    if args.action == 'upload' and (not args.image or not args.name):
        parser.error('upload requires --image and --name')
    if args.action == 'poll' and not args.asset_id:
        parser.error('poll requires --asset-id')
    client = AppStoreConnect(load_config(args.config))
    if args.action == 'upload':
        result = upload_image(client, args.app_id, args.image, args.name, args.state_dir)
    elif args.action == 'poll':
        result = asset_summary(client.call('GET', f'/v1/appAssetLibraryImages/{args.asset_id}'))
        if result['state'] == 'FAILED':
            raise ValueError('Apple image processing failed: ' + json.dumps(result['stateDetails']))
    else:
        app = client.call('GET', f'/v1/apps/{args.app_id}')['data']
        library = client.call('GET', f'/v1/apps/{args.app_id}/assetLibrary')['data']
        result = {'app': {'id': app['id'], 'name': app['attributes']['name']},
                  'libraryId': library['id'],
                  'images': [asset_summary({'data': image}) for image in client.images(library['id'], args.name)]}
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, URLError, subprocess.CalledProcessError) as error:
        # Network exceptions can contain signed upload URLs; print a stable message instead.
        message = str(error) if isinstance(error, ValueError) else type(error).__name__
        print('Error: ' + message, file=sys.stderr)
        sys.exit(1)
