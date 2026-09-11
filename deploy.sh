#!/data/data/com.termux/files/usr/bin/bash
# Build, install and launch in one go.
#
# With adb connected over Wireless debugging this is fully unattended — no installer
# dialog, no taps. Without it, falls back to handing the APK to the package installer,
# which needs one tap. See README.md for the one-time pairing.
set -euo pipefail
cd "$(dirname "$0")"

APK=hexatype.apk
PKG=com.dddumpling.game
ACTIVITY=$PKG/.MainActivity

if [ "$#" -eq 0 ]; then set -- --developer; fi
./build.sh "$@"

notify() {
    # Only if the Termux:API app is present; the CLI blocks forever without it.
    if command -v termux-notification >/dev/null 2>&1; then
        timeout 5 termux-notification --title "Hexatype" --content "$1" \
            --id hexatype-deploy >/dev/null 2>&1 || true
    fi
}

if timeout 15 adb devices 2>/dev/null | grep -qw device; then
    echo ">> installing over adb"
    # -r keeps app data, so the best score and settings survive the update.
    timeout 180 adb install -r "$APK"
    timeout 30 adb shell am start -n "$ACTIVITY" >/dev/null
    echo ">> installed and launched, no taps needed"
    notify "updated and launched"
else
    echo ">> no adb device paired; using the package installer (one tap needed)"
    echo "   pair once with Wireless debugging to make this unattended - see README.md"
    termux-open "$(pwd)/$APK"
    notify "APK ready - confirm the install prompt"
fi
