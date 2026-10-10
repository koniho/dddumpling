require_relative 'promote-play-production'
def rejects
  begin
    yield
  rescue RuntimeError
    return
  end
  raise 'Expected rejection'
end
notes = 'Approved notes'
target = {'versionCodes' => ['28'], 'status' => 'draft', 'releaseNotes' => [{'language' => 'en-US', 'text' => notes}]}
source = {'track' => 'alpha', 'releases' => [target]}
prior = {'versionCodes' => ['27'], 'status' => 'completed'}
production = {'track' => 'production', 'releases' => [prior]}
result = production_release(source, production, '28', notes)
raise 'Wrong promoted build' unless result['releases'] == [target.merge('status' => 'completed')]
raise 'Source mutated' unless target['status'] == 'draft'
raise 'Not idempotent' unless production_release(source, result, '28', notes) == result
rejects { production_release(source, production, '29', notes) }
rejects { production_release(source, production, '28', 'Unapproved notes') }
rejects { production_release(source, production.merge('track' => 'beta'), '28', notes) }
rejects { production_release(source, production.merge('releases' => [prior.merge('status' => 'inProgress')]), '28', notes) }
rejects { production_release(source, production.merge('releases' => [prior.merge('versionCodes' => ['29'])]), '28', notes) }
rejects { production_release(source.merge('releases' => [target, target]), production, '28', notes) }
puts 'Production promotion guards passed'
