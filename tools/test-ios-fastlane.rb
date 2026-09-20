# Offline checks for the iOS lanes. No Fastlane action here contacts Apple or match.
require "tmpdir"
require "fileutils"

$calls = []
$match_private_keys = []
module UI
  def self.user_error!(message); raise ArgumentError, message; end
end
def default_platform(*); end
def platform(*); yield; end
def desc(*); end
def lane(*); end
def import(path); load File.expand_path("../fastlane/#{path}", __dir__); end
def record_action(name, options = {})
  $calls << [name, options]
  options
end
def app_store_connect_api_key(**options); record_action(:app_store_connect_api_key, options); end
def setup_ci(**options); record_action(:setup_ci, options); end
def sh(*command); record_action(:sh, command: command); end
def match(**options)
  $match_private_keys << ENV["MATCH_GIT_PRIVATE_KEY"]
  ENV["sigh_com.dddumpling.game.ios_appstore_profile-name"] = "match AppStore com.dddumpling.game.ios"
  record_action(:match, options)
end
def update_code_signing_settings(**options); record_action(:update_code_signing_settings, options); end
def build_app(**options); record_action(:build_app, options); end
def upload_to_testflight(**options); record_action(:upload_to_testflight, options); end

load File.expand_path("../fastlane/ios.rb", __dir__)

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

def clear_ios_environment
  ENV.delete("DDDUMPLING_GAME_CENTER")
  ENV.keys.grep(/\AIOS_|\AMATCH_/).each { |name| ENV.delete(name) }
  ENV.delete("sigh_com.dddumpling.game.ios_appstore_profile-name")
end

def archive_environment
  {
    "IOS_TEAM_ID" => "A1B2C3D4E5",
    "IOS_MATCH_GIT_URL" => "git@github.com:example/signing.git",
    "IOS_MATCH_GIT_PRIVATE_KEY" => "-----BEGIN OPENSSH PRIVATE KEY-----\nfixture\n-----END OPENSSH PRIVATE KEY-----",
    "IOS_MATCH_PASSWORD" => "encrypted-certificates-password",
    "IOS_APPSTORE_KEY_ID" => "ABC123DEFG",
    "IOS_APPSTORE_ISSUER_ID" => "00000000-0000-0000-0000-000000000000",
    "IOS_APPSTORE_KEY_BASE64" => "base64-private-key",
    "IOS_BUILD_NUMBER" => "42.1"
  }
end

clear_ios_environment
$calls.clear
rejects("IOS_TEAM_ID") { ios_archive! }
assert($calls.empty?, "archive validates configuration before actions")

archive_environment.each { |name, value| ENV[name] = value }
ENV["IOS_BUILD_NUMBER"] = "42.100"
$calls.clear
rejects("1–3 numeric components") { ios_archive! }
assert($calls.empty?, "archive validates the build number before actions")

archive_environment.each { |name, value| ENV[name] = value }
$calls.clear
$match_private_keys.clear
ios_archive!
actions = $calls.map(&:first)
assert(actions.include?(:match) && actions.include?(:build_app), "archive obtains signing assets and builds")
assert(!actions.include?(:upload_to_testflight), "archive never uploads")
match_options = $calls.assoc(:match).last
assert(match_options[:readonly] && match_options[:type] == "appstore", "match is read-only App Store signing")
assert($match_private_keys == [archive_environment.fetch("IOS_MATCH_GIT_PRIVATE_KEY")], "match receives the SSH deploy key contents")
assert(match_options[:keychain_password] == "", "match uses setup_ci's standard empty temporary keychain password")
signing_options = $calls.assoc(:update_code_signing_settings).last
assert(signing_options[:targets] == ["DDDumpling"] && signing_options[:build_configurations] == ["Release"], "only the app Release configuration uses manual signing")
build_options = $calls.assoc(:build_app).last
assert(build_options[:xcargs] == "CURRENT_PROJECT_VERSION=42.1 DDDUMPLING_GAME_CENTER=0" && build_options[:export_method] == "app-store", "archive uses the supplied build number and defaults GameKit off")
assert(ENV["MATCH_PASSWORD"].nil? && ENV["MATCH_GIT_PRIVATE_KEY"].nil?, "match credentials do not leak into subsequent lanes")
archive_calls = $calls.reject { |name, _| name == :sh }.to_h

ENV["DDDUMPLING_GAME_CENTER"] = "1"
$calls.clear
ios_archive!
assert($calls.assoc(:build_app).last[:xcargs].end_with?("DDDUMPLING_GAME_CENTER=1"), "archive passes explicit GameKit opt-in")
ENV["DDDUMPLING_GAME_CENTER"] = "yes"
$calls.clear
rejects("DDDUMPLING_GAME_CENTER must be 0 or 1") { ios_archive! }
assert($calls.empty?, "invalid GameKit setting fails before signing actions")

