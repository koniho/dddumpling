# Game Center versions are separate review items, even when the app enables GameKit.
require 'json'
require 'uri'

class GameCenterReview
  def initialize(request)
    @request = request
  end

  def call(method, path, body = nil)
    @request.call(method, path, body)
  end

  def list(path)
    rows = []
    seen = []
    while path
      raise 'Repeated Apple pagination link' if seen.include?(path)
      seen << path
      page = call('get', path)
      rows.concat(page.fetch('data'))
      path = page.dig('links', 'next')
      if path
        uri = URI(path)
        raise 'Unexpected Apple pagination host' unless uri.scheme == 'https' && uri.host == 'api.appstoreconnect.apple.com'
        path = uri.request_uri
      end
    end
    rows
  end

  def plan(app_id, mappings)
    raise 'No configured Apple achievements' if mappings.empty?
    detail = call('get', "/v1/apps/#{app_id}/gameCenterDetail").fetch('data').fetch('id')
    achievements = list("/v1/gameCenterDetails/#{detail}/gameCenterAchievementsV2?limit=200")
    ids = mappings.values.map { |mapping| mapping.fetch('apple_resource_id') }
    raise 'Duplicate Apple achievement mappings' unless ids.uniq == ids
    mappings.map do |key, mapping|
      achievement = achievements.find { |item| item['id'] == mapping.fetch('apple_resource_id') }
      unless achievement && achievement.dig('attributes', 'vendorIdentifier') == mapping.fetch('apple_id') &&
          achievement.dig('attributes', 'archived') != true
        raise "Missing, archived or mismatched Apple achievement: #{key}"
      end
      versions = list("/v2/gameCenterAchievements/#{achievement.fetch('id')}/versions?limit=200")
      latest = versions.max_by { |item| item.fetch('attributes').fetch('version') }
      raise "No Apple achievement version: #{key}" unless latest
      state = latest.fetch('attributes').fetch('state')
      unless %w[PREPARE_FOR_SUBMISSION READY_FOR_REVIEW DEVELOPER_REJECTED LIVE].include?(state)
        raise "Apple achievement #{key} needs attention: #{state}"
      end
      {key: key, id: latest.fetch('id'), state: state}
    end
  end

  def references(items)
    items.map do |item|
      refs = item.fetch('relationships').map do |name, relationship|
        next if name == 'reviewSubmission'
        data = relationship['data']
        [name, data.fetch('id')] if data
      end
      refs.compact!
      raise 'Unexpected Apple review item' unless refs.length == 1
      refs.first
    end
  end

  def items(submission_id)
    list("/v1/reviewSubmissions/#{submission_id}/items?include=appStoreVersion,gameCenterAchievementVersion&limit=200")
  end

  def expected(version_id, plan)
    [['appStoreVersion', version_id]] + plan.reject { |item| item[:state] == 'LIVE' }.map { |item| ['gameCenterAchievementVersion', item[:id]] }
  end

  # Reuse only a draft containing a subset of this exact release. Never cancel review.
  def draft(app_id, version_id, plan, apply: false)
    wanted = expected(version_id, plan)
    submissions = list("/v1/apps/#{app_id}/reviewSubmissions?filter%5Bplatform%5D=IOS&limit=200")
    active = submissions.reject { |item| item.dig('attributes', 'state') == 'COMPLETE' }
    raise 'An Apple review is already in progress or multiple drafts exist' if active.length > 1 ||
      active.any? { |item| item.dig('attributes', 'state') != 'READY_FOR_REVIEW' }
    submission = active.first
    existing = submission ? references(items(submission.fetch('id'))) : []
    raise 'Apple draft contains different or additional items' unless (existing - wanted).empty? && existing.uniq == existing
    return submission unless apply
    submission ||= call('post', '/v1/reviewSubmissions', {data: {
      type: 'reviewSubmissions', attributes: {platform: 'IOS'},
      relationships: {app: {data: {type: 'apps', id: app_id}}}
    }}).fetch('data')
    types = {'appStoreVersion' => 'appStoreVersions', 'gameCenterAchievementVersion' => 'gameCenterAchievementVersions'}
    (wanted - existing).each do |relationship, id|
      call('post', '/v1/reviewSubmissionItems', {data: {
        type: 'reviewSubmissionItems', relationships: {
          reviewSubmission: {data: {type: 'reviewSubmissions', id: submission.fetch('id')}},
          relationship => {data: {type: types.fetch(relationship), id: id}}
        }
      }})
    end
    verify_items!(submission.fetch('id'), wanted)
    submission
  end

  def verify_items!(submission_id, wanted)
    actual = references(items(submission_id))
    raise 'Apple review is missing expected achievements or contains extra items' unless actual.sort == wanted.sort
  end

  def submit!(submission, version_id, plan, wait: -> { sleep(3) })
    id = submission.fetch('id')
    wanted = expected(version_id, plan)
    verify_items!(id, wanted)
    ready = false
    20.times do
      app_version = call('get', "/v1/appStoreVersions/#{version_id}").fetch('data')
      if app_version.dig('attributes', 'appVersionState') == 'READY_FOR_REVIEW'
        ready = true
        break
      end
      wait.call
    end
    raise 'Apple app version is not ready for review; draft preserved' unless ready
    call('patch', "/v1/reviewSubmissions/#{id}", {data: {
      type: 'reviewSubmissions', id: id, attributes: {submitted: true}
    }})
    20.times do
      saved = call('get', "/v1/reviewSubmissions/#{id}").fetch('data')
      state = saved.dig('attributes', 'state')
      if %w[WAITING_FOR_REVIEW IN_REVIEW COMPLETE].include?(state)
        verify_items!(id, wanted)
        states = plan.reject { |item| item[:state] == 'LIVE' }.map do |item|
          call('get', "/v2/gameCenterAchievementVersions/#{item[:id]}").dig('data', 'attributes', 'state')
        end
        return saved if states.all? { |s| %w[WAITING_FOR_REVIEW IN_REVIEW ACCEPTED PENDING_RELEASE LIVE].include?(s) }
      end
      raise "Apple submission needs attention: #{state}" if %w[UNRESOLVED_ISSUES CANCELING].include?(state)
      wait.call
    end
    raise 'Apple has not confirmed the app and achievements in review; inspect before retrying'
  end
end
