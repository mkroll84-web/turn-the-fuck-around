# Provider configuration

Real destination search uses Google Places SDK for Android **6.0.2**, initialized for **Places API (New)**. Enable that API in a billing-enabled Google Maps Platform project. Restrict the API key to Android package `com.ttfa` and the APK signing certificate's SHA-1; restrict API access to Places API (New).

[README_FOR_MELISSA.md](../README_FOR_MELISSA.md) has exact plain-English account, API, key-restriction, certificate and in-app setup steps. Users can paste the restricted key in Settings → Google Places, without rebuilding. The key is encrypted in private app storage using Android Keystore AES-GCM. Demo drive makes no Places requests.

Developers may instead use `GOOGLE_PLACES_API_KEY` in ignored `local.properties` (see `local.properties.example`) or as an environment variable when building. Environment values override local values; app-saved keys override both. Never commit a real key. APK-embedded client keys are extractable, so restrictions remain essential. The distributed debug APK has a blank build key.

The search provider belongs in `data/places`, behind `domain/DestinationSearch.kt`. The app uses current `DISPLAY_NAME`, `FORMATTED_ADDRESS`, and `LOCATION` fields, autocomplete session tokens, and no search-category filters. A fresh permitted GPS fix biases results; searches without location still work. Google result attribution is shown on a non-map screen separate from OSM.

Real Start Navigation opens Google Maps using resolved destination coordinates and place ID. This does not require our project to enable a Google routing API. In-app live routing remains a separate milestone behind `domain/Navigation.kt`; no provider-backed live WTF alternatives are claimed. Future routing credentials and safety coverage depend on the routing SDK chosen later.
