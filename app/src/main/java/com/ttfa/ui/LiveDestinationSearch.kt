package com.ttfa.ui

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import com.ttfa.domain.Destination

@Composable
fun LiveDestinationSearch(state: UiState, model: NavigationViewModel, startNavigation: (Destination) -> Unit) {
    val live = state.liveSearch
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val input = rememberTextFieldState(initialText = live.query)
    LaunchedEffect(input) {
        snapshotFlow { input.text.toString() }.collect { query ->
            if (query != model.state.value.liveSearch.query) model.search(query)
        }
    }
    LaunchedEffect(live.selected?.placeId) {
        live.selected?.let { input.setTextAndPlaceCursorAtEnd(it.name) }
    }
    // Keep keystrokes local and synchronous; SDK/StateFlow responses must not
    // replace in-progress editing with an older prefix.
    OutlinedTextField(state = input,
        label = { Text("Where to? Address, business, or place") },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth(),
        trailingIcon = { if (input.text.isNotEmpty()) TextButton({ input.setTextAndPlaceCursorAtEnd(""); model.clearLiveSearch() }) { Text("Clear") } })
    if (live.loading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(if (live.suggestions.isEmpty()) "Searching Google Places…" else "Loading destination…")
    }
    live.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (!state.googleKeyConfigured) {
        Text("Real search needs a Google key. Your typing still works; add the key in Settings → Google Places.")
        OutlinedButton({ model.settings(true) }) { Text("Set up Google search") }
    } else if (live.query.isBlank()) {
        Text(if (state.gps != null && state.locationPermissionGranted) "Search addresses, businesses, landmarks, or cities. Results are biased near your current location."
            else "Search addresses, businesses, landmarks, or cities. Location permission is optional.")
    } else if (!live.loading && live.error == null && live.suggestions.isEmpty() && live.selected == null) {
        Text("No Google results found. Add a street, city, or more of the place name.")
    }
    live.suggestions.forEach { suggestion ->
        OutlinedButton({ keyboard?.hide(); focus.clearFocus(); model.selectPlace(suggestion) }, Modifier.fillMaxWidth(), enabled = !live.loading) {
            Column(Modifier.fillMaxWidth()) {
                Text(suggestion.name, fontWeight = FontWeight.Bold)
                if (suggestion.secondaryText.isNotBlank()) Text(suggestion.secondaryText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    live.selected?.let { selected ->
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Selected destination", style = MaterialTheme.typography.labelLarge)
                Text(selected.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(selected.formattedAddress)
            }
        }
        selected.attributions.forEach { attribution ->
            AndroidView(factory = { context -> TextView(context).apply { movementMethod = LinkMovementMethod.getInstance() } },
                update = { it.text = Html.fromHtml(attribution, Html.FROM_HTML_MODE_COMPACT) })
        }
    }
    // Use the current SDK's supplied brand asset; Google content is never plotted on the OSM map.
    if (live.suggestions.isNotEmpty() || live.selected != null) {
        Image(painterResource(com.google.android.libraries.places.R.drawable.googlemaps_logo_withdarkoutline),
            contentDescription = "Google Maps", modifier = Modifier.height(22.dp).padding(vertical = 2.dp))
    }
    BrandButton({ keyboard?.hide(); focus.clearFocus(); live.selected?.let(startNavigation) }, enabled = live.selected != null && !live.loading, modifier = Modifier.fillMaxWidth()) {
        Text("Start Navigation")
    }
    Text("Start Navigation opens Google Maps for real driving directions. In-app WTF rerouting is still a demo.", style = MaterialTheme.typography.bodySmall)
}
