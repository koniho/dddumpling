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
# Enable the version-level link required by builds with the Game Center entitlement.
def ios_game_center_version!(version_id, request:, apply: false)
  path = "/appStoreVersions/#{version_id}/gameCenterAppVersion"
  begin
    current = request.call('get', path, nil)['data']
  rescue RuntimeError => error
    raise unless error.message.start_with?("get #{path}: 404 ")
    current = nil
  end
  return current unless apply
  return current if current && current.dig('attributes', 'enabled') == true
  unless current
    current = request.call('post', '/gameCenterAppVersions', {
      data: {type: 'gameCenterAppVersions', relationships: {
        appStoreVersion: {data: {type: 'appStoreVersions', id: version_id}}
      }}
    }).fetch('data')
  end
  unless current.dig('attributes', 'enabled') == true
    request.call('patch', "/gameCenterAppVersions/#{current.fetch('id')}", {
      data: {type: 'gameCenterAppVersions', id: current.fetch('id'), attributes: {enabled: true}}
    })
  end
  saved = request.call('get', path, nil).fetch('data')
  UI.user_error!('Game Center version link verification failed') unless saved && saved.dig('attributes', 'enabled') == true
  saved
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
    require_relative '../tools/store-release-notes'
    request = lambda do |method, path, body|
      request_json('https://api.appstoreconnect.apple.com/v1', Spaceship::ConnectAPI.token.text, method, path, body)
    end
    request.call('get', "/apps/#{app.id}/gameCenterDetail", nil)
    if editable
      link = ios_game_center_version!(editable.id, request: request)
      UI.message("Game Center version link: #{link ? link.dig('attributes', 'enabled') : 'missing'}")
    end
    if ENV['APPLY'] == 'true'
      app.ensure_version!(version, platform: 'IOS')
      selected = app.get_edit_app_store_version(platform: 'IOS')
      UI.user_error!('Expected selected editable Apple version') unless selected && selected.version_string == version
      ios_game_center_version!(selected.id, request: request, apply: true)
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
