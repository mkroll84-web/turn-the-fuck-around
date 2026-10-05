#!/usr/bin/env bash
# Sourced by cloud scripts: Android's Java compilation needs a full JDK, not a JRE.
ttfa_jdk=/workspace/toolchains/jdk-21-debian/usr/lib/jvm/java-21-openjdk-amd64
if [[ ! -x "$ttfa_jdk/bin/jlink" ]]; then
    ttfa_system_java=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p')
    if [[ -x "$ttfa_system_java/bin/jlink" && -x "$ttfa_system_java/bin/javac" ]]; then
        ttfa_jdk="$ttfa_system_java"
    else
        # Verified Debian 13 amd64 packages, extracted without changing system files.
        mkdir -p /workspace/toolchains/jdk-21-debian
        for ttfa_package in jdk jre; do
            ttfa_archive="/workspace/toolchains/jdk-21-debian/openjdk-21-${ttfa_package}-headless.deb"
            curl -fsSL "https://deb.debian.org/debian/pool/main/o/openjdk-21/openjdk-21-${ttfa_package}-headless_21.0.12.1+1-1~deb13u1_amd64.deb" -o "$ttfa_archive"
            if [[ "$ttfa_package" == jdk ]]; then
                ttfa_checksum=f3abafb6c644b03df042824e707cd211ea33254761d7f7b75be3f8dc0df97c7a
            else
                ttfa_checksum=e95f36193e45464ac758e5436f940bf3eaecc77de67188275a42104f58aa7674
            fi
            echo "$ttfa_checksum  $ttfa_archive" | sha256sum -c -
            dpkg-deb -x "$ttfa_archive" /workspace/toolchains/jdk-21-debian
        done
    fi
fi
export JAVA_HOME="$ttfa_jdk"
export PATH="$JAVA_HOME/bin:$PATH"
unset ttfa_jdk ttfa_system_java ttfa_package ttfa_archive ttfa_checksum
