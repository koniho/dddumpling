#!/bin/sh
# Generate a compile-time constant; never read developer mode from saved data or a runtime toggle.
set -eu
case "${2:-}" in true|false) ;; *) echo 'Expected true or false developer flag' >&2; exit 1 ;; esac
build_id="${DDDUMPLING_BUILD_ID:-$(date -u +%Y%m%d%H%M%S%N)}"
case "$build_id" in *[!A-Za-z0-9._-]*) echo 'Invalid build ID' >&2; exit 1 ;; esac
flags_dir="$1/com/dddumpling/game"
mkdir -p "$flags_dir"
cat > "$flags_dir/BuildFlags.java" <<JAVA
package com.dddumpling.game;
final class BuildFlags {
    static final boolean DEVELOPER = $2;
    static final String BUILD_ID = "$build_id";
    private BuildFlags() {}
}
JAVA
