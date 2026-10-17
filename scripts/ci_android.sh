#!/usr/bin/env bash
# GitHub Ubuntu 24.04 runners already provide ANDROID_HOME and command-line tools.
set -eu
: "${ANDROID_HOME:?El runner debe proporcionar ANDROID_HOME}"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [ ! -f "$ANDROID_HOME/platforms/android-35/android.jar" ]; then
  "$SDKMANAGER" --sdk_root="$ANDROID_HOME" 'platforms;android-35'
fi
if [ ! -x "$ANDROID_HOME/build-tools/35.0.0/apksigner" ]; then
  "$SDKMANAGER" --sdk_root="$ANDROID_HOME" 'build-tools;35.0.0'
fi
test -f "$ANDROID_HOME/platforms/android-35/android.jar"
test -x "$ANDROID_HOME/build-tools/35.0.0/apksigner"
