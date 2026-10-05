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

## Paid app + Voo Mode (version 0.3)

**The final Play app costs US $3 once. Voo costs an additional US $2 once. There is no subscription.** The app does not collect the base-app payment: Google Play does that before installation. Local currency prices/taxes can differ.

Normal, Sassy, Unhinged, WTF Mode and normal settings belong to the base app. Destination side-of-road guidance, the Fuck That button and in-app real navigation belong to the planned base feature set, not extra paid products. They are not implemented by this monetization update. The current MVP still opens Google Maps for real directions. Voo currently unlocks demo personality text at all three levels (including Absolutely Foul); a custom deep voice/live Voo guidance is not implemented yet. Do not publish or sell this prototype as the finished navigation app.

### 1. Set the base app price

1. Sign in to [Google Play Console](https://play.google.com/console/). Register a developer account if you do not have one.
2. Create the Android app entry. Keep the package name **com.ttfa**. Choose **Paid** when asked whether the app is free or paid. A publicly released free app cannot later be converted to paid under the same package.
3. Complete the payments/merchant setup requested by Google.
4. Open **Monetize with Play → Products → App pricing** (Console labels may change). Set the United States price to **US $3.00**. Review converted regional prices and save. Do not create an in-app product for the base app.

### 2. Create the Voo product

1. Open **Monetize with Play → Products → One-time products**.
2. Create the exact product ID **voo_mode_unlock**. IDs are permanent: copy it exactly.
3. Name it **Voo Mode**. Describe it as a permanent personality unlock with all Voo roast levels; no subscription.
4. Create an active **Buy** purchase option (not Rent). Set its United States price to **US $2.00**, review regional prices, activate the purchase option/product and make it available in the testing countries. A plain full-price buy option is all this app needs; do not add rental/preorder offers.

### 3. Configure billing verification

The app already includes Google Play Billing Library **9.1.0**, the billing permission via its manifest, pending-payment support, purchase acknowledgement, and restoration. No secret API key is needed for the BillingClient itself.

Google supplies a public license key used to check that a purchase was signed by Play:

1. In this app's Play Console, open **Monetization setup → Licensing** (some Console layouts call this Services & APIs). Copy the **Base64-encoded RSA public key**.
2. In the project folder, open **local.properties**. Add this line on its own line, replacing the example text:

   ```properties
   GOOGLE_PLAY_LICENSE_KEY=PASTE_YOUR_GOOGLE_PLAY_RSA_PUBLIC_KEY_HERE
   ```

   This file is ignored by Git. Alternatively, a build environment can securely supply the variable `GOOGLE_PLAY_LICENSE_KEY`. This is a public verification key, not your Google account password or a service-account private key. Never paste a service-account credential here.
3. Have the app rebuilt for Internal Testing after supplying the key. A build without it disables real checkout and fails closed on unverified purchases. Demo and the development override still work.

Purchases must be `PURCHASED`, for `voo_mode_unlock`, for `com.ttfa`, with a valid Play signature. Pending or invalid purchases never unlock Voo. Purchases are acknowledged, never consumed. Failed acknowledgement can be retried with **Restore Purchases**; the app retries on the next foreground connection too. Google refunds unacknowledged purchases after its acknowledgement deadline (normally three days after payment completes; license-test deadlines are much shorter).

**Before public production release:** use a secure backend with Google Play Developer API purchase verification and Real-time Developer Notifications for immediate refund/revocation handling. The MVP verifies Google's RSA signatures locally; it does not have that backend yet. Service-account credentials belong only on that server, never inside the Android app. Local signatures prevent simply editing a cached paid flag, but cannot provide immediate offline refund/revocation notifications or make a modified client tamper-proof. You do not need to write that server yourself; it is a development milestone before public release.

### 4. Test without charging yourself

**For the current sideloaded development APK:** open **Settings → Development Voo override** and turn it on. The selector updates immediately. Turn it off to test the locked screen again. This is labeled development access, creates no Play purchase and charges nothing. It is OFF by default. Release builds compile a separate implementation that cannot read or enable the override, even if development preferences remain on the phone.

**For real Google Play testing:**

1. Build a signed release Android App Bundle, enroll in Play App Signing, and upload it to **Testing → Internal testing**. The cloud debug APK is not the Play release artifact.
2. Activate `voo_mode_unlock` and its Buy option. Complete any Console prerequisites before publishing the internal track.
3. Add your Google account to the internal tester email list and to **Settings → License testing** in the developer account. License testers can use Google's test payment methods without real charges. Internal testers who are not license testers can be charged for in-app purchases.
4. Open the internal-test opt-in link on your phone using that same Google account, accept the test and install/update from **Google Play**, not by sideloading the debug APK. If the sideloaded debug app has a different signing key, uninstall it first; this clears its saved settings and Google search key. Then install from the internal track. Product availability can take time to propagate.
5. Leave the development override off. Tap locked Voo, press Unlock and choose Google's **test card, always approves**. Verify all roast levels unlock. Also test decline, cancel and delayed/pending payment: they must stay locked until payment completes.
6. Reinstall from the internal track and tap **Restore Purchases**. Test an update and another phone with the same purchasing Play account. Test refunds/revocations while online. Reuse the same signing key/package for updates.

### 5. How restoration works

The permanent non-consumable purchase belongs to the purchasing **Google Play account**, not to an app toggle or a subscription. The app queries owned purchases whenever Billing connects and when the app returns to the foreground. **Settings → Restore Purchases** performs the same check. Reinstallation/change of phone restores from Play when online and signed in with the purchasing account. Previously acknowledged, signed purchase data supports offline access on the existing installation; reinstalling needs Play connectivity. A successful ownership check showing no valid purchase removes cached access. Network errors do not erase previously verified ownership. Switching accounts or refunding a purchase may require a fresh ownership check; offline revocations cannot be immediate.

### New branding

Settings has **System / Light / Dark** appearance options. Aqua is the primary brand color, hot pink accents Voo and Absolutely Foul, and WTF comparisons use an aqua branded card. Paid checkout displays the actual localized price supplied by Google Play. The US $2 fallback is a preview when the store product is unavailable, and checkout is disabled then.
