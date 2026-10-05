# Voo purchases and branding validation — version 0.3.0

- October 5, 2026: `scripts/cloud-build.sh assembleDebug assembleRelease testDebugUnitTest testReleaseUnitTest lintDebug lintRelease` completed successfully against Billing Library 9.1.0 (current Google Maven metadata).
- Debug: 31 tests; release: 32 tests. Zero failures, errors or skips. Includes prior navigation/search/encryption tests, valid/tampered/wrong-key RSA purchase signatures, invalid verification configuration, locked Voo behavior, all unlocked Voo roast levels, and a release-only test that cannot enable development access.
- Debug lint: 0 errors / 21 warnings. Release lint: 0 errors / 20 warnings. Warnings cover dependency/target age, backup guidance and optional Kotlin convenience APIs; no checks were disabled.
- Both build variants compiled. Release bytecode inspection confirms `DevelopmentUnlock.enabled()` always returns false, `set(true)` does nothing and no preferences are read. The debug implementation is excluded from release compilation.
- The deliverable is the signed **debug** APK at `builds/TURN-THE-FUCK-AROUND.apk`, `com.ttfa`, version 0.3.0 / code 3, Android 8+ (min SDK 26). It retains the same signing certificate as installed MVP versions. SHA-256: `bbe0b2ffd23cb888c2fe1543f74c49d81fd2bc45394eda0193b6d4d997488555`.
- No real Google Places key or Play license key is configured in the distributed APK. Demo works without them. Real checkout is disabled without a valid public verification key and eligible Play Buy offer.

## Version 0.3 device checks

Android 9/API 28 checks completed in stages using the signed debug APK:

- Locked Voo opened the branded paywall with the one-time price, base-app/add-on disclosure, no-subscription statement and sample text. Unconfigured checkout was disabled. Close returned to Settings.
- The explicitly named development switch was off, unlocked Voo when toggled, reported development access rather than a purchase, and survived a force-stop/restart. The unlocked Voo radio option could then be selected without a paywall.
- Absolutely Foul selection persisted. Demo destination selection, navigation/ETA, pause, missed-turn injection, WTF MODE WINS and the gated Voo Foul message completed.
- Dark preference persisted and its rendered home/map were visually checked. Explicit Light selection under Appearance persisted and a light settings capture was produced.

The complete UI runner did not finish in one uninterrupted invocation. API 28's software-emulated UiTestAutomationBridge repeatedly returned null roots. Checks were resumed in stages after correcting harness locators for offscreen headings, unnamed switch ancestors and duplicate Light labels (roast intensity versus appearance). The current harness waits for fresh trees, names the development switch, scopes appearance selectors, and handles signed bounds/visible viewports. No failed assertion was relabeled as passed; the final individual checks above were repeated directly. Payment/account tests below remain unrun.

Real Google Play payments, cancellation, delayed payment, acknowledgement/refund outcomes, and account-linked reinstall/change-phone restoration have **not** been exercised against a Play Console internal track: this task has no activated product/license-test account. Client API integration compiles against the real SDK; production never substitutes simulated purchases. Local signature verification is implemented, but server-side Play Developer API verification/RTDN remains a public-release prerequisite. Custom Voo voice and in-app real road navigation remain future milestones.

## Historical destination-search validation (0.2.0)

# Destination search validation — version 0.2.0

## Build and automated checks

- `scripts/cloud-build.sh assembleDebug testDebugUnitTest lintDebug` completed successfully on October 5, 2026 using a full Java 21 JDK, Gradle 8.9, AGP 8.7.3, Kotlin 2.4.0, Android SDK 35, and Google Places SDK 6.0.2.
- 27 unit tests executed: 11 existing navigation safety/provider tests, 13 destination-search tests, and 3 encryption tests. Zero failures, errors, or skipped tests.
- Search tests cover full and partial addresses, businesses, landmarks and cities passing unchanged to the provider, typing debounce, resolved ID/name/address/coordinates, invalid details, stale autocomplete/detail responses, cancellation on mode reset, optional fresh permission-backed location bias, missing-key errors without demo results, and the Google Maps handoff URL.
- Android lint completed with 0 errors and 14 warnings: older target/dependency versions, backup configuration guidance, and optional Kotlin convenience APIs. No checks were disabled. Current lint 31.13.2 and Lifecycle 2.10.0 resolve the Kotlin analysis API mismatch introduced by current Compose/Places dependencies.
- Build-tools 35.0.0 `apksigner verify --verbose --print-certs` confirmed a valid v2 signature and the same signing certificate as version 0.1.0. SHA-1: `14:FD:5D:C5:5A:00:A7:74:01:8B:5C:B3:09:EA:80:37:E0:9C:88:7C`.
- The generated BuildConfig was checked: the distributed APK's build-time Google key is blank. Real keys are not in Git, examples, logs, or the distributed APK.
- Shell/Python syntax checks and Git whitespace checks passed. The retained full JDK's `java`, `javac`, and `jlink` were checked. Both cloud setup/build scripts activate that JDK; verified Debian packages are extracted under `/workspace` without changing system files.

