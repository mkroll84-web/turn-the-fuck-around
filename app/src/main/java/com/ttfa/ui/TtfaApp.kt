package com.ttfa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttfa.domain.Personality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

private val Ink = Color(0xFF101C20)
private val Teal = Color(0xFF006B63)
private val Orange = Color(0xFFFF6A45)
private fun miles(meters: Double) = String.format(Locale.US, "%.1f mi", meters / 1609.344)
private fun minutes(seconds: Int) = "${ceil(seconds / 60.0).toInt()} min"

@Composable
fun TtfaApp(state: UiState, model: NavigationViewModel, requestGps: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Orange, background = Color(0xFFF6F3ED), surface = Color(0xFFF6F3ED))) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().background(Ink).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TURN THE FUCK AROUND", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Navigation with common fucking sense.", color = Color(0xFFAFCCC5), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { model.settings(!state.settings) }) { Text(if (state.settings) "Done" else "Settings", color = Color.White) }
                }
                if (state.settings) Settings(state, model, requestGps) else {
                    Row(Modifier.fillMaxWidth().background(if (state.simulation) Color(0xFFFFE7B7) else Color(0xFFDEEAE7)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(if (state.simulation) "DEMO DRIVE · fictional directions · never drive this route" else "GPS MAP · live routing not connected", style = MaterialTheme.typography.labelMedium)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (state.simulation) DemoMap(state, Modifier.fillMaxSize()) else MapPanel(state, Modifier.fillMaxSize())
                        Surface(Modifier.align(Alignment.TopStart).padding(12.dp), shape = MaterialTheme.shapes.medium, shadowElevation = 3.dp) {
                            Text(if (state.simulation) "San Francisco demo" else state.gpsStatus, Modifier.padding(10.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(Modifier.fillMaxWidth().heightIn(max = 410.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.navigating || state.arrived) NavigationCard(state, model) else HomeCard(state, model, requestGps)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCard(state: UiState, model: NavigationViewModel, requestGps: () -> Unit) {
    if (state.simulation) {
        OutlinedTextField(state.query, model::search, label = { Text("Where to? Search demo places") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (state.destinations.isEmpty()) Text("No demo places found. Try coffee, park, or diner.")
        state.destinations.forEach { destination ->
            OutlinedButton(onClick = { model.select(destination) }, modifier = Modifier.fillMaxWidth()) {
                Text((if (state.selected?.id == destination.id) "✓  " else "") + destination.name)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("WTF MODE", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(state.wtf, model::wtf, modifier = Modifier.semantics { contentDescription = "WTF MODE" })
        }
        Button(model::start, enabled = state.selected != null && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (state.busy) "Finding route…" else "Start Navigation · demo") }
    } else {
        Text("Your location", style = MaterialTheme.typography.titleLarge)
        Text(state.gpsStatus)
        state.gps?.let { Text(String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude)) }
        Text("Real driving directions require a connected routing provider. You can explore your GPS position now or try the demo.")
        Button(requestGps, modifier = Modifier.fillMaxWidth()) { Text("Enable phone GPS") }
        OutlinedButton({ model.simulation(true) }, modifier = Modifier.fillMaxWidth()) { Text("Try demo navigation") }
    }
    state.message?.let { Text(it, color = Teal) }
    Text(if (state.simulation) "Fictional offline map · demo destinations only" else "© OpenStreetMap contributors · map tiles need internet", style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun NavigationCard(state: UiState, model: NavigationViewModel) {
    Text(state.nextManeuver, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(miles(state.remainingMeters), fontWeight = FontWeight.Bold)
        Text(minutes(state.remainingSeconds))
        Text("ETA " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + state.remainingSeconds * 1000L)))
    }
    state.message?.let { Text(it, color = Teal) }
    state.decision?.let { decision ->
        Surface(color = Color(0xFFDEEFE7), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(if (decision.won) "WTF MODE WINS · SIMULATION" else "NORMAL REROUTE · SIMULATION", fontWeight = FontWeight.Black)
                state.normalRoute?.let { Text("Normal: ${miles(it.distanceMeters)} · ${minutes(it.durationSeconds)}") }
                Text("Chosen: ${miles(decision.route.distanceMeters)} · ${minutes(decision.route.durationSeconds)}")
                if (decision.won) Text("Saves ${miles(decision.metersSaved)} and ${minutes(decision.secondsSaved)}")
                Text("Fictional route comparison. Not verified for real roads.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (state.navigating) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(model::pause, modifier = Modifier.weight(1f)) { Text(if (state.paused) "Resume" else "Pause") }
            Button(model::missTurn, enabled = !state.busy, modifier = Modifier.weight(1f)) { Text(if (state.busy) "Rerouting…" else "Miss a turn") }
        }
    }
    OutlinedButton(model::stop, modifier = Modifier.fillMaxWidth()) { Text(if (state.arrived) "Back to home" else "End navigation") }
}

@Composable
private fun Settings(state: UiState, model: NavigationViewModel, requestGps: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your co-pilot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Demo drive", Modifier.weight(1f)); Switch(state.simulation, model::simulation, modifier = Modifier.semantics { contentDescription = "Demo drive" }) }
        Text("Demo routes are fictional. GPS map mode shows your phone’s location and does not provide driving directions.")
        Row(verticalAlignment = Alignment.CenterVertically) { Text("WTF MODE", Modifier.weight(1f), fontWeight = FontWeight.Bold); Switch(state.wtf, model::wtf, modifier = Modifier.semantics { contentDescription = "WTF MODE" }) }
        Text("Only provider-verified legal alternatives may be used in live navigation. Comedy never changes a maneuver.")
        Text("Personality", style = MaterialTheme.typography.titleMedium)
        Personality.entries.forEach { personality ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(state.personality == personality, { model.personality(personality) }, modifier = Modifier.semantics { contentDescription = personality.label })
                Text(personality.label)
            }
        }
        Text("Minimum distance saved: ${state.thresholds.minimumMetersSaved.toInt()} m")
        Slider(state.thresholds.minimumMetersSaved.toFloat(), { model.thresholds(it.toDouble(), state.thresholds.minimumSecondsSaved) }, valueRange = 100f..1600f, steps = 14)
        Text("Minimum time saved: ${state.thresholds.minimumSecondsSaved} seconds")
        Slider(state.thresholds.minimumSecondsSaved.toFloat(), { model.thresholds(state.thresholds.minimumMetersSaved, it.toInt()) }, valueRange = 30f..300f, steps = 8)
        Text("Both savings thresholds must be met. Otherwise use the normal reroute.")
        HorizontalDivider()
        Text(state.gpsStatus)
        Button(requestGps) { Text("Enable phone GPS") }
        Text("Safety first", style = MaterialTheme.typography.titleMedium)
        Text("Set your destination while parked. This MVP has no voice guidance, background navigation, traffic, or live road routing. Do not use it for real driving yet.")
        Text("Map data © OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall)
    }
}
