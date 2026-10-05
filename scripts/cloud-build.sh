#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-/workspace/toolchains/android}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-/workspace/toolchains/android-user}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
mkdir -p "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
args=()
# Java does not automatically honor HTTPS_PROXY. Keep TLS verification enabled.
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    read -r proxy_host proxy_port < <(python3 -c 'import os,urllib.parse; u=urllib.parse.urlparse(os.environ["HTTPS_PROXY"]); print(u.hostname, u.port or 80)')
    args+=("-Dhttps.proxyHost=$proxy_host" "-Dhttps.proxyPort=$proxy_port" "-Dhttp.proxyHost=$proxy_host" "-Dhttp.proxyPort=$proxy_port")
fi
if [[ -f /workspace/toolchains/java-cacerts ]]; then
    args+=("-Djavax.net.ssl.trustStore=/workspace/toolchains/java-cacerts")
fi
if [[ -x /workspace/toolchains/gradle-8.9/bin/gradle ]]; then
    /workspace/toolchains/gradle-8.9/bin/gradle "${args[@]}" "${@:-assembleDebug}" --console=plain
else
    ./gradlew "${args[@]}" "${@:-assembleDebug}" --console=plain
fi