clear_ios_environment
$calls.clear
rejects("IOS_APPSTORE_KEY_ID") { ios_upload_testflight! }
assert($calls.empty?, "upload validates credentials before actions")

%w[IOS_APPSTORE_KEY_ID IOS_APPSTORE_ISSUER_ID IOS_APPSTORE_KEY_BASE64].each { |name| ENV[name] = archive_environment.fetch(name) }
$calls.clear
Dir.mktmpdir("ios-fastlane-missing-") do |root|
  rejects("Build the signed IPA") { ios_upload_testflight!(ipa_path: File.join(root, "missing.ipa")) }
end
assert($calls.empty?, "upload requires a pre-existing IPA before contacting Apple")

upload_options = nil
Dir.mktmpdir("ios-fastlane-ipa-") do |root|
  ipa_path = File.join(root, "DDDumpling.ipa")
  File.write(ipa_path, "fixture")
  $calls.clear
  ios_upload_testflight!(ipa_path: ipa_path)
  upload_options = $calls.assoc(:upload_to_testflight).last
  assert(upload_options[:ipa] == ipa_path, "upload uses the archived IPA")
  notes = File.read(File.expand_path("../ios/store/en-US/what_to_test.txt", __dir__)).strip
  assert(!notes.empty? && upload_options[:changelog] == notes, "upload includes the reviewed TestFlight notes")
  assert(!upload_options[:distribute_external] && upload_options[:skip_waiting_for_build_processing], "upload avoids external distribution")
end

Group = Struct.new(:id, :is_internal_group)
TestApp = Struct.new(:get_beta_groups)
ENV["IOS_BUILD_NUMBER"] = "42.1"
ENV["IOS_TEST_VERSION"] = "0.1.17"
$calls.clear
ios_distribute_external!(app: TestApp.new([Group.new("internal", true), Group.new("external", false)]))
external_options = $calls.assoc(:upload_to_testflight).last
assert(external_options[:app_platform] == "ios", "distribution without an IPA never prompts for platform")
assert(external_options[:groups] == ["external"], "distribution selects only existing external groups")
assert(external_options[:app_version] == "0.1.17" && external_options[:build_number] == "42.1", "distribution pins the exact uploaded build")
assert(external_options[:distribute_only] && external_options[:distribute_external] && external_options[:submit_beta_review], "external distribution submits review without uploading again")
assert(!external_options[:skip_waiting_for_build_processing], "external distribution waits for processing")
$calls.clear
rejects("No existing external") { ios_distribute_external!(app: TestApp.new([Group.new("internal", true)])) }
assert(!$calls.assoc(:upload_to_testflight), "no groups never falls through to another distribution target")

require "fastlane"
require "fastlane/actions/app_store_connect_api_key"
require "fastlane/actions/setup_ci"
require "fastlane/actions/match"
require "fastlane/actions/update_code_signing_settings"
require "fastlane/actions/build_app"
require "fastlane/actions/upload_to_testflight"

def assert_supported_options(action, supplied)
  unknown = supplied - action.available_options.map(&:key)
  assert(unknown.empty?, "#{action} does not support: #{unknown.join(', ')}")
end

assert_supported_options(Fastlane::Actions::AppStoreConnectApiKeyAction, %i[key_id issuer_id key_content is_key_content_base64])
assert_supported_options(Fastlane::Actions::SetupCiAction, %i[force keychain_name])
assert_supported_options(Fastlane::Actions::MatchAction, %i[type readonly app_identifier team_id git_url keychain_name keychain_password api_key])
assert_supported_options(Fastlane::Actions::UpdateCodeSigningSettingsAction, %i[path use_automatic_signing targets build_configurations team_id code_sign_identity profile_name])
assert_supported_options(Fastlane::Actions::BuildAppAction, %i[project scheme configuration xcargs export_method archive_path output_directory output_name export_options])
assert_supported_options(Fastlane::Actions::UploadToTestflightAction, %i[api_key ipa changelog distribute_external skip_waiting_for_build_processing])

{
  app_store_connect_api_key: Fastlane::Actions::AppStoreConnectApiKeyAction,
  setup_ci: Fastlane::Actions::SetupCiAction,
  match: Fastlane::Actions::MatchAction,
  update_code_signing_settings: Fastlane::Actions::UpdateCodeSigningSettingsAction,
  build_app: Fastlane::Actions::BuildAppAction
}.each { |name, action| assert_supported_options(action, archive_calls.fetch(name).keys) }
assert_supported_options(Fastlane::Actions::UploadToTestflightAction, upload_options.keys)
assert_supported_options(Fastlane::Actions::UploadToTestflightAction, external_options.keys)

puts "iOS Fastlane configuration checks passed"