## Device screen checks

Android 9/API 28 screen checks ran in stages against the version 0.2.0 debug APK:

- Demo launch, destination selection, navigation/ETA, pause, missed-turn detection, WTF win/exact savings, normal reroute with WTF disabled, Full roast, and preferences after force-stop/restart completed. Demo/settings code remained unchanged during the later live-input correction.
- Runtime key configuration rejected the obvious placeholder, encrypted a test-only value in Android private storage, retained a configured key after app restart, and removed it successfully. No real Google key or Google API calls were used in this test.
- On the final APK, the live input accepted the complete `1600 Amphitheatre Parkway` address, displayed the missing-key message, and did not show demo suggestions. The actual Start Navigation control was disabled. Denying location permission produced the expected status, and Try demo navigation returned to the offline fictional destination catalog.

The all-in-one runner was interrupted by a standalone UIAutomator/app_process SIGTRAP under software emulation; the affected checks were rerun separately. The current harness supports `--phase demo`, `--phase search`, and `--phase input`. It retries the known dump-tool trap without reusing old UI data, reads actual clickable/checkable parents rather than decorative child labels, waits for the emulated keyboard, and uses paced normal typing. Bulk ADB text injection dropped characters on this emulator; bulk-injection throughput is not claimed. The final disabled-navigation/GPS/demo-return checks were resumed after the full-address assertion had passed and completed with exit status 0. No assertions were disabled.

Screenshots are generated under `app/build/reports/smoke/`: home, WTF win, normal fallback, live-search-needs-key, and GPS-permission-denied. Generated test artifacts remain outside version control.

## Unrun checks and limits

**Real Google autocomplete and place-detail responses have not been tested against a billing-enabled Google project.** No Google key exists in this task. The real SDK integration compiles against the downloaded current SDK; unit providers simulate asynchronous outcomes only inside the test suite. The app does not substitute fake results for live Google searches. Once Melissa creates a project, enables Places API (New), restricts her key, and saves it in Settings, full-address/business/landmark/city results need a physical-phone check. Instructions are in `README_FOR_MELISSA.md`.

The optional search bias needs a fresh permitted phone fix. Actual GPS accuracy and real OSM map tile connectivity remain physical-device checks. Compilation targets API 35; API 35 runtime and physical driving were not tested. Software emulation is slow without `/dev/kvm`; the emulator encountered a System UI startup ANR before the completed app screen run. That system dialog was dismissed. App fatal exceptions were checked separately.

Real Start Navigation hands the selected coordinates and place ID to Google Maps, with browser fallback. It does not provide in-app road routing, spoken directions, traffic, background tracking or provider-verified live WTF alternatives. Those remain separate milestones. Google results are shown on a non-map panel, independently of the OSM GPS map, with the SDK's Google Maps attribution asset.

The existing checkout contains WTF Mode, three personalities, savings thresholds and the offline demo. Voo Mode, separate roast-intensity controls, a Fuck That button and a side-of-road guidance implementation were not present in that baseline; this change does not remove them or pretend to implement them.

Updated cloud startup instructions were saved as a configuration draft, including current SDK/search behavior and the full JDK activation. Saving the draft does not apply or publish it. Publication/restoration into a new task was not performed or claimed. To reuse the saved cloud configuration later, review/save it in environment settings and publish the environment; this is not required to install the APK or configure search on a phone.
