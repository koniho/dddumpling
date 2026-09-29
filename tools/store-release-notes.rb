#!/usr/bin/env ruby
# Update notes only; never upload binaries, submit for review, or change release status.
require 'json'
require 'net/http'
require 'uri'
require 'stringio'
require 'rexml/document'

def request_json(base, token, method, path, body = nil)
  uri = URI(base + path)
  request = Net::HTTP.const_get(method.capitalize).new(uri)
  request['Authorization'] = "Bearer #{token}"
  request['Content-Type'] = 'application/json'
  request.body = JSON.generate(body) if body
  response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 30, read_timeout: 120) { |http| http.request(request) }
  raise "#{method} #{uri.path}: #{response.code} #{response.body}" unless response.is_a?(Net::HTTPSuccess)
  response.body.to_s.empty? ? {} : JSON.parse(response.body)
end

def with_english_note(release, code, notes)
  raise 'Refusing to edit an active or different Play release' unless release['status'] == 'draft' && release['versionCodes'].map(&:to_s) == [code]
  copy = Marshal.load(Marshal.dump(release))
  copy['releaseNotes'] = Array(copy['releaseNotes']).reject { |note| note['language'] == 'en-US' } + [{ 'language' => 'en-US', 'text' => notes }]
  copy
end

def run_store_notes
  require 'googleauth'
  require 'jwt'
  require 'openssl'
  require 'base64'
  write = ARGV == ['--write']
  raise 'Usage: store-release-notes.rb [--write]' unless ARGV.empty? || write
  root = File.expand_path('..', __dir__)
  manifest = REXML::Document.new(File.read(File.join(root, 'AndroidManifest.xml'))).root
  version = manifest.attributes['android:versionName']
  code = manifest.attributes['android:versionCode']
  notes = File.read(File.join(root, 'app-store/google-play/en-US/changelogs', "#{code}.txt")).strip
  raise 'Notes must contain 1–500 characters' unless (1..500).cover?(notes.length)
  raise 'iOS and Play notes differ' unless notes == File.read(File.join(root, 'ios/store/en-US/what_to_test.txt')).strip
  puts JSON.generate(mode: write ? 'update' : 'inspect', version: version, code: code, approved_notes: notes)

  credentials = Google::Auth::ServiceAccountCredentials.make_creds(json_key_io: StringIO.new(ENV.fetch('GOOGLE_PLAY_SERVICE_ACCOUNT_JSON')), scope: 'https://www.googleapis.com/auth/androidpublisher')
  google_token = credentials.fetch_access_token!.fetch('access_token')
  google = ->(method, path, body = nil) { request_json('https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.dddumpling.game', google_token, method, path, body) }
  edit = google.call('post', '/edits', {})['id']
  begin
    tracks = google.call('get', "/edits/#{edit}/tracks").fetch('tracks', [])
    found = false
    changed = false
    tracks.each do |track|
      Array(track['releases']).each_with_index do |release, index|
        next unless Array(release['versionCodes']).map(&:to_s).include?(code)
        found = true
        puts JSON.generate(platform: 'Play', track: track['track'], release: release)
        updated = with_english_note(release, code, notes)
        next unless write && updated != release
        track['releases'][index] = updated
        google.call('put', "/edits/#{edit}/tracks/#{URI.encode_www_form_component(track['track'])}", track)
        changed = true
      end
    end
    raise "Play code #{code} not found" unless found
    if changed
      google.call('post', "/edits/#{edit}:commit?changesNotSentForReview=true", {})
      edit = nil
      puts 'Play draft notes updated without sending for review.'
    end
  ensure
    google.call('delete', "/edits/#{edit}") if edit
  end

  key = OpenSSL::PKey.read(Base64.decode64(ENV.fetch('IOS_APPSTORE_KEY_BASE64')))
  token = JWT.encode({ iss: ENV.fetch('IOS_APPSTORE_ISSUER_ID'), iat: Time.now.to_i, exp: Time.now.to_i + 1200, aud: 'appstoreconnect-v1' }, key, 'ES256', { kid: ENV.fetch('IOS_APPSTORE_KEY_ID'), typ: 'JWT' })
  apple = ->(method, path, body = nil) { request_json('https://api.appstoreconnect.apple.com/v1', token, method, path, body) }
  app = apple.call('get', '/apps?' + URI.encode_www_form('filter[bundleId]' => 'com.dddumpling.game.ios')).fetch('data').first
  raise 'iOS app not found' unless app
  versions = apple.call('get', "/apps/#{app['id']}/appStoreVersions?" + URI.encode_www_form('filter[platform]' => 'IOS', 'limit' => 50)).fetch('data')
  versions.each do |entry|
    puts JSON.generate(platform: 'App Store', id: entry['id'], attributes: entry['attributes'])
    localizations = apple.call('get', "/appStoreVersions/#{entry['id']}/appStoreVersionLocalizations").fetch('data')
    localizations.each { |localization| puts JSON.generate(platform: 'App Store notes', version: entry['attributes']['versionString'], id: localization['id'], attributes: localization['attributes'].slice('locale', 'whatsNew')) }
    next unless write && entry['attributes']['versionString'] == version
    raise 'App Store version is not an editable draft' unless %w[PREPARE_FOR_SUBMISSION DEVELOPER_REJECTED REJECTED METADATA_REJECTED].include?(entry['attributes']['appStoreState'])
    english = localizations.find { |localization| localization['attributes']['locale'] == 'en-US' }
    raise 'App Store English localization missing' unless english
    if english['attributes']['whatsNew'] != notes
      apple.call('patch', "/appStoreVersionLocalizations/#{english['id']}", { data: { type: 'appStoreVersionLocalizations', id: english['id'], attributes: { whatsNew: notes } } })
    end
    saved = apple.call('get', "/appStoreVersionLocalizations/#{english['id']}").fetch('data')
    raise 'App Store notes verification failed' unless saved['attributes']['whatsNew'] == notes
    puts "App Store #{version} draft notes verified."
  end
  builds = apple.call('get', '/builds?' + URI.encode_www_form('filter[app]' => app['id'], 'sort' => '-uploadedDate', 'limit' => 5, 'include' => 'preReleaseVersion')).fetch('data')
  builds.each do |build|
    puts JSON.generate(platform: 'TestFlight', id: build['id'], attributes: build['attributes'].slice('version', 'processingState', 'uploadedDate'))
    localizations = apple.call('get', "/builds/#{build['id']}/betaBuildLocalizations").fetch('data')
    localizations.each { |loc| puts JSON.generate(platform: 'TestFlight notes', build: build['attributes']['version'], id: loc['id'], attributes: loc['attributes']) }
    next unless write && build == builds.first
    prerelease = apple.call('get', "/builds/#{build['id']}/preReleaseVersion").fetch('data')
    raise 'Latest TestFlight build is a different release' unless prerelease['attributes']['version'] == version
    english = localizations.find { |loc| loc['attributes']['locale'] == 'en-US' }
    raise 'TestFlight English localization missing' unless english
    if english['attributes']['whatsNew'] != notes
      apple.call('patch', "/betaBuildLocalizations/#{english['id']}", { data: { type: 'betaBuildLocalizations', id: english['id'], attributes: { whatsNew: notes } } })
    end
    saved = apple.call('get', "/betaBuildLocalizations/#{english['id']}").fetch('data')
    raise 'TestFlight notes verification failed' unless saved['attributes']['whatsNew'] == notes
    puts "TestFlight #{version} (#{build['attributes']['version']}) notes verified."
  end
  raise "No App Store draft exists for #{version}" if write && versions.none? { |entry| entry['attributes']['versionString'] == version }
end

run_store_notes if $PROGRAM_NAME == __FILE__
