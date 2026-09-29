#!/usr/bin/env ruby
# Activate an uploaded closed-test draft without uploading another binary.

def completed_closed_release(track, destination, code)
  raise 'Expected a closed testing track' if destination.empty? || %w[production beta internal].include?(destination) || destination.include?(':')
  raise 'Track response does not match destination' unless track['track'] == destination
  raise 'Expected a positive version code' unless code.match?(/\A[1-9][0-9]*\z/)
  releases = track.fetch('releases', [])
  matches = releases.select { |r| Array(r['versionCodes']).map(&:to_s) == [code] }
  raise 'Requested release was not found exactly once' unless matches.length == 1
  target = matches.first
  raise 'Requested release must be draft or completed' unless %w[draft completed].include?(target['status'])
  releases.each do |release|
    next if release.equal?(target)
    codes = Array(release['versionCodes']).map { |v| Integer(v) }
    raise 'Refusing to replace a newer or unfinished release' unless release['status'] == 'completed' && !codes.empty? && codes.all? { |v| v < code.to_i }
  end
  # The requested build supersedes older completed releases; retain its notes and targeting.
  track.merge('releases' => [target.merge('status' => 'completed')])
end

def activate_play_release
  require_relative 'store-release-notes'
  require 'googleauth'
  code = ENV.fetch('PLAY_VERSION_CODE')
  manifest = REXML::Document.new(File.read(File.expand_path('../AndroidManifest.xml', __dir__))).root
  raise 'Requested code must match the current manifest' unless code == manifest.attributes['android:versionCode']
  track = ENV.fetch('PLAY_CLOSED_TRACK', '').strip
  track = 'alpha' if track.empty?
  credentials = Google::Auth::ServiceAccountCredentials.make_creds(
    json_key_io: StringIO.new(ENV.fetch('GOOGLE_PLAY_SERVICE_ACCOUNT_JSON')),
    scope: 'https://www.googleapis.com/auth/androidpublisher')
  token = credentials.fetch_access_token!.fetch('access_token')
  google = ->(method, path, body = nil) {
    request_json('https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.dddumpling.game', token, method, path, body)
  }
  edit = google.call('post', '/edits', {}).fetch('id')
  begin
    path = "/edits/#{edit}/tracks/#{URI.encode_www_form_component(track)}"
    before = google.call('get', path)
    after = completed_closed_release(before, track, code)
    puts JSON.generate(before: before, requested: after)
    unless before == after
      google.call('put', path, after)
      google.call('post', "/edits/#{edit}:commit?changesNotSentForReview=false", {})
      edit = nil
    end
  ensure
    google.call('delete', "/edits/#{edit}") if edit
  end
  edit = google.call('post', '/edits', {}).fetch('id')
  begin
    saved = google.call('get', "/edits/#{edit}/tracks/#{URI.encode_www_form_component(track)}")
    raise 'Activation readback does not match the requested release' unless saved == completed_closed_release(saved, track, code)
    puts JSON.generate(verified_track: saved)
  ensure
    google.call('delete', "/edits/#{edit}")
  end
end

activate_play_release if $PROGRAM_NAME == __FILE__
