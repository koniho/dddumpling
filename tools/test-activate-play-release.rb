require_relative 'activate-play-release'

def assert(value, message)
  raise message unless value
end
def rejects
  begin
    yield
  rescue RuntimeError
    return
  end
  raise 'Expected rejection'
end
draft = {'name' => '0.1.25 (27)', 'versionCodes' => ['27'], 'status' => 'draft',
         'releaseNotes' => [{'language' => 'en-US', 'text' => 'Reviewed notes'}], 'inAppUpdatePriority' => 2}
old = {'versionCodes' => ['25'], 'status' => 'completed'}
track = {'track' => 'alpha', 'releases' => [draft, old]}
copy = Marshal.load(Marshal.dump(track))
result = completed_closed_release(track, 'alpha', '27')
assert(result['releases'] == [draft.merge('status' => 'completed')], 'Only the chosen release becomes completed; metadata is preserved')
assert(track == copy, 'Planning must not mutate the original response')
assert(completed_closed_release(result, 'alpha', '27') == result, 'Activation must be idempotent')
%w[production beta internal wear:alpha].each { |name| rejects { completed_closed_release(track.merge('track' => name), name, '27') } }
rejects { completed_closed_release(track, 'other', '27') }
rejects { completed_closed_release(track, 'alpha', '26') }
rejects { completed_closed_release(track, 'alpha', '0') }
rejects { completed_closed_release(track.merge('releases' => [draft, draft]), 'alpha', '27') }
rejects { completed_closed_release(track.merge('releases' => [draft.merge('versionCodes' => ['27', '25'])]), 'alpha', '27') }
rejects { completed_closed_release(track.merge('releases' => [draft.merge('status' => 'inProgress')]), 'alpha', '27') }
rejects { completed_closed_release(track.merge('releases' => [draft, {'versionCodes' => ['28'], 'status' => 'completed'}]), 'alpha', '27') }
rejects { completed_closed_release(track.merge('releases' => [draft, old.merge('status' => 'draft')]), 'alpha', '27') }
puts 'Existing Play release activation checks passed'
