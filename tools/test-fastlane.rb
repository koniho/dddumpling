# Offline checks: exercise the lane's upload options without contacting Google.
require "tmpdir"
require "fileutils"
module UI
  def self.user_error!(message); raise ArgumentError, message; end
end
def default_platform(*); end
def platform(*); yield; end
def desc(*); end
def lane(*); end
def import(path); load File.expand_path("../fastlane/#{path}", __dir__); end
load File.expand_path("../fastlane/Fastfile", __dir__)

def assert(value, message)
  raise message unless value
end

def rejects(message)
  begin
    yield
  rescue ArgumentError => error
    assert(error.message.include?(message), error.message)
    return
  end
  raise "Expected rejection: #{message}"
end

Dir.mktmpdir do |root|
  FileUtils.mkdir_p("#{root}/build")
  FileUtils.mkdir_p("#{root}/app-store/google-play/en-US/changelogs")
  File.write("#{root}/AndroidManifest.xml", '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.dddumpling.game" android:versionCode="11" android:versionName="0.1.10"/>')
  File.write("#{root}/build/DDDUMPLING.aab", "fixture")
  notes = "#{root}/app-store/google-play/en-US/changelogs/11.txt"
  File.write(notes, "Improved playtest build.")
  ENV["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"] = JSON.generate(type: "service_account", client_email: "test@example.invalid", private_key: "fixture")
  ENV["GITHUB_REF"] = "refs/tags/v0.1.10"
  ENV.delete("PLAY_CLOSED_TRACK")
  ENV.delete("PLAY_RELEASE_STATUS")
  FileUtils.mkdir_p("#{root}/app-store/google-play/screenshots")
  options = play_upload_options(root)
  assert(play_upload_options(root, "internal")[:track] == "internal", "explicit internal uploads")
  ENV["PLAY_CLOSED_TRACK"] = "playtesters"
  assert(play_upload_options(root)[:track] == "playtesters", "custom closed track")
  %w[production beta internal wear:production].each do |track|
    ENV["PLAY_CLOSED_TRACK"] = track
    rejects("closed testing track") { play_upload_options(root) }
  end
  ENV.delete("PLAY_CLOSED_TRACK")
  rejects("closed or internal") { play_upload_options(root, "production") }
  assert(options[:track] == "alpha" && options[:release_status] == "draft", "closed draft by default")
  ENV["PLAY_RELEASE_STATUS"] = "completed"
  assert(play_upload_options(root)[:release_status] == "completed", "activation must be explicit")
  ENV["PLAY_RELEASE_STATUS"] = "unexpected"
  rejects("draft or completed") { play_upload_options(root) }
  ENV.delete("PLAY_RELEASE_STATUS")
  assert(options[:skip_upload_metadata] && options[:skip_upload_images] && options[:skip_upload_screenshots], "preserve store listing")
  assert(!options[:skip_upload_changelogs] && options[:version_name] == "0.1.10 (11)", "publish versioned notes")
  assert(Dir.children(options[:metadata_path]) == ["en-US"], "shared assets are not treated as languages")
  assert(File.read(File.join(options[:metadata_path], "en-US/changelogs/11.txt")) == File.read(notes), "stage the exact release notes")
  ENV["GITHUB_REF"] = "refs/tags/v0.1.9"
  rejects("Release tag must match") { play_upload_options(root) }
  ENV["GITHUB_REF"] = "refs/heads/main"
  File.write(notes, "x" * 501)
  rejects("1–500") { play_upload_options(root) }
  File.delete(notes)
  rejects("Add release notes") { play_upload_options(root) }
  File.write(notes, "Update")
  ENV.delete("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
  rejects("Set GitHub secret") { play_upload_options(root) }
  ENV["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"] = "bad secret value"
  rejects("valid service-account") { play_upload_options(root) }
  ENV["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"] = '{}'
  rejects("service-account JSON key") { play_upload_options(root) }
end
Dir.mktmpdir do |root|
  FileUtils.mkdir_p("#{root}/ios")
  File.write("#{root}/ios/project.yml", "        MARKETING_VERSION: 0.1.27\n")
  assert(ios_production_version!(root, "0.1.27") == "0.1.27", "retained iOS version is accepted independently of Android")
  rejects("iOS marketing version") { ios_production_version!(root, "0.1.29") }
  File.write("#{root}/ios/project.yml", "missing version\n")
  rejects("iOS marketing version") { ios_production_version!(root, "0.1.27") }
end
Dir.mktmpdir do |root|
  FileUtils.mkdir_p("#{root}/ios/store/en-US")
  path = "#{root}/ios/store/en-US/what_to_test.txt"
  File.write(path, "Connect Game Center for cloud progress.\n\nPlease test sign-in.")
  assert(ios_production_notes!(root) == "Connect Game Center for cloud progress.", "Apple publishes its own summary without beta instructions")
  File.write(path, "  ")
  rejects("Apple release notes") { ios_production_notes!(root) }
  File.write(path, "x" * 4001)
  rejects("Apple release notes") { ios_production_notes!(root) }
end
PendingVersion = Struct.new(:id, :version_string, :app_store_state)
PendingReview = Struct.new(:app_store_version_for_review, :items, :state, :cancellations) do
  def cancel_submission
    self.cancellations += 1
    app_store_version_for_review.app_store_state = 'DEVELOPER_REJECTED'
  end
end
PendingApp = Struct.new(:version, :submission) do
  def get_in_progress_review_submission(**); submission; end
  def get_edit_app_store_version(**); version; end
end
old = PendingVersion.new('v35', '0.1.35', 'WAITING_FOR_REVIEW')
review = PendingReview.new(old, ['app-version-item'], 'WAITING_FOR_REVIEW', 0)
app = PendingApp.new(old, review)
ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35')
assert(review.cancellations == 0, 'replacement preflight never cancels review')
['', '0.1.34'].each do |wrong|
  rejects('different App Store draft') { ios_replace_pending!(app, old, '0.1.36', replace: wrong, apply: true) }
end
rejects('different App Store draft') { ios_replace_pending!(app, old, '0.1.34', replace: '0.1.35', apply: true) }
old.app_store_state = 'IN_REVIEW'
rejects('different App Store draft') { ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true) }
old.app_store_state = 'WAITING_FOR_REVIEW'
review.items << 'another-item'
rejects('different or additional items') { ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true) }
review.items.pop
review.state = 'IN_REVIEW'
rejects('different or additional items') { ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true) }
review.state = 'WAITING_FOR_REVIEW'
review.app_store_version_for_review = PendingVersion.new('other', '0.1.35', 'WAITING_FOR_REVIEW')
rejects('different or additional items') { ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true) }
review.app_store_version_for_review = old
assert(review.cancellations == 0, 'all guard failures preserve the pending review')
ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true)
assert(review.cancellations == 1 && old.app_store_state == 'DEVELOPER_REJECTED', 'only the authorized review is cancelled')
ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true)
assert(review.cancellations == 1, 'retry after cancellation does not cancel again')
old.app_store_state = 'WAITING_FOR_REVIEW'
def review.cancel_submission; self.cancellations += 1; end
waits = 0
rejects('still processing') do
  ios_replace_pending!(app, old, '0.1.36', replace: '0.1.35', apply: true, wait: -> { waits += 1 })
