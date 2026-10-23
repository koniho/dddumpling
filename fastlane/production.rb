# Promote only already-uploaded, explicitly selected builds.
def ios_production_version!(root, version)
  project = File.read(File.join(root, "ios/project.yml"))
  marketing = project[/^\s*MARKETING_VERSION:\s*([0-9]+\.[0-9]+\.[0-9]+)\s*$/, 1]
  UI.user_error!("Release version must match iOS marketing version") unless marketing && version == marketing
  version
end
# The first paragraph is the player-facing summary; later paragraphs guide beta testing.
def ios_production_notes!(root)
  notes = File.read(File.join(root, 'ios/store/en-US/what_to_test.txt')).strip.split(/\n\s*\n/, 2).first.to_s
  UI.user_error!('Add Apple release notes (1–4000 characters)') unless (1..4000).cover?(notes.length)
  notes
end
platform :ios do
  desc 'Inspect or submit the selected uploaded build for automatic App Store release'
  lane :production do
    environment = ios_upload_configuration!
    api_key = ios_app_store_api_key!(environment)
    root = File.expand_path('..', __dir__)
    version = ENV.fetch('IOS_RELEASE_VERSION')
    ios_production_version!(root, version)
    build_number = ios_build_number!
    notes = ios_production_notes!(root)
    app = Spaceship::ConnectAPI::App.find(IOS_BUNDLE_ID)
    UI.user_error!('App not found') unless app
    builds = Spaceship::ConnectAPI::Build.all(app_id: app.id, version: version, build_number: build_number, platform: 'IOS')
    UI.user_error!('Expected one processed uploaded build') unless builds.length == 1 && builds.first.processing_state == 'VALID'
    versions = app.get_app_store_versions(filter: {platform: 'IOS'})
    versions.each { |v| UI.message("App Store #{v.version_string}: #{v.app_store_state}, release #{v.release_type}") }
    editable = app.get_edit_app_store_version(platform: 'IOS')
    UI.user_error!('A different App Store draft already exists') if editable && editable.version_string != version
    UI.message("Selected #{version} (#{build_number}), build ID #{builds.first.id}; automatic release after approval")
    if ENV['APPLY'] == 'true'
      upload_to_app_store(
        api_key: api_key, app_identifier: IOS_BUNDLE_ID, app_version: version,
        build_number: build_number, skip_binary_upload: true, skip_screenshots: true,
        release_notes: {'en-US' => notes}, force: true,
        submit_for_review: true, automatic_release: true, reject_if_possible: true,
        precheck_include_in_app_purchases: false
      )
      saved = app.get_app_store_versions(filter: {platform: 'IOS', versionString: version}).first
      UI.message("Verified App Store #{saved.version_string}: #{saved.app_store_state}, release #{saved.release_type}")
    end
  end
end
