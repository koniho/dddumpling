#!/usr/bin/env ruby
require_relative 'store-release-notes'

# Only the selected English promotional field changes; never alter review or release state.
def update_promotional_text(version, text, apple:, write: false)
  raise 'Expected an exact release version' unless /\A\d+\.\d+\.\d+\z/.match?(version)
  raise 'Promotional text must contain 1–170 characters' unless (1..170).cover?(text.length)
  versions = apple.call('get', '/apps/6811478003/appStoreVersions?' + URI.encode_www_form(
    'filter[platform]' => 'IOS', 'filter[versionString]' => version)).fetch('data')
  raise 'Expected exactly one matching iOS version' unless versions.length == 1 && versions[0].dig('attributes', 'versionString') == version
  selected = versions.first
  locations = apple.call('get', "/appStoreVersions/#{selected.fetch('id')}/appStoreVersionLocalizations").fetch('data')
  english = locations.select { |item| item.dig('attributes', 'locale') == 'en-US' }
  raise 'Expected exactly one English localization' unless english.length == 1
  location = english.first
  before = location.fetch('attributes')
  path = "/appStoreVersionLocalizations/#{location.fetch('id')}"
  puts JSON.generate(version: version, state: selected.dig('attributes', 'appStoreState'),
                     before: before['promotionalText'], requested: text, apply: write)
  return unless write
  if before['promotionalText'] != text
    apple.call('patch', path, {data: {type: 'appStoreVersionLocalizations', id: location.fetch('id'),
                                    attributes: {promotionalText: text}}})
  end
  saved = apple.call('get', path).fetch('data').fetch('attributes')
  raise 'Promotional text readback differs' unless saved['promotionalText'] == text
  raise 'Other localization fields changed' unless saved.reject { |k, _| k == 'promotionalText' } == before.reject { |k, _| k == 'promotionalText' }
  puts JSON.generate(verified_version: version, promotionalText: saved['promotionalText'])
end

if $PROGRAM_NAME == __FILE__
  require 'jwt'
  require 'openssl'
  require 'base64'
  raise 'Usage: store-promotional-text.rb [--write]' unless ARGV.empty? || ARGV == ['--write']
  key = OpenSSL::PKey.read(Base64.decode64(ENV.fetch('IOS_APPSTORE_KEY_BASE64')))
  token = JWT.encode({iss: ENV.fetch('IOS_APPSTORE_ISSUER_ID'), iat: Time.now.to_i,
    exp: Time.now.to_i + 1200, aud: 'appstoreconnect-v1'}, key, 'ES256',
    {kid: ENV.fetch('IOS_APPSTORE_KEY_ID'), typ: 'JWT'})
  apple = ->(method, path, body = nil) { request_json('https://api.appstoreconnect.apple.com/v1', token, method, path, body) }
  text = File.read(File.expand_path('../ios/store/en-US/promotional_text.txt', __dir__)).strip
  update_promotional_text(ENV.fetch('IOS_STORE_VERSION'), text, apple: apple, write: ARGV == ['--write'])
end