end
assert(waits == 40, 'slow Apple cancellation is bounded')
state = nil
calls = []
request = lambda do |method, path, body|
  calls << [method, path, body]
  case method
  when 'get' then {'data' => state}
  when 'post'
    assert(body.dig(:data, :relationships, :appStoreVersion, :data, :id) == 'version-1', 'link only the selected version')
    state = {'id' => 'gc-1', 'attributes' => {'enabled' => false}}
    {'data' => state}
  when 'patch'
    assert(path == '/gameCenterAppVersions/gc-1' && body.dig(:data, :attributes, :enabled), 'enable selected Game Center version')
    state['attributes']['enabled'] = true
    {'data' => state}
  end
end
ios_game_center_version!('version-1', request: request)
assert(calls.map(&:first) == ['get'], 'Game Center preflight is read-only')
calls.clear
ios_game_center_version!('version-1', request: request, apply: true)
assert(calls.map(&:first) == ['get', 'post', 'patch', 'get'], 'create, enable and verify missing link')
calls.clear
ios_game_center_version!('version-1', request: request, apply: true)
assert(calls.map(&:first) == ['get'], 'enabled links are unchanged on retry')
state['attributes']['enabled'] = false
calls.clear
ios_game_center_version!('version-1', request: request, apply: true)
assert(calls.map(&:first) == ['get', 'patch', 'get'], 'reuse an existing disabled link')
puts "Fastlane configuration checks passed"
