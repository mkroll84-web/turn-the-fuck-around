# First milestone

Native Android app: Kotlin 2.0.21, Jetpack Compose, Android API 26+, Gradle 8.9, AGP 8.7.3, compile/target API 35. Java 17 or 21 can build it.

- `domain/Navigation.kt`: provider contract, route provenance, threshold policy, geography used only for deviation, and personality text. No Android dependencies.
- `data/DemoNavigationProvider.kt`: fictional destination catalog and simulated routing. Rejects all live routing requests.
- `ui/NavigationViewModel.kt`: state, saved preferences, simulation, reroute coordination. Swap the provider binding here when a real adapter is available.
- `ui/DemoMap.kt`: offline fictional diagram. It is deliberately not a source of legal road connectivity.
- `ui/MapPanel.kt`: OpenStreetMap rendering for phone GPS mode via osmdroid; replace independently of routing. OSM tiles need internet. osmdroid is archived upstream; this lightweight MVP renderer should be replaced with a supported provider SDK before production.
- `MainActivity.kt`: foreground-only Android location and runtime permissions. No background tracking or stored location history.

## Safety boundary

WTF policy compares routes supplied by the same provider for the same destination. Candidates need explicit travel-direction legality and restriction coverage; live candidates must carry PROVIDER_VERIFIED trust. Both configurable savings thresholds must be met. Otherwise the normal provider reroute wins. Geometry detects deviations only and never generates maneuvers. Demo fixtures carry SIMULATION_ONLY trust and are blocked in live mode.

The policy is a contract, not proof that a provider supports every restriction. A future adapter must establish whether provider data handles one-way roads, divided roads, medians, U-turn and other turn restrictions, restricted/private access, and heading. Unknown legality means reject that alternative. Do not label a route verified based merely on a successful HTTP response. Alternative routes must come from supported provider routing requests; never join points across roads to manufacture a shortcut.

The MVP does not perform live navigation. GPS map mode shows location; demo mode animates fictional routes, progress, maneuvers, arrival, and reroutes. “Miss a turn” supplies three accurate off-route fixes to the detector. Accuracy filtering plus consecutive samples prevents single noisy fixes from causing a reroute. Demo ETA and route times are fictional and accelerated. Provider failure leaves the previous route visible with an error; there is no silent unsafe fallback. Settings persist; active demo sessions do not survive process death.

## Real provider follow-up

A reasonable next integration is Mapbox Navigation SDK for Android, using a Mapbox account and the SDK's Directions/Search services. Before selecting it, confirm current licensing, pricing, road-restriction coverage and support for alternative reconnect routes. Do not promise private-road/U-turn completeness from a generic Directions response.

Mapbox Android dependency downloads may require a secret download token with `DOWNLOADS:READ`. Store that token only in the user's Gradle credentials outside version control (or secure environment bindings). A runtime public access token belongs in ignored `local.properties` and is injected into build configuration by the future adapter. Public mobile tokens are extractable from an APK; scope them appropriately. No Mapbox dependency, token requirement, or fake integration is included in this milestone. The demo needs no API key.

Future implementation: add a provider adapter in `data`, bind it in the ViewModel's composition point, connect live accurate GPS fixes with travel bearing to a navigation session, and delegate guidance/map matching/maneuver progress to the SDK. Add foreground service, spoken instructions, GPS-staleness handling, live deviation/recovery tests, provider contract tests, and device testing before real driving. Compare normal and alternative remaining journey totals, not straight-line distance. Provider-certified legality is mandatory for alternatives; absence of an alternative is a valid normal-reroute result.
