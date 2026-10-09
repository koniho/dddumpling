#!/usr/bin/env python3
"""Fetch DDDumpling performance reports through the Apple Ads Platform API."""
import argparse
import base64
from datetime import date, timedelta
import json
import os
from pathlib import Path
import ssl
import subprocess
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPRedirectHandler, HTTPSHandler, Request, build_opener


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIG = ROOT / '.private/apple-ads/config.json'
API = 'https://api.ads.apple.com/v1'
TOKEN_URL = 'https://appleid.apple.com/auth/oauth2/token'
REPORTS = {
    'campaign': '/reports/apps/campaigns/query',
    'countries': '/reports/apps/campaigns/query',
    'search-terms': '/reports/apps/searchterms/query',
}


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError('Apple redirected an authenticated request; refusing to follow it')


def b64(data):
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('ascii')


def es256_signature(der):
    """Convert OpenSSL's DER ECDSA signature to JWS's two 32-byte integers."""
    if len(der) < 8 or der[0] != 0x30 or der[1] != len(der) - 2:
        raise ValueError('Unexpected OpenSSL signature encoding')
    offset, scalars = 2, []
    for _ in range(2):
        if offset + 2 > len(der) or der[offset] != 2:
            raise ValueError('Invalid ECDSA integer')
        length = der[offset + 1]
        scalar = der[offset + 2:offset + 2 + length]
        if not scalar or len(scalar) != length or scalar[0] & 0x80:
            raise ValueError('Invalid ECDSA integer')
        scalar = scalar.lstrip(b'\0')
        if len(scalar) > 32:
            raise ValueError('Signing key must use the P-256 curve')
        scalars.append(scalar.rjust(32, b'\0'))
        offset += 2 + length
    if offset != len(der):
        raise ValueError('Unexpected ECDSA signature trailing bytes')
    return b''.join(scalars)


def client_secret(config, key_path):
    issued = int(time.time())
    header = {'alg': 'ES256', 'kid': config['key_id']}
    claims = {'sub': config['client_id'], 'iss': config['team_id'],
              'aud': 'https://appleid.apple.com', 'iat': issued, 'exp': issued + 3600}
    signing_input = '.'.join(b64(json.dumps(v, separators=(',', ':')).encode())
                             for v in (header, claims)).encode('ascii')
    signed = subprocess.run(['openssl', 'dgst', '-sha256', '-sign', str(key_path)],
                            input=signing_input, capture_output=True, check=True)
    return signing_input.decode('ascii') + '.' + b64(es256_signature(signed.stdout))


def request_json(url, body=None, headers=None, form=False):
    context = ssl.create_default_context()
    # Homebrew Python may lack a certificate bundle; use the macOS system bundle.
    if Path('/etc/ssl/cert.pem').is_file():
        context.load_verify_locations('/etc/ssl/cert.pem')
    opener = build_opener(HTTPSHandler(context=context), NoRedirects())
    data = None
    request_headers = dict(headers or {})
    if body is not None:
        data = (urlencode(body).encode() if form else json.dumps(body).encode())
        request_headers['Content-Type'] = ('application/x-www-form-urlencoded'
                                           if form else 'application/json')
    req = Request(url, data=data, headers=request_headers)
    for attempt in range(3):
        try:
            with opener.open(req, timeout=45) as response:
                result = json.load(response)
            if result.get('error') or result.get('errors'):
                raise ValueError('Apple returned an API error; no report was accepted')
            return result
        except HTTPError as error:
            if error.code in (429, 500, 502, 503, 504) and attempt < 2:
                time.sleep(2 ** attempt)
                continue
            # Do not echo response bodies, which could include credentials.
            advice = {401: 'check the registered key and client IDs',
                      403: 'check the API role and ad account access'}
            raise ValueError(f'Apple HTTP {error.code}: '
                             + advice.get(error.code, 'request failed')) from None


