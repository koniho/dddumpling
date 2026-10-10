#!/usr/bin/env ruby

def production_release(source, production, code, notes)
  raise 'Expected production destination' unless production['track'] == 'production'
  matches = Array(source['releases']).select { |r| Array(r['versionCodes']).map(&:to_s) == [code] }
  raise 'Expected exactly one uploaded release' unless matches.length == 1
  target = matches.first
  raise 'Source must be draft or completed' unless %w[draft completed].include?(target['status'])
  english = Array(target['releaseNotes']).find { |n| n['language'] == 'en-US' }
  raise 'Uploaded notes differ from approved notes' unless english && english['text'].strip == notes
  Array(production['releases']).each do |release|
    raise 'Refusing to replace a newer or unfinished production release' unless release['status'] == 'completed' && !Array(release['versionCodes']).empty? && release['versionCodes'].all? { |v| Integer(v) <= Integer(code) }
  end
  {'track' => 'production', 'releases' => [target.merge('status' => 'completed')]}
end

def promote_play_production
  require_relative 'store-release-notes'
  require 'googleauth'
  code = ENV.fetch('PLAY_VERSION_CODE')
  root = File.expand_path('..', __dir__)
  manifest = REXML::Document.new(File.read(File.join(root, 'AndroidManifest.xml'))).root
  raise 'Requested code must match manifest' unless code == manifest.attributes['android:versionCode']
  notes = File.read(File.join(root, "app-store/google-play/en-US/changelogs/#{code}.txt")).strip
  credentials = Google::Auth::ServiceAccountCredentials.make_creds(json_key_io: StringIO.new(ENV.fetch('GOOGLE_PLAY_SERVICE_ACCOUNT_JSON')), scope: 'https://www.googleapis.com/auth/androidpublisher')
  token = credentials.fetch_access_token!.fetch('access_token')
  google = ->(method, path, body = nil) { request_json('https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.dddumpling.game', token, method, path, body) }
  source_track = ENV.fetch('PLAY_CLOSED_TRACK', '').strip
  source_track = 'alpha' if source_track.empty?
  raise 'Expected a closed source track' if %w[production beta internal].include?(source_track) || source_track.include?(':')
  edit = google.call('post', '/edits', {}).fetch('id')
  begin
    source = google.call('get', "/edits/#{edit}/tracks/#{URI.encode_www_form_component(source_track)}")
    before = google.call('get', "/edits/#{edit}/tracks/production")
    after = production_release(source, before, code, notes)
    puts JSON.generate(before: before, requested: after, apply: ENV['APPLY'] == 'true')
    if ENV['APPLY'] == 'true' && before != after
      google.call('put', "/edits/#{edit}/tracks/production", after)
      google.call('post', "/edits/#{edit}:validate", {})
      google.call('post', "/edits/#{edit}:commit?changesNotSentForReview=false", {})
      edit = nil
    end
  ensure
    google.call('delete', "/edits/#{edit}") if edit
  end
  return unless ENV['APPLY'] == 'true'
  edit = google.call('post', '/edits', {}).fetch('id')
  begin
    saved = google.call('get', "/edits/#{edit}/tracks/production")
    raise 'Production readback differs' unless saved == after
    puts JSON.generate(verified_production: saved)
  ensure
    google.call('delete', "/edits/#{edit}")
  end
end
promote_play_production if $PROGRAM_NAME == __FILE__
