#!/bin/bash
# Sourced by scripts; JAVA_HOME can point to a separately installed JDK 21.
IOS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(dirname "$IOS_ROOT")"
if [ -z "${JAVA_HOME:-}" ]; then
    if [ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]; then
        export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
    else
        export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
J2OBJC_HOME="${J2OBJC_HOME:-$IOS_ROOT/vendor/j2objc-3.1/dist}"
