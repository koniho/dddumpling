require_relative 'store-promotional-text'

class PromotionalTextTest
  def assert(value)
    raise 'Assertion failed' unless value
  end
  def assert_equal(expected, actual)
    raise "Expected #{expected.inspect}, got #{actual.inspect}" unless expected == actual
  end
  def assert_empty(value); assert(value.empty?); end
  def assert_raises(type)
    begin
      yield
    rescue type
      return
    end
    raise 'Expected rejection'
  end
  def capture_io
    prior = $stdout
    $stdout = StringIO.new
    yield
  ensure
    $stdout = prior
  end
  def setup
    @attrs = {'locale' => 'en-US', 'promotionalText' => 'Old copy', 'whatsNew' => 'Keep release notes', 'description' => 'Keep description'}
    @calls = []
    @versions = [{'id' => 'version-id', 'attributes' => {'versionString' => '0.1.35', 'appStoreState' => 'WAITING_FOR_REVIEW'}}]
    @apple = lambda do |method, path, body = nil|
      @calls << [method, path, body]
      if path.start_with?('/apps/')
        {'data' => @versions}
      elsif path.end_with?('/appStoreVersionLocalizations')
        {'data' => [{'id' => 'english-id', 'attributes' => @attrs.dup}]}
      elsif method == 'patch'
        @attrs.merge!(body.fetch(:data).fetch(:attributes).transform_keys(&:to_s))
        {}
      else
        {'data' => {'attributes' => @attrs.dup}}
      end
    end
  end
  def test_preview_never_writes
    capture_io { update_promotional_text('0.1.35', 'New copy', apple: @apple) }
    assert @calls.all? { |method, _, _| method == 'get' }
  end
  def test_patch_is_scoped_and_verified_without_changing_review
    capture_io { update_promotional_text('0.1.35', 'New copy', apple: @apple, write: true) }
    writes = @calls.reject { |method, _, _| method == 'get' }
    assert_equal 1, writes.length
    assert_equal '/appStoreVersionLocalizations/english-id', writes[0][1]
    assert_equal({promotionalText: 'New copy'}, writes[0][2][:data][:attributes])
    assert_equal 'Keep release notes', @attrs['whatsNew']
    assert_equal '/appStoreVersionLocalizations/english-id', @calls.last[1]
  end
  def test_bad_copy_is_rejected_before_network
    ['', 'x' * 171].each { |text| assert_raises(RuntimeError) { update_promotional_text('0.1.35', text, apple: @apple, write: true) } }
    assert_empty @calls
  end
  def test_missing_or_different_version_never_writes
    [[], [{'id' => 'wrong', 'attributes' => {'versionString' => '0.1.33'}}]].each do |versions|
      @versions = versions
      assert_raises(RuntimeError) { update_promotional_text('0.1.35', 'New copy', apple: @apple, write: true) }
    end
    assert @calls.all? { |method, _, _| method == 'get' }
  end
end

PromotionalTextTest.instance_methods(false).grep(/^test_/).each do |name|
  test = PromotionalTextTest.new
  test.setup
  test.public_send(name)
end
puts 'Promotional text guards passed'
