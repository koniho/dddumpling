#!/bin/sh
# Requires an installed current Dev APK; never builds, installs, or resets progress.
set -eu
cd "$(dirname "$0")/.."
transport=${GATOR_TRANSPORT:-rish}
case "$transport" in rish|adb) ;; *) echo 'GATOR_TRANSPORT must be rish or adb' >&2; exit 1;; esac
remote() {
    if [ "$transport" = rish ]; then RISH_APPLICATION_ID=com.termux rish -c "$1";
    else adb shell "$1"; fi
}
scene=${PROFILE_SCENE:-gator}
case "$scene" in gator) scene_prefix='';; slime) scene_prefix=slime-;; divide) scene_prefix=divide-;; underwater) scene_prefix=underwater-;; crystal) scene_prefix=crystal-;; *) echo 'PROFILE_SCENE must be gator, slime, divide, crystal or underwater' >&2; exit 1;; esac
if [ "${PROFILE_MONOCHROME:-0}" = 1 ]; then scene_prefix="${scene_prefix}mono-"; fi
mode=${1:-both}
case "$mode" in both) modes='vector cached';; vector|cached) modes=$mode;;
    *) echo 'usage: sh tools/profile-gator.sh [both|vector|cached]' >&2; exit 1;; esac
out="build/$scene-profile-$(date +%Y%m%d-%H%M%S)-$$"
mkdir -p "$out"
app=com.dddumpling.game.dev
activity=$app/com.dddumpling.game.MainActivity
token="$scene-$(date +%s)-$$"
started=false
cleanup() {
    if [ "$started" = true ]; then
        remote "am start --activity-single-top -n $activity --es gator_profile stop" >"$out/stop.txt" 2>&1 || :
    fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM
remote 'id; wm size; dumpsys thermalservice' >"$out/device.txt" 2>&1
start() {
    started=true
    remote "am start --activity-single-top -n $activity --es gator_profile $1 --es gator_profile_token $2" >"$out/launch-$2.txt" 2>&1
    # Confirm this binary supports the hook; a stale APK must not silently profile normal play.
    for attempt in 1 2 3 4 5; do
        sleep 1
        remote 'logcat -d -s GatorProfile:I' >"$out/ready.txt" 2>&1
        if grep -Fq "ready=$1 token=$2" "$out/ready.txt"; then return; fi
    done
    echo 'Replay did not start. Install the current developer APK and keep the device unlocked.' >&2
    exit 1
}
for variant in $modes; do
    echo "$variant: warming the complete 36-second replay…"
    start "$scene_prefix$variant" "$token-$variant-warm"
    sleep 36
    # Start again at entry so both captures contain the same full loop and prewarmed artwork.
    remote "dumpsys gfxinfo $app reset" >"$out/$variant-reset.txt" 2>&1
    echo "$variant: capturing entry and scripted combat…"
    start "$scene_prefix$variant" "$token-$variant-measure"
    sleep 38
    remote "dumpsys gfxinfo $app framestats" >"$out/$variant-gfxinfo.txt" 2>&1
    remote 'logcat -d -s GatorProfile:I' >"$out/$variant-replay.txt" 2>&1
    if ! grep -Fq "loop=1 token=$token-$variant-measure" "$out/$variant-replay.txt"; then
        echo 'Capture did not finish a full replay. Keep the app foregrounded; inspect logs for stalls.' >&2
        exit 1
    fi
    remote 'dumpsys thermalservice' >"$out/$variant-thermal.txt" 2>&1
    grep -E 'Total frames|Janky frames|percentile|GPU HISTOGRAM' "$out/$variant-gfxinfo.txt" || :
done
echo "Raw frame timings and device conditions: $out"
