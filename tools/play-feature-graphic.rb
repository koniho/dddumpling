#!/usr/bin/env ruby
# Replace only the English feature graphic and keep the change out of review.
require 'digest'
require 'json'
require 'net/http'
require 'stringio'
require 'uri'

class PlayFeatureClient
  API = 'https://androidpublisher.googleapis.com'.freeze
  APP = '/androidpublisher/v3/applications/com.dddumpling.game'.freeze

  def initialize(token)
    @token = token
  end

  def call(method, path, body = nil, media: nil)
    uri = URI(API + (media ? '/upload' : '') + APP + path)
    request = Net::HTTP.const_get(method.capitalize).new(uri)
    request['Authorization'] = "Bearer #{@token}"
    request['Content-Type'] = media ? 'image/png' : 'application/json'
    request.body = media || (JSON.generate(body) if body)
    response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 30, read_timeout: 120) { |http| http.request(request) }
    unless response.is_a?(Net::HTTPSuccess)
      raise "Google Play HTTP #{response.code} for #{method} #{uri.path}"
    end
    response.body.to_s.empty? ? {} : JSON.parse(response.body)
  end
end

def feature_png(content)
  raise 'Expected a 1024 x 500 opaque RGB PNG under 15 MB' unless
    content.bytesize >= 33 && content.bytesize < 15 * 1024 * 1024 &&
    content.start_with?("\x89PNG\r\n\x1a\n".b) && content.byteslice(8, 8) == "\x00\x00\x00\rIHDR".b &&
    content.byteslice(16, 8).unpack('N2') == [1024, 500] && content.byteslice(24, 2).bytes == [8, 2]
  offset = 8
  while offset + 12 <= content.bytesize
    length = content.byteslice(offset, 4).unpack1('N')
    chunk = content.byteslice(offset + 4, 4)
    offset += 12 + length
    break if offset > content.bytesize
    raise 'Feature graphic must not have transparency' if chunk == 'tRNS'
    return Digest::SHA256.hexdigest(content) if chunk == 'IEND'
  end
  raise 'Truncated PNG image'
end

def feature_slot(edit)
  "/edits/#{URI.encode_www_form_component(edit)}/listings/en-US/featureGraphic"
end

def feature_matches(images, sha)
  images.length == 1 && images.first['sha256'].to_s.downcase == sha
end

def discard_feature_edit(client, edit)
  client.call('delete', "/edits/#{URI.encode_www_form_component(edit)}") if edit
end

def save_play_feature(client, content, write: false, expected_sha: nil, output: $stdout)
  sha = feature_png(content)
  raise 'Feature graphic differs from the approved checksum' if expected_sha && expected_sha != sha
  edit = client.call('post', '/edits', {}).fetch('id')
  begin
    images = client.call('get', feature_slot(edit)).fetch('images', [])
    output.puts JSON.generate(package: 'com.dddumpling.game', language: 'en-US', type: 'featureGraphic',
                              requested_sha256: sha, existing: images.map { |image| image.slice('id', 'sha256') })
    return 'already_present' if feature_matches(images, sha)
    return 'preview' unless write
    client.call('delete', feature_slot(edit))
    uploaded = client.call('post', feature_slot(edit) + '?uploadType=media', media: content).fetch('image')
    raise 'Uploaded image checksum does not match the file' unless feature_matches([uploaded], sha)
    staged = client.call('get', feature_slot(edit)).fetch('images', [])
    raise 'Staged feature graphic verification failed' unless feature_matches(staged, sha)
    client.call('post', "/edits/#{URI.encode_www_form_component(edit)}:commit?changesNotSentForReview=true&changesInReviewBehavior=ERROR_IF_IN_REVIEW")
    edit = nil
  ensure
    discard_feature_edit(client, edit)
  end
  verify_edit = client.call('post', '/edits', {}).fetch('id')
  begin
    saved = client.call('get', feature_slot(verify_edit)).fetch('images', [])
    raise 'Saved feature graphic verification failed' unless feature_matches(saved, sha)
    output.puts JSON.generate(status: 'saved_pending_review', image: saved.first.slice('id', 'sha256', 'url'))
  ensure
    discard_feature_edit(client, verify_edit)
  end
  'saved_pending_review'
end

def run_play_feature
  raise 'Usage: play-feature-graphic.rb [--write]' unless ARGV.empty? || ARGV == ['--write']
  content = File.binread(File.expand_path('../app-store/google-play/feature-graphic.png', __dir__))
  sha = feature_png(content)
  expected = ENV.fetch('PLAY_FEATURE_SHA256', '').strip
  raise 'Feature graphic differs from the approved checksum' unless expected.empty? || expected == sha
  require 'googleauth'
  credentials = Google::Auth::ServiceAccountCredentials.make_creds(
    json_key_io: StringIO.new(ENV.fetch('GOOGLE_PLAY_SERVICE_ACCOUNT_JSON')),
    scope: 'https://www.googleapis.com/auth/androidpublisher')
  client = PlayFeatureClient.new(credentials.fetch_access_token!.fetch('access_token'))
  result = save_play_feature(client, content, write: ARGV == ['--write'], expected_sha: expected.empty? ? nil : expected)
  puts "Feature graphic: #{result}."
end

run_play_feature if $PROGRAM_NAME == __FILE__
