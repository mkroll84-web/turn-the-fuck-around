# Accounts and keys

You need **no account or API key to try this demo**.

Real driving navigation is a later milestone. A proposed provider is **Mapbox** (https://account.mapbox.com/). Its navigation service may have usage charges. We must confirm its current pricing and road-restriction support before connecting it.

When that integration starts, the first step for you will be creating a Mapbox account. You do not need to do that now.

The integration will need:

- A public access token for map/search/routing requests. The future adapter will read `MAPBOX_PUBLIC_TOKEN` from ignored `local.properties`, through build configuration.
- If required by Mapbox's current Android SDK distribution, a secret download token with `DOWNLOADS:READ`. That goes in secure Gradle credentials outside the repository, never application resources or a chat message.

These names are planned configuration points, **not working integration settings yet**. Adding keys alone will not enable real navigation in this MVP. Provider-specific code belongs in `app/src/main/java/com/ttfa/data`; the contract is `domain/Navigation.kt`. No live provider is currently linked.
