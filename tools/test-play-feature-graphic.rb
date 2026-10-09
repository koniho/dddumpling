require 'minitest/autorun'
require_relative 'play-feature-graphic'

class FeatureFakeClient
  attr_reader :calls
  attr_accessor :bad_upload, :fail_commit, :existing

  def initialize(existing = [])
    @existing = existing
    @calls = []
    @counter = 0
  end

  def call(method, path, body = nil, media: nil)
    @calls << [method, path, body, media]
    if path == '/edits'
      @counter += 1
      @staged = @existing.dup
      return { 'id' => "edit#{@counter}" }
    end
    if path.include?(':commit')
      raise 'Commit failed' if fail_commit
      @existing = @staged.dup
      return {}
    end
    return {} unless path.include?('/listings/')
    case method
    when 'get' then { 'images' => @staged }
    when 'delete' then @staged = []; {}
    when 'post'
      image = { 'id' => 'new', 'sha256' => bad_upload ? 'wrong' : Digest::SHA256.hexdigest(media) }
      @staged = [image]
      { 'image' => image }
    end
  end
end

class PlayFeatureTests < Minitest::Test
  def setup
    @content = File.binread(File.expand_path('../app-store/google-play/feature-graphic.png', __dir__))
    @sha = Digest::SHA256.hexdigest(@content)
    @client = FeatureFakeClient.new([{ 'id' => 'old', 'sha256' => 'old' }])
    @output = StringIO.new
  end

  def save(write: true, expected: @sha)
    save_play_feature(@client, @content, write: write, expected_sha: expected, output: @output)
  end

  def test_upload_only_selected_slot_and_commit_without_review
    assert_equal 'saved_pending_review', save
    writes = @client.calls.select { |method, path| method != 'get' && path.include?('/listings/') }
    assert_equal ['delete', 'post'], writes.map(&:first)
    assert writes.all? { |_, path| path.start_with?('/edits/edit1/listings/en-US/featureGraphic') }
    commit = @client.calls.find { |_, path| path.include?(':commit') }
    assert_includes commit[1], 'changesNotSentForReview=true'
    assert_includes commit[1], 'changesInReviewBehavior=ERROR_IF_IN_REVIEW'
    assert_nil commit[2]
    assert_equal ['delete', '/edits/edit2'], @client.calls.last.first(2)
  end

  def test_preview_has_no_image_mutations_or_commit
    assert_equal 'preview', save(write: false)
    assert_equal %w[post get delete], @client.calls.map(&:first)
    assert_equal '/edits/edit1', @client.calls.last[1]
  end

  def test_existing_hash_is_idempotent
    @client.existing = [{ 'sha256' => @sha }]
    assert_equal 'already_present', save
    assert_equal %w[post get delete], @client.calls.map(&:first)
  end

  def test_unapproved_file_cannot_open_edit
    assert_raises(RuntimeError) { save(expected: 'different') }
    assert_empty @client.calls
  end

  def test_upload_hash_mismatch_discards_edit_without_commit
    @client.bad_upload = true
    assert_raises(RuntimeError) { save }
    refute @client.calls.any? { |_, path| path.include?(':commit') }
    assert_equal ['delete', '/edits/edit1'], @client.calls.last.first(2)
  end

  def test_failed_commit_discards_edit_and_preserves_previous_image
    @client.fail_commit = true
    assert_raises(RuntimeError) { save }
    assert_equal 'old', @client.existing.first['id']
    assert_equal ['delete', '/edits/edit1'], @client.calls.last.first(2)
  end

  def test_invalid_png_rejected_before_api_access
    @content = @content.dup
    @content.setbyte(25, 6)
    assert_raises(RuntimeError) { save }
    assert_empty @client.calls
    assert_raises(RuntimeError) { feature_png("\x89PNG".b) }
    assert_raises(RuntimeError) { feature_png(@content.byteslice(0, 16)) }
    assert_raises(RuntimeError) { feature_png(@content.byteslice(0, 33)) }
  end

  def test_error_message_preserves_diagnostic_and_redacts_token
    client = PlayFeatureClient.new('private-token')
    message = client.error_message(JSON.generate(error: { message: 'Denied private-token for this app' }))
    assert_equal 'Denied [redacted] for this app', message
    assert_equal 'Non-JSON API error', client.error_message('error html')
  end
end
