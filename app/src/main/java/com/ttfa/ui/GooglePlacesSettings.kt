package com.ttfa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun GooglePlacesSettings(state: UiState, model: NavigationViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var key by remember { mutableStateOf("") } // Never saved to activity bundles or public app state.
    Text("Google Places", style = MaterialTheme.typography.titleLarge)
    Text(if (state.googleKeyConfigured) "A Google key is configured." else "Add a Google Maps Platform key to enable real address search. Demo drive needs no key.")
    OutlinedTextField(key, { key = it }, label = { Text("Google Places API key") },
        placeholder = { Text("PASTE_YOUR_GOOGLE_PLACES_API_KEY_HERE") },
        visualTransformation = PasswordVisualTransformation(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ if (model.saveGoogleKey(key)) { key = ""; keyboard?.hide(); focus.clearFocus() } }, enabled = key.isNotBlank()) { Text("Save key") }
        if (state.googleKeyConfigured) OutlinedButton({ model.removeGoogleKey(); key = "" }) { Text("Remove saved key") }
    }
    state.googleKeyMessage?.let { Text(it) }
    Text("Setup steps are in README_FOR_MELISSA.md on GitHub. Restrict the key to this Android app before pasting it. Don’t send it in chat.", style = MaterialTheme.typography.bodySmall)
    HorizontalDivider()
}
