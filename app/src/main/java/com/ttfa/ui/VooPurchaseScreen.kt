package com.ttfa.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private tailrec fun Context.activity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }
@Composable
fun VooPurchaseScreen(state: UiState, model: NavigationViewModel) {
    val activity = LocalContext.current.activity()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("UNLOCK VOO MODE 😏", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
        Text("Deep voice.\nBad attitude.\nAbsolutely unnecessary disrespect.", style = MaterialTheme.typography.headlineSmall)
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A taste of the attitude", fontWeight = FontWeight.Bold)
                Text("“Darling, that turn wasn’t playing hard to get.”")
                Text("“Deep breath, hot mess. You fucked that turn up beautifully.”")
                Text("Sample text only. Voo behavior and all roast levels, including Absolutely Foul, unlock after purchase.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("One-time purchase: ${state.billing.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("The Google Play base app is US $3 once. Voo is an additional US $2 once (local store prices may vary). No subscription.")
        Text("This MVP uses Voo text in Demo Drive. Real directions open in Google Maps; a custom deep navigation voice is planned.", style = MaterialTheme.typography.bodySmall)
        Text(state.billing.message)
        Button(onClick = { activity?.let(model.billing::purchase) }, enabled = activity != null && state.billing.ready && !state.billing.pending,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPink, contentColor = BrandInk), modifier = Modifier.fillMaxWidth()) {
            Text(if (state.billing.pending) "PAYMENT PENDING" else "UNLOCK VOO MODE")
        }
        OutlinedButton({ model.purchaseScreen(false) }, modifier = Modifier.fillMaxWidth()) { Text("NOT TODAY, DUMBASS") }
        TextButton(model.billing::refresh, modifier = Modifier.fillMaxWidth()) { Text("Restore purchases") }
    }
}
