#!/bin/sh
# Generate a compile-time constant; never read developer mode from saved data or a runtime toggle.
set -eu
case "${2:-}" in true|false) ;; *) echo 'Expected true or false developer flag' >&2; exit 1 ;; esac
flags_dir="$1/com/sram/hexatype"
mkdir -p "$flags_dir"
cat > "$flags_dir/BuildFlags.java" <<JAVA
package com.sram.hexatype;
final class BuildFlags {
    static final boolean DEVELOPER = $2;
    private BuildFlags() {}
}
JAVA
