#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Reuse the existing isolated checkout. Do not create a worktree.
export ANDROID_HOME=/workspace/toolchains/android
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export GRADLE_USER_HOME=/workspace/.gradle
mkdir -p /workspace/toolchains "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
source scripts/cloud-java.sh
if [[ ! -x /workspace/toolchains/gradle-8.9/bin/gradle ]]; then
    curl -fsSL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o /workspace/toolchains/gradle.zip
    curl -fsSL https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256 -o /workspace/toolchains/gradle.sha256
    (cd /workspace/toolchains; echo "$(cat gradle.sha256)  gradle.zip" | sha256sum -c -; unzip -q -o gradle.zip)
fi
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
    curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /workspace/toolchains/android-tools.zip
    # Official checksum from Google's repository2-1.xml for cmdline-tools;12.0.
    echo 'd313adb7aedccf6cf0cfca51ec180f0059f5f8f8  /workspace/toolchains/android-tools.zip' | sha1sum -c -
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    unzip -q /workspace/toolchains/android-tools.zip -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    java_install=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p')
    cp "$java_install/lib/security/cacerts" /workspace/toolchains/java-cacerts
    for cert in /usr/local/share/ca-certificates/*.crt; do
        [[ -f "$cert" ]] || continue
        alias_name="ttfa-$(basename "$cert")"
        keytool -importcert -noprompt -trustcacerts -alias "$alias_name" -file "$cert" -keystore /workspace/toolchains/java-cacerts -storepass changeit
    done
    read -r proxy_host proxy_port < <(python3 -c 'import os,urllib.parse; u=urllib.parse.urlparse(os.environ["HTTPS_PROXY"]); print(u.hostname, u.port or 80)')
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttps.proxyHost=$proxy_host -Dhttps.proxyPort=$proxy_port -Dhttp.proxyHost=$proxy_host -Dhttp.proxyPort=$proxy_port -Djavax.net.ssl.trustStore=/workspace/toolchains/java-cacerts"
fi
# sdkmanager stops reading once all licenses are accepted, so avoid a pipefail from yes.
set +o pipefail
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
license_status=${PIPESTATUS[1]}
set -o pipefail
[[ "$license_status" == 0 ]]
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
scripts/cloud-build.sh assembleDebug testDebugUnitTest lintDebug
