IOS_BUNDLE_ID = "com.dddumpling.game.ios".freeze
IOS_KEYCHAIN_NAME = "dddumpling-ci.keychain-db".freeze
IOS_IPA_PATH = File.expand_path("../ios/build/DDDumpling.ipa", __dir__).freeze

def ios_required_environment!(names)
  missing = names.select { |name| ENV.fetch(name, "").strip.empty? }
  UI.user_error!("Set required environment variables: #{missing.join(', ')}") unless missing.empty?
  names.to_h { |name| [name, ENV.fetch(name).strip] }
end

def ios_build_number!
  value = ENV.fetch("IOS_BUILD_NUMBER", "").strip
  compatible = /\A(?:[1-9][0-9]{0,3})(?:\.(?:0|[1-9][0-9]?)){0,2}\z/.match?(value)
  UI.user_error!("IOS_BUILD_NUMBER must be 1–3 numeric components (first 1–9999, later 0–99)") unless compatible
  value
end

def ios_app_store_api_key!(environment)
  app_store_connect_api_key(
    key_id: environment.fetch("IOS_APPSTORE_KEY_ID"),
    issuer_id: environment.fetch("IOS_APPSTORE_ISSUER_ID"),
    key_content: environment.fetch("IOS_APPSTORE_KEY_BASE64"),
    is_key_content_base64: true
  )
end

def ios_archive_configuration!
  names = %w[
    IOS_TEAM_ID
    IOS_MATCH_GIT_URL
    IOS_MATCH_GIT_PRIVATE_KEY
    IOS_MATCH_PASSWORD
    IOS_APPSTORE_KEY_ID
    IOS_APPSTORE_ISSUER_ID
    IOS_APPSTORE_KEY_BASE64
  ]
  ios_required_environment!(names).merge("IOS_BUILD_NUMBER" => ios_build_number!)
end

def ios_upload_configuration!
  ios_required_environment!(%w[
    IOS_APPSTORE_KEY_ID
    IOS_APPSTORE_ISSUER_ID
    IOS_APPSTORE_KEY_BASE64
  ])
end

def ios_prepare_release_project!
  root = File.expand_path("..", __dir__)
  sh("bash", File.join(root, "ios/scripts/translate.sh"), "Release")
  sh("xcodegen", "generate", "--spec", File.join(root, "ios/project.yml"))
end

def ios_signing_profile_name!
  name = ENV.fetch("sigh_com.dddumpling.game.ios_appstore_profile-name", "").strip
  UI.user_error!("match did not provide the App Store provisioning profile for #{IOS_BUNDLE_ID}") if name.empty?
  name
end

def ios_with_match_credentials(environment)
  prior_password = ENV["MATCH_PASSWORD"]
  prior_private_key = ENV["MATCH_GIT_PRIVATE_KEY"]
  ENV["MATCH_PASSWORD"] = environment.fetch("IOS_MATCH_PASSWORD")
  ENV["MATCH_GIT_PRIVATE_KEY"] = environment.fetch("IOS_MATCH_GIT_PRIVATE_KEY")
  yield
ensure
  ENV["MATCH_PASSWORD"] = prior_password
  ENV["MATCH_GIT_PRIVATE_KEY"] = prior_private_key
end

def ios_game_center_flag!
  value = ENV.fetch("DDDUMPLING_GAME_CENTER", "0")
  value = "0" if value.empty?
  UI.user_error!("DDDUMPLING_GAME_CENTER must be 0 or 1") unless %w[0 1].include?(value)
  value
end

def ios_archive!
  game_center = ios_game_center_flag!
  environment = ios_archive_configuration!
  api_key = ios_app_store_api_key!(environment)
  ENV.delete("sigh_com.dddumpling.game.ios_appstore_profile-name")

  setup_ci(
    force: true,
    keychain_name: IOS_KEYCHAIN_NAME
  )
  ios_prepare_release_project!
  ios_with_match_credentials(environment) do
    match(
      type: "appstore",
      readonly: true,
      app_identifier: IOS_BUNDLE_ID,
      team_id: environment.fetch("IOS_TEAM_ID"),
      git_url: environment.fetch("IOS_MATCH_GIT_URL"),
      keychain_name: IOS_KEYCHAIN_NAME,
      keychain_password: "",
      api_key: api_key
    )
  end

  profile_name = ios_signing_profile_name!
  project = File.expand_path("../ios/DDDumpling.xcodeproj", __dir__)
  update_code_signing_settings(
    path: project,
    use_automatic_signing: false,
    targets: ["DDDumpling"],
    build_configurations: ["Release"],
    team_id: environment.fetch("IOS_TEAM_ID"),
    code_sign_identity: "Apple Distribution",
    profile_name: profile_name
  )
  build_app(
    project: project,
    scheme: "DDDumpling",
    configuration: "Release",
    xcargs: "CURRENT_PROJECT_VERSION=#{environment.fetch('IOS_BUILD_NUMBER')} DDDUMPLING_GAME_CENTER=#{game_center}",
    export_method: "app-store",
    archive_path: File.expand_path("../ios/build/DDDumpling.xcarchive", __dir__),
    output_directory: File.dirname(IOS_IPA_PATH),
    output_name: File.basename(IOS_IPA_PATH),
    export_options: {
      signingStyle: "manual",
      provisioningProfiles: { IOS_BUNDLE_ID => profile_name }
    }
  )
end

def ios_upload_testflight!(ipa_path: IOS_IPA_PATH)
  environment = ios_upload_configuration!
  UI.user_error!("Build the signed IPA with `bundle exec fastlane ios archive` first") unless File.file?(ipa_path)
  api_key = ios_app_store_api_key!(environment)
  upload_to_testflight(
    api_key: api_key,
    ipa: ipa_path,
    changelog: File.read(File.expand_path("../ios/store/en-US/what_to_test.txt", __dir__)).strip,
    distribute_external: false,
    skip_waiting_for_build_processing: true
  )
end

def ios_distribute_external!(app: nil)
  environment = ios_upload_configuration!
  build = ios_build_number!
  version = ENV.fetch("IOS_TEST_VERSION", "").strip
  UI.user_error!("Set IOS_TEST_VERSION to the uploaded marketing version") unless /\A\d+\.\d+\.\d+\z/.match?(version)
  api_key = ios_app_store_api_key!(environment)
  app ||= Spaceship::ConnectAPI::App.find(IOS_BUNDLE_ID)
  UI.user_error!("TestFlight app not found") unless app
  groups = app.get_beta_groups.reject(&:is_internal_group).map(&:id)
  UI.user_error!("No existing external TestFlight groups") if groups.empty?
  upload_to_testflight(
    api_key: api_key,
    app_identifier: IOS_BUNDLE_ID,
    app_version: version,
    build_number: build,
    distribute_only: true,
    distribute_external: true,
    groups: groups,
    submit_beta_review: true,
    notify_external_testers: true,
    skip_waiting_for_build_processing: false,
    wait_processing_timeout_duration: 1200,
    changelog: File.read(File.expand_path("../ios/store/en-US/what_to_test.txt", __dir__)).strip
  )
end

platform :ios do
  desc "Build a signed iPhone IPA using read-only match assets; does not upload"
  lane :archive do
    ios_archive!
  end

  desc "Assign an exact uploaded build to existing external groups and submit beta review"
  lane :distribute_external do
    ios_distribute_external!
  end

  desc "Upload an already archived IPA to TestFlight without distributing to external testers"
  lane :upload_testflight do
    ios_upload_testflight!
  end
end
