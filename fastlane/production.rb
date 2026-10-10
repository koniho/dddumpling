# Promote only already-uploaded, explicitly selected builds.
platform :ios do
  desc 'Inspect or submit the selected uploaded build for automatic App Store release'
  lane :production do
    environment = ios_upload_configuration!
    api_key = ios_app_store_api_key!(environment)
    root = File.expand_path('..', __dir__)
    manifest = REXML::Document.new(File.read(File.join(root, 'AndroidManifest.xml'))).root
    version = ENV.fetch('IOS_RELEASE_VERSION')
    UI.user_error!('Release version must match manifest') unless version == manifest.attributes['android:versionName']
    build_number = ios_build_number!
    notes = File.read(File.join(root, 'ios/store/en-US/what_to_test.txt')).strip
    play_notes = File.read(File.join(root, "app-store/google-play/en-US/changelogs/#{manifest.attributes['android:versionCode']}.txt")).strip
    UI.user_error!('Store notes differ') unless notes == play_notes
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
        submit_for_review: true, automatic_release: true,
        precheck_include_in_app_purchases: false
      )
      saved = app.get_app_store_versions(filter: {platform: 'IOS', versionString: version}).first
      UI.message("Verified App Store #{saved.version_string}: #{saved.app_store_state}, release #{saved.release_type}")
    end
  end
end
