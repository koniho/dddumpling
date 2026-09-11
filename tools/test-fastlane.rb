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
  assert(options[:track] == "alpha" && options[:release_status] == "completed", "closed track by default")
  assert(options[:skip_upload_metadata] && options[:skip_upload_images] && options[:skip_upload_screenshots], "preserve store listing")
  assert(!options[:skip_upload_changelogs] && options[:release_name] == "0.1.10 (11)", "publish versioned notes")
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
puts "Fastlane configuration checks passed"
