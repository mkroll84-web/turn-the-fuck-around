# Real destination search: setup for Melissa

The new APK supports real addresses, partial addresses, businesses, landmarks, and cities through Google Places. Google requires your own Google Maps Platform project, billing account, and API key. **Demo drive still works offline without an account or key.**

You can add the key inside the installed app. You do not have to edit code or rebuild the APK.

## Do these steps in order

### 1. Create your Google project

Open [Google Cloud Console](https://console.cloud.google.com/) and sign in with your Google account. At the top, click the project selector, then **New Project**. Name it **TURN THE FUCK AROUND**, click **Create**, and select that project when creation finishes.

### 2. Turn on billing

Open the menu, choose **Billing**, and link a billing account to this project. Google may ask for payment details. Google Places has usage-based charges; review its [pricing](https://developers.google.com/maps/billing-and-pricing/pricing) before using it. Billing budgets notify you about spending; they do not automatically stop spending. You can also set supported API quotas under **APIs & Services → Quotas**.

### 3. Enable the search API

Go to **APIs & Services → Library**. Search for **Places API (New)**, open that exact API, and click **Enable**.

This version needs **Places API (New)** only. The older Places API is not the API used by this app. You do not need to enable Maps SDK for Android, Geocoding API, Routes API, or Directions API for this version: the phone-location map uses OpenStreetMap, and real driving directions open in the separate Google Maps app.

### 4. Create your key

Go to **APIs & Services → Credentials → Create credentials → API key**. Open the new key's settings. Give it a name such as **TTFA Android search**.

### 5. Restrict the key to this APK

Under **Application restrictions**, choose **Android apps**. Click **Add** and enter these two values exactly:

| Google asks for | Enter |
| --- | --- |
| Package name | `com.ttfa` |
| SHA-1 certificate fingerprint | `14:FD:5D:C5:5A:00:A7:74:01:8B:5C:B3:09:EA:80:37:E0:9C:88:7C` |

Under **API restrictions**, select **Restrict key**, choose **Places API (New)**, and save.

That fingerprint belongs to the debug APK supplied in this repository. A future Play Store or release APK will have a different signing certificate and will need its own authorized fingerprint. An APK rebuilt on another computer can also have a different debug fingerprint; the value above is for the APK supplied here. Do not choose website or server restrictions for this Android key. Changes can take a few minutes to become active.

### 6. Paste the key into the app

Download and install [the latest APK](https://github.com/mkroll84-web/turn-the-fuck-around/blob/main/builds/TURN-THE-FUCK-AROUND.apk). On GitHub, use the file's **Download raw file** button. Install it over the previous version; both versions use the same signing certificate.

Open the app and tap **Settings**. Scroll down to **Google Places**. Paste your API key into **Google Places API key**, then tap **Save key**. The field is masked, and the saved key is encrypted on your phone using Android's key storage. Never send the key in chat or put it on GitHub.

### 7. Search for a real destination

In Settings, turn **Demo drive** off, then tap **Done**. Tap **Where to? Address, business, or place** and type normally. Suggestions appear while you type. Tap a suggestion to load its full details. The app displays the selected name and full address and enables **Start Navigation**.

Start Navigation opens the selected destination in **Google Maps** for real driving directions. If Google Maps is not installed, the app tries your browser. In-app WTF route comparison remains part of the fictional demo; live in-app routing is a future milestone.

Phone location permission is optional for search. Tap **Enable phone GPS** and allow location if you want results biased toward your current position. Search still works without location permission and can find places outside your area.

## If no results appear

- A missing-key message means the app has no saved key yet. Use Settings → Google Places.
- A rejected-key message means you should check billing, **Places API (New)**, and both Android restriction values above. Allow a few minutes after changing them.
- A connection message means the phone needs working internet. Offline demo mode does not need internet.
- A usage-limit message means the project has reached a Google quota or billing limit.
- If you edit the search after selecting a destination, select a suggestion again before starting navigation. This prevents navigation to an old selection.

## Optional developer setup — you do not need this for the supplied APK

Copy `local.properties.example` to `local.properties`, keep your own Android SDK path, and replace `PASTE_YOUR_GOOGLE_PLACES_API_KEY_HERE` with your restricted key:

```properties
sdk.dir=/your/android/sdk
GOOGLE_PLACES_API_KEY=PASTE_YOUR_GOOGLE_PLACES_API_KEY_HERE
```

`local.properties` is ignored by Git. A `GOOGLE_PLACES_API_KEY` environment variable is also supported and takes precedence when building. Neither setup is required for the key-free APK: you can save the key on your phone instead. App-saved keys take precedence over build keys. Removing an app-saved key reverts to a build key if one exists.

API keys embedded in Android APKs can be extracted, so Android and API restrictions are essential. The APK distributed here contains no real API key. Google handles search queries and, when available, the location bias. Search selections are held in app memory rather than saved as a search history.
