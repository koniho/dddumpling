# Apple Ads reporting

`tools/apple-ads.py` fetches campaign, country, and search-term reports for
DDDumpling's `First Launch` campaign (`2144830851`) through the current
Apple Ads Platform API. It exposes account discovery and reporting only.

## Register the local key

The local key pair and configuration live in ignored `.private/apple-ads/`.
The private key stays on this machine; register `public-key.pem` with Apple:

For a new checkout, create that local pair with OpenSSL:

```sh
mkdir -p .private/apple-ads
chmod 700 .private/apple-ads
openssl ecparam -genkey -name prime256v1 -noout -out .private/apple-ads/private-key.pem
chmod 600 .private/apple-ads/private-key.pem
openssl ec -in .private/apple-ads/private-key.pem -pubout -out .private/apple-ads/public-key.pem
```

Use these commands only when no registered pair exists. Replacing a private key makes
its existing Apple registration unusable until the new public key is registered.
Create `.private/apple-ads/config.json` with the returned IDs:

```json
{
  "client_id": "",
  "team_id": "",
  "key_id": "",
  "campaign_id": 2144830851,
  "ad_account_id": null
}
```

1. Sign in to Apple Ads as an API-enabled user.
2. Open Account Settings → API, paste the public key including its BEGIN/END
   lines, and save.
3. Enter Apple's `clientId`, `teamId`, and `keyId` into `config.json` as
   `client_id`, `team_id`, and `key_id`. These are Apple Ads credentials;
   App Store Connect keys cannot substitute for them.

If the key field is missing, an Account Admin needs to assign an API role.
API Account Read Only is sufficient for these reports.
An existing user can be assigned an API role; using a second Apple Account you control
preserves the original account's admin access. The admin API tab shows third-party service
provider grants, while public-key registration requires an API user role.

## Fetch results

Use Python 3 and OpenSSL. No Python packages are required.

```sh
python3 tools/apple-ads.py accounts
python3 tools/apple-ads.py fetch --start 2026-10-06 --end 2026-10-07
```

If multiple ad accounts are listed, pass `--ad-account ID` to `fetch` or set
`ad_account_id` in the local configuration. Without dates, the client requests
the last seven days through yesterday. Dates use the ad account's reporting
timezone (`ORTZ`); pass explicit dates near a timezone boundary. The client
paginates results and saves reports under `.private/apple-ads/reports/`.

Use `totalMetrics` for each row's period totals. `granularMetrics` contains the
daily breakdown; do not add both together. Tap-through CPA is spend divided by
`tapInstalls`. New-player acquisition can instead use `tapNewDownloads` to
exclude redownloads. Keep currencies separate. Low-volume search terms may be
suppressed, so search-term totals may not reconcile with campaign totals.
Recent attributed installs can still arrive after the reporting day.

The key-registration step requires a signed-in Apple Ads user. Until Apple has
issued the three IDs, local checks can validate signing and request handling,
but cannot validate account access or fetch live metrics.

References: [OAuth setup](https://developer.apple.com/documentation/apple-ads-platform-api/implementing-oauth-for-the-apple-ads-platform-api),
[campaign reports](https://developer.apple.com/documentation/apple-ads-platform-api/get-app-campaign-reports),
[search-term reports](https://developer.apple.com/documentation/apple-ads-platform-api/get-app-search-term-reports).
