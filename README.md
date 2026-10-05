# TURN THE FUCK AROUND

**Navigation with common fucking sense.**

Native Android MVP built with Kotlin and Jetpack Compose. No API key is needed for the working demo.

## Try it

Install the debug APK on an Android phone (Android 8 or newer). Open the app, pick a demo destination, and tap **Start Navigation · demo**. Tap **Miss a turn** to compare the normal reroute against the fictional shorter reconnect route. Settings lets you choose a personality, disable WTF Mode, and adjust its minimum time/distance savings.

The offline demo map and every demo maneuver are fictional. Do not follow them on real roads. Turn off Demo drive to type real addresses, businesses, landmarks, or cities and choose Google Places autocomplete suggestions. A restricted Google Places key and billing are required; add the key in Settings without rebuilding. [Plain-English setup for Melissa](README_FOR_MELISSA.md).

Real Start Navigation opens Google Maps for driving directions. In-app live routing/WTF comparison is not connected yet. Phone GPS and the OpenStreetMap location map remain available. Location permission is optional for search; fresh permitted fixes bias results nearby. Google results and selected destinations appear separately from the OpenStreetMap map.

## Build

Open this folder in Android Studio and let it finish downloading Android SDK 35 and dependencies. Select a phone or emulator and press Run. No secrets or paid account are needed.

Command-line development requires a full Java 17/21 JDK and an Android SDK with platform 35 and build-tools 35.0.0. Set `ANDROID_HOME` or put `sdk.dir=/your/android/sdk` in ignored `local.properties`. `local.properties.example` documents the optional build-time key; do not commit real keys.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Cloud development uses the existing checkout, not a new worktree. See `scripts/cloud-build.sh` for the current cloud toolchain setup and `docs/ARCHITECTURE.md` for boundaries, safety policy, and the future real-routing integration. Account and key details are in `docs/PROVIDER_SETUP.md`.

## Included

Home, offline demo map, real Google Places search/selection, Google Maps directions handoff, demo route line and driver marker, animated navigation, ETA/distance/maneuver, pause/resume, arrival, off-route detection, normal reroute fallback, WTF route comparison, personalities, saved settings, and foreground phone GPS with permission-denial handling. Search, routing, and map rendering have separate provider boundaries.

No in-app spoken directions, live road routing, background tracking, traffic, or production navigation yet. Comedy is display text only and cannot alter directions. Unit tests exercise safety rejection, thresholds, deviation accuracy, search state/cancellation, location bias, destination handoff, and key encryption.

Validation evidence and limits: `docs/VALIDATION.md`. Download the installable debug APK from `builds/TURN-THE-FUCK-AROUND.apk` on GitHub using **Download raw file**.

## Paid app and Voo (0.3)

The final Google Play app is planned as US $3 paid upfront; Voo is a separate US $2 permanent one-time product, `voo_mode_unlock`. There is no base-app in-app checkout or subscription. The app uses Billing Library 9.1.0, signed purchase verification, acknowledgement and ownership restoration. Configure the public Play license key and active Buy option before real checkout. The debug-only Settings override tests Voo without creating a purchase; release builds cannot enable it. See [Melissa's setup guide](README_FOR_MELISSA.md#paid-app--voo-mode-version-03) for pricing, license testers and restoration, and [architecture](docs/ARCHITECTURE.md) for the production backend work still required. Voo currently adds demo text, not a custom navigation voice.

Aqua/pink branding includes system, light and dark themes, branded map/routes, and Voo purchase screens. `scripts/cloud-build.sh testReleaseUnitTest` also runs a release-variant test proving development access cannot be enabled.
