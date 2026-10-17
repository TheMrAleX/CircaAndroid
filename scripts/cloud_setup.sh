#!/usr/bin/env bash
# Cloud setup; preserve the existing checkout and never create a worktree.
set -eu
cd /workspace/CircaAndroid
TOOLS=/workspace/toolchains
export JAVA_HOME="$TOOLS/jdk-17.0.14+7"
export ANDROID_HOME="$TOOLS/android"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME=/workspace/.android
export GRADLE_USER_HOME=/workspace/.gradle
mkdir -p "$TOOLS" "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  curl -fL --retry 2 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.14_7.tar.gz' -o /tmp/aproximado-jdk17.tar.gz
  echo 'a3af83983fb94dd7d11b13ba2dba0fb6819dc2caaf87e6937afd22ad4680ae9a  /tmp/aproximado-jdk17.tar.gz' | sha256sum -c -
  tar xzf /tmp/aproximado-jdk17.tar.gz -C "$TOOLS"
fi
export PATH="$JAVA_HOME/bin:$PATH"
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -fL --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/aproximado-android-tools.zip
  echo '2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258  /tmp/aproximado-android-tools.zip' | sha256sum -c -
  unpack=$(mktemp -d /tmp/aproximado-tools.XXXXXX)
  unzip -q /tmp/aproximado-android-tools.zip -d "$unpack"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$unpack/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rmdir "$unpack"
fi
# Configure Java's HTTPS proxy and its trusted system CA store without exposing credentials.
python3 - <<'PY'
import os
from pathlib import Path
from urllib.parse import urlparse
u=urlparse(os.environ.get('HTTPS_PROXY',''))
p=Path(os.environ['GRADLE_USER_HOME'])/'gradle.properties'
s=p.read_text() if p.exists() else ''
updates={}
if u.hostname:
    updates.update({'systemProp.https.proxyHost':u.hostname,'systemProp.https.proxyPort':str(u.port or 8080),'systemProp.http.proxyHost':u.hostname,'systemProp.http.proxyPort':str(u.port or 8080)})
if Path('/etc/ssl/certs/java/cacerts').exists():
    updates['systemProp.javax.net.ssl.trustStore']='/etc/ssl/certs/java/cacerts'
lines=[line for line in s.splitlines() if line.split('=',1)[0] not in updates]
p.write_text('\n'.join(lines+[f'{key}={value}' for key,value in updates.items()])+'\n')
PY
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
proxy_args=()
if [ -n "${HTTPS_PROXY:-}" ]; then
  proxy_host=$(python3 -c 'import os,urllib.parse;print(urllib.parse.urlparse(os.environ["HTTPS_PROXY"]).hostname)')
  proxy_port=$(python3 -c 'import os,urllib.parse;print(urllib.parse.urlparse(os.environ["HTTPS_PROXY"]).port or 8080)')
  proxy_args=(--proxy=http --proxy_host="$proxy_host" --proxy_port="$proxy_port")
fi
if [ ! -f "$ANDROID_HOME/platforms/android-35/android.jar" ] || [ ! -x "$ANDROID_HOME/build-tools/35.0.0/aapt2" ]; then
  # No pipefail: sdkmanager status is authoritative; yes normally ends with SIGPIPE.
  yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" "${proxy_args[@]}" --licenses >/dev/null
  "$SDKMANAGER" --sdk_root="$ANDROID_HOME" "${proxy_args[@]}" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
fi
python3 scripts/validate_questions.py
python3 scripts/generate_database.py
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
