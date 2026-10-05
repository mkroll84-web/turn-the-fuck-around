# MVP validation

## Passed in the current cloud machine

- `scripts/cloud-setup.sh`: installed/refreshed dependencies and completed the build, unit tests and lint. A second build reused valid cached outputs.
- `scripts/cloud-build.sh assembleDebug testDebugUnitTest lintDebug`: installable Kotlin/Compose Android APK; 11 tests executed, 0 failures, 0 errors, 0 skipped.
- Latest UI accessibility changes: `scripts/cloud-build.sh assembleDebug lintDebug` passed.
- Android lint: 0 errors, 5 warnings (four newer-dependency suggestions and Android backup-configuration guidance). The dependency versions are pinned; the application is a development MVP.
- APK signature verification via build-tools 35.0.0 `apksigner verify --verbose`: valid APK Signature Scheme v2 signature.
- `python3 scripts/emulator-smoke.py --adb /workspace/toolchains/android/platform-tools/adb --serial emulator-5556`: passed against the actual debug APK on Android 9 (API 28).
- Git whitespace check, shell script syntax checks and Python smoke-test syntax check passed.

## Android screen checks

The test launched fresh app state, selected a destination, started navigation, checked ETA, paused simulation, injected an off-route event, and verified the WTF win and exact savings. It disabled WTF Mode, selected Full roast, and verified the normal reroute and personality message. It force-stopped/restarted the app and verified both saved preferences. It entered GPS map mode, declined Android's location permission, checked the resulting status, and successfully returned to demo navigation.

The runner reads fresh accessibility hierarchies; transient null-root results are retried instead of reading stale screen data. Switches and personality controls have explicit spoken labels. The runner refuses to clear data on a physical device.

Screenshots from the passing run are generated in `app/build/reports/smoke/`: `home.png`, `wtf-wins.png`, `normal-fallback.png`, and `gps-permission-denied.png`. Home, WTF win and normal fallback screenshots were visually inspected. These artifacts are generated outputs, not source files.

## Environment observations and limits

Software emulation is slow because this machine has no `/dev/kvm`. Android 15/API 35 emulation was attempted, but first startup was too slow for practical UI work; Android 9 was used for the completed runtime test. The initial Android 9 launch encountered a System UI startup ANR, then completed after dismissing the system dialog. The final fresh-data test passed after startup settled. There were no app fatal exceptions in the final AndroidRuntime log. API 35 compilation passed; API 35 app runtime was not validated.

Phone GPS position/accuracy needs a real-device check. Location permission denial and GPS map screen construction were tested; a real GPS fix was not verified. Cloud OSM tile access was initially denied by network policy. Required tile hostnames have been saved to the environment draft, but draft saving does not apply them to this instance. Offline demo navigation works without that access. Physical-phone map tile connectivity still needs verification.

Live road routing, live destination search, voice guidance, traffic, background navigation, and provider-backed legal reconnect routes are not implemented yet. Routes and destination search in this milestone are explicitly fictional demo data. The safety policy is tested, but it does not prove any real road maneuver is legal. No live driving readiness is claimed.

Reusable install/start instructions and network additions have been saved as a configuration draft. Publication and restoration in a new task have not been performed or verified.