class AppleAds:
    def __init__(self, config, key_path):
        self.config = config
        token = request_json(TOKEN_URL, {
            'grant_type': 'client_credentials', 'client_id': config['client_id'],
            'client_secret': client_secret(config, key_path), 'scope': 'searchadsorg',
        }, form=True)
        self.token = token['access_token']

    def call(self, path, body=None, ad_account=None):
        # Reporting POSTs query data; this client exposes no campaign mutations.
        if not ((path == '/acls' and body is None)
                or (path in REPORTS.values() and body is not None)):
            raise ValueError('Only account discovery and reporting endpoints are allowed')
        headers = {'Authorization': 'Bearer ' + self.token}
        if ad_account is not None:
            headers['X-AP-Context'] = f'adAccountId={int(ad_account)}'
        return request_json(API + path, body, headers)

    def accounts(self):
        response = self.call('/acls')
        accounts = response.get('result', {}).get('acls')
        if not isinstance(accounts, list):
            raise ValueError('Unexpected Apple account-discovery response')
        return accounts

    def report(self, kind, start, end, ad_account):
        body = report_request(self.config['campaign_id'], start, end, kind)
        rows = []
        while True:
            response = self.call(REPORTS[kind], body, ad_account)
            page = response.get('result', {}).get('rows')
            if not isinstance(page, list):
                raise ValueError('Unexpected Apple report response')
            rows.extend(page)
            offset = body['pagination']['offset'] + len(page)
            total = response.get('pagination', {}).get('totalCount')
            if not page or (total is not None and offset >= int(total)):
                break
            if total is None and len(page) < body['pagination']['pageSize']:
                break
            body['pagination']['offset'] = offset
        return {'rows': rows}


def report_request(campaign_id, start, end, kind):
    if start > end or (end - start).days >= 90:
        raise ValueError('Report dates must be ordered and span at most 90 days')
    body = {
        'pagination': {'offset': 0, 'pageSize': 100},
        'filters': [{'field': 'campaignId', 'operator': 'EQUALS',
                     'value': [str(campaign_id)]}],
        'timeRange': {'start': start.isoformat(), 'end': end.isoformat(),
                      'timeZone': 'ORTZ'},
    }
    if start != end:
        body['timeRange']['granularity'] = 'DAILY'
    if kind == 'countries':
        body['groupBy'] = ['countryOrRegion']
    return body


def write_private_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    # An exclusive creation prevents overwriting an existing report or config.
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as output:
        json.dump(value, output, indent=2)
        output.write('\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['accounts', 'fetch'])
    parser.add_argument('--config', type=Path, default=DEFAULT_CONFIG)
    parser.add_argument('--ad-account', type=int)
    parser.add_argument('--start', type=date.fromisoformat)
    parser.add_argument('--end', type=date.fromisoformat)
    parser.add_argument('--out', type=Path)
    args = parser.parse_args()
    config = json.loads(args.config.read_text())
    missing = [key for key in ('client_id', 'team_id', 'key_id') if not config.get(key)]
    if missing:
        raise ValueError('Register public-key.pem in Apple Ads Account Settings > API, '
                         'then fill config.json: ' + ', '.join(missing))
    key_path = args.config.parent / 'private-key.pem'
    end = args.end or date.today() - timedelta(days=1)
    start = args.start or end - timedelta(days=6)
    report_request(config['campaign_id'], start, end, 'campaign')
    client = AppleAds(config, key_path)
    accounts = client.accounts()
    if args.action == 'accounts':
        print(json.dumps(accounts, indent=2))
        return
    ad_account = args.ad_account or config.get('ad_account_id')
    if ad_account is None:
        if len(accounts) != 1:
            raise ValueError('Run accounts, then select --ad-account from the listed IDs')
        ad_account = accounts[0]['adAccount']['id']
    if not any(int(a['adAccount']['id']) == int(ad_account) for a in accounts):
        raise ValueError('The selected ad account is not accessible to this API user')
    out = args.out or args.config.parent / 'reports' / f'{start}_{end}_{time.time_ns()}'
    if out.exists():
        raise ValueError('Output folder already exists; choose a new --out')
    out.mkdir(parents=True, mode=0o700)
    write_private_json(out / 'period.json', {
        'campaign_id': config['campaign_id'], 'ad_account_id': ad_account,
        'start': start.isoformat(), 'end': end.isoformat(), 'time_zone': 'ORTZ',
        'fetched_at_unix': int(time.time()),
    })
    for kind in REPORTS:
        result = client.report(kind, start, end, ad_account)
        write_private_json(out / f'{kind}.json', result)
        print(f'{kind}: {len(result["rows"])} rows saved')
    print(f'Reports: {out}')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, OSError, URLError, subprocess.CalledProcessError) as error:
        print(f'Apple Ads: {error}', file=sys.stderr)
        sys.exit(1)
