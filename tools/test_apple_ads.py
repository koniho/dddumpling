"""Focused checks for signing, report pagination, and credential handling."""
import base64
from datetime import date
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
import io


spec = importlib.util.spec_from_file_location('apple_ads', Path(__file__).with_name('apple-ads.py'))
ads = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ads)


class AppleAdsTests(unittest.TestCase):
    def test_jwt_verifies_with_public_key(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            private, public = root / 'key.pem', root / 'public.pem'
            subprocess.run(['openssl', 'ecparam', '-genkey', '-name', 'prime256v1',
                            '-noout', '-out', str(private)], check=True, capture_output=True)
            subprocess.run(['openssl', 'ec', '-in', str(private), '-pubout',
                            '-out', str(public)], check=True, capture_output=True)
            config = {'client_id': 'SEARCHADS.client', 'team_id': 'SEARCHADS.team',
                      'key_id': 'key-id'}
            with patch.object(ads.time, 'time', return_value=1800000000):
                token = ads.client_secret(config, private)
            header, claims, signature = token.split('.')
            decode = lambda v: base64.urlsafe_b64decode(v + '=' * (-len(v) % 4))
            self.assertEqual(json.loads(decode(header)), {'alg': 'ES256', 'kid': 'key-id'})
            payload = json.loads(decode(claims))
            self.assertEqual(payload['iss'], 'SEARCHADS.team')
            self.assertEqual(payload['sub'], 'SEARCHADS.client')
            self.assertEqual(payload['aud'], 'https://appleid.apple.com')
            self.assertEqual(payload['exp'] - payload['iat'], 3600)
            raw = decode(signature)
            self.assertEqual(len(raw), 64)
            integers = []
            for scalar in (raw[:32], raw[32:]):
                value = scalar.lstrip(b'\0') or b'\0'
                if value[0] & 0x80:
                    value = b'\0' + value
                integers.append(b'\x02' + bytes([len(value)]) + value)
            body = b''.join(integers)
            sigfile = root / 'sig.der'
            sigfile.write_bytes(b'\x30' + bytes([len(body)]) + body)
            verified = subprocess.run(['openssl', 'dgst', '-sha256', '-verify', str(public),
                                       '-signature', str(sigfile)],
                                      input=f'{header}.{claims}'.encode(), capture_output=True)
            self.assertEqual(verified.returncode, 0, verified.stderr)

    def test_single_day_has_no_granularity(self):
        day = date(2026, 10, 6)
        body = ads.report_request(2144830851, day, day, 'countries')
        self.assertNotIn('granularity', body['timeRange'])
        self.assertEqual(body['timeRange']['timeZone'], 'ORTZ')
        self.assertEqual(body['groupBy'], ['countryOrRegion'])
        with self.assertRaises(ValueError):
            ads.report_request(1, day, day.replace(day=5), 'campaign')
        with self.assertRaises(ValueError):
            ads.report_request(1, date(2026, 1, 1), day, 'campaign')

    def test_paginates_without_double_counting_metrics(self):
        client = ads.AppleAds.__new__(ads.AppleAds)
        client.config = {'campaign_id': 2144830851}
        offsets = []
        pages = [list(range(100)), list(range(100, 200)), [200]]
        def call(path, body, account):
            offsets.append(body['pagination']['offset'])
            self.assertEqual(account, 42)
            self.assertEqual(body['timeRange']['timeZone'], 'ORTZ')
            return {'result': {'rows': [{'metadata': {'id': i},
                                        'totalMetrics': {'tapInstalls': 5},
                                        'granularMetrics': [{'tapInstalls': 5}]}
                                       for i in pages.pop(0)]},
                    'pagination': {'totalCount': 201}}
        client.call = call
        rows = client.report('search-terms', date(2026, 10, 6), date(2026, 10, 7), 42)['rows']
        self.assertEqual(offsets, [0, 100, 200])
        self.assertEqual(len(rows), 201)
        self.assertEqual(rows[-1]['totalMetrics']['tapInstalls'], 5)

    def test_empty_report_stops_without_pagination(self):
        client = ads.AppleAds.__new__(ads.AppleAds)
        client.config = {'campaign_id': 1}
        with patch.object(client, 'call', return_value={'result': {'rows': []}}) as call:
            self.assertEqual(client.report('campaign', date(2026, 10, 6),
                                           date(2026, 10, 7), 42), {'rows': []})
            call.assert_called_once()

    def test_client_cannot_change_campaigns(self):
        client = ads.AppleAds.__new__(ads.AppleAds)
        with self.assertRaises(ValueError):
            client.call('/campaigns/2144830851', {'status': 'PAUSED'}, 42)
        with self.assertRaises(ValueError):
            client.call('/reports/apps/campaigns/query', None, 42)

    def test_auth_error_does_not_echo_response_or_credentials(self):
        error = HTTPError(ads.TOKEN_URL, 401, 'Unauthorized', {},
                          io.BytesIO(b'{"client_secret":"DO-NOT-PRINT"}'))
        with patch.object(ads, 'build_opener') as opener:
            opener.return_value.open.side_effect = error
            with self.assertRaisesRegex(ValueError, 'Apple HTTP 401') as raised:
                ads.request_json(ads.TOKEN_URL, {'client_secret': 'DO-NOT-PRINT'}, form=True)
            self.assertNotIn('DO-NOT-PRINT', str(raised.exception))

    def test_private_files_are_not_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'report.json'
            ads.write_private_json(path, {'rows': []})
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            with self.assertRaises(FileExistsError):
                ads.write_private_json(path, {'rows': ['replacement']})
            self.assertEqual(json.loads(path.read_text()), {'rows': []})


if __name__ == '__main__':
    unittest.main()
