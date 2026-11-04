require_relative '../fastlane/game_center_review'

def assert(value, message)
  raise message unless value
end

def rejects(message)
  begin
    yield
  rescue RuntimeError => error
    raise unless error.message.include?(message)
    return
  end
  raise "Expected rejection: #{message}"
end

class AppleReviewFixture
  attr_accessor :achievements, :versions, :submissions, :review_items, :received, :pagination, :app_ready
  attr_reader :writes
  def initialize
    @achievements = [{'id' => 'a', 'attributes' => {'vendorIdentifier' => 'game.a'}}]
    @versions = [{'id' => 'av', 'attributes' => {'version' => 1, 'state' => 'PREPARE_FOR_SUBMISSION'}}]
    @submissions = []
    @review_items = []
    @writes = []
    @received = true
    @app_ready = true
  end

  def item(relationship, id)
    {'relationships' => {relationship => {'data' => {'id' => id}}}}
  end

  def request(method, path, body)
    @writes << [method, path, body] unless method == 'get'
    case [method, path.split('?').first]
    when ['get', '/v1/appStoreVersions/version'] then {'data' => {'attributes' => {'appVersionState' => @app_ready ? 'READY_FOR_REVIEW' : 'PREPARE_FOR_SUBMISSION'}}}
    when ['get', '/v1/apps/app/gameCenterDetail'] then {'data' => {'id' => 'gc'}}
    when ['get', '/v1/gameCenterDetails/gc/gameCenterAchievementsV2']
      if @pagination && !path.include?('page=2')
        {'data' => [], 'links' => {'next' => 'https://api.appstoreconnect.apple.com/v1/gameCenterDetails/gc/gameCenterAchievementsV2?page=2'}}
      else
        {'data' => @achievements}
      end
    when ['get', '/v2/gameCenterAchievements/a/versions'] then {'data' => @versions}
    when ['get', '/v1/apps/app/reviewSubmissions'] then {'data' => @submissions}
    when ['post', '/v1/reviewSubmissions']
      @submissions << {'id' => 'review', 'attributes' => {'state' => 'READY_FOR_REVIEW'}}
      {'data' => @submissions.last}
    when ['get', '/v1/reviewSubmissions/review/items'] then {'data' => @review_items}
    when ['post', '/v1/reviewSubmissionItems']
      rels = body.fetch(:data).fetch(:relationships)
      name = (rels.keys - [:reviewSubmission]).first
      @review_items << item(name.to_s, rels.fetch(name).fetch(:data).fetch(:id)) if @received
      {'data' => {}}
    when ['patch', '/v1/reviewSubmissions/review']
      @submissions.first['attributes']['state'] = 'WAITING_FOR_REVIEW'
      @versions.first['attributes']['state'] = 'WAITING_FOR_REVIEW' if @received
      {'data' => @submissions.first}
    when ['get', '/v1/reviewSubmissions/review'] then {'data' => @submissions.first}
    when ['get', '/v2/gameCenterAchievementVersions/av'] then {'data' => @versions.first}
    else raise "Unexpected request: #{method} #{path}"
    end
  end
end
mapping = {'first' => {'apple_id' => 'game.a', 'apple_resource_id' => 'a'}}
f = AppleReviewFixture.new
review = GameCenterReview.new(f.method(:request))
f.pagination = true
plan = review.plan('app', mapping)
review.draft('app', 'version', plan)
assert(f.writes.empty?, 'preflight must be read-only, including pagination')
submission = review.draft('app', 'version', plan, apply: true)
assert(review.references(f.review_items).sort == [['appStoreVersion', 'version'], ['gameCenterAchievementVersion', 'av']].sort, 'first submission includes app and achievement versions')
writes = f.writes.length
review.draft('app', 'version', plan, apply: true)
assert(f.writes.length == writes, 'retry must not duplicate review items')
review.submit!(submission, 'version', plan, wait: -> {})
assert(f.submissions.first.dig('attributes', 'state') == 'WAITING_FOR_REVIEW', 'verify Apple accepted submission')
rejects('already in progress') { review.draft('app', 'version', plan, apply: true) }

f = AppleReviewFixture.new
review = GameCenterReview.new(f.method(:request))
f.achievements.first['attributes']['vendorIdentifier'] = 'wrong'
rejects('mismatched') { review.plan('app', mapping) }
f.achievements.first['attributes']['vendorIdentifier'] = 'game.a'
f.versions.first['attributes']['state'] = 'REJECTED'
rejects('needs attention') { review.plan('app', mapping) }
f.versions.first['attributes']['state'] = 'LIVE'
live = review.plan('app', mapping)
assert(review.expected('version', live) == [['appStoreVersion', 'version']], 'live achievements do not get resubmitted')
f.versions << {'id' => 'new', 'attributes' => {'version' => 2, 'state' => 'PREPARE_FOR_SUBMISSION'}}
assert(review.plan('app', mapping).first[:id] == 'new', 'newest version wins over historical live version')
f.submissions << {'id' => 'review', 'attributes' => {'state' => 'READY_FOR_REVIEW'}}
f.review_items << f.item('appStoreVersion', 'other')
rejects('different or additional') { review.draft('app', 'version', plan, apply: true) }
assert(f.writes.empty?, 'foreign draft must not be mutated')
f.review_items.clear
f.received = false
rejects('missing expected') { review.draft('app', 'version', plan, apply: true) }
assert(f.writes.none? { |method, _, _| method == 'patch' }, 'missing achievement prevents submission')
f.review_items = [f.item('appStoreVersion', 'version'), f.item('gameCenterAchievementVersion', 'av')]
f.versions.first['attributes']['state'] = 'PREPARE_FOR_SUBMISSION'
rejects('not confirmed') { review.submit!(f.submissions.first, 'version', plan, wait: -> {}) }
f.app_ready = false
writes = f.writes.length
rejects('not ready for review') { review.submit!(f.submissions.first, 'version', plan, wait: -> {}) }
assert(f.writes.length == writes, 'never submit before Apple finishes preparing the app version')
puts 'Game Center review checks passed' 
