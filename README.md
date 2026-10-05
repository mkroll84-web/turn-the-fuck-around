# TURN THE FUCK AROUND

**Navigation with common fucking sense.**

Native Android MVP built with Kotlin and Jetpack Compose. No API key is needed for the working demo.

## Try it

Install the debug APK on an Android phone (Android 8 or newer). Open the app, pick a demo destination, and tap **Start Navigation · demo**. Tap **Miss a turn** to compare the normal reroute against the fictional shorter reconnect route. Settings lets you choose a personality, disable WTF Mode, and adjust its minimum time/distance savings.

The offline demo map and every demo maneuver are fictional. Do not follow them on real roads. **Live driving navigation is not connected yet.** Turn off Demo drive in Settings and tap Enable phone GPS to see your real position on an OpenStreetMap map. GPS requires phone permission and enabled location services; map tiles need internet. Destination search currently searches three demo destinations only.

## Build

Open this folder in Android Studio and let it finish downloading Android SDK 35 and dependencies. Select a phone or emulator and press Run. No secrets or paid account are needed.

Command-line development requires Java 17/21 and an Android SDK with platform 35 and build-tools 35.0.0. Set `ANDROID_HOME` or put `sdk.dir=/your/android/sdk` in ignored `local.properties`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Cloud development uses the existing checkout, not a new worktree. See `scripts/cloud-build.sh` for the current cloud toolchain setup and `docs/ARCHITECTURE.md` for boundaries, safety policy, and the future real-routing integration. Account and key details are in `docs/PROVIDER_SETUP.md`.

## Included

Home, offline demo map, destination selection, route line and driver marker, animated navigation, ETA/distance/maneuver, pause/resume, arrival, off-route detection, normal reroute fallback, WTF route comparison, personalities, saved settings, and foreground phone GPS with permission-denial handling. The provider and map renderer are separate.

No spoken directions, live road search/routing, background tracking, traffic, or production navigation yet. Comedy is display text only and cannot alter directions. Unit tests exercise safety rejection, thresholds, deviation accuracy, and provider behavior.

Validation evidence and limits: `docs/VALIDATION.md`. The debug APK was launched and tested on an Android 9 emulator; the safety suite has 11 passing tests.
