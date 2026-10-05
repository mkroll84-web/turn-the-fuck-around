package com.ttfa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ttfa.data.DemoNavigationProvider
import com.ttfa.data.places.GooglePlacesKeyStore
import com.ttfa.data.places.GooglePlacesSearchProvider
import com.ttfa.domain.*
import com.ttfa.data.billing.VooBillingRepository
import com.ttfa.data.billing.VooBillingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.ceil

data class UiState(
    val query: String = "", val destinations: List<Destination> = emptyList(), val selected: Destination? = null,
    val location: Coordinate = Coordinate(37.7749, -122.4194), val gps: Coordinate? = null,
    val gpsStatus: String = "Phone GPS is off", val simulation: Boolean = true, val navigating: Boolean = false,
    val route: Route? = null, val nextManeuver: String = "Choose a demo destination", val remainingMeters: Double = 0.0,
    val remainingSeconds: Int = 0, val wtf: Boolean = true, val personality: Personality = Personality.DRY,
    val thresholds: WtfThresholds = WtfThresholds(), val settings: Boolean = false, val paused: Boolean = false,
    val message: String? = null, val decision: RouteDecision? = null, val normalRoute: Route? = null,
    val busy: Boolean = false, val arrived: Boolean = false,
    val liveSearch: DestinationSearchState = DestinationSearchState(),
    val googleKeyConfigured: Boolean = false, val googleKeyMessage: String? = null,
    val locationPermissionGranted: Boolean = false,
    val billing: VooBillingState = VooBillingState(), val purchaseScreen: Boolean = false,
    val intensity: RoastIntensity = RoastIntensity.LIGHT, val theme: ThemeMode = ThemeMode.SYSTEM,

)
class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    val billing = VooBillingRepository(application)
    private val provider: NavigationProvider = DemoNavigationProvider()
    private val googleKeyStore = GooglePlacesKeyStore(application)
    private val liveSearch = DestinationSearchController(viewModelScope, GooglePlacesSearchProvider(application, googleKeyStore))
    private var lastGpsFixMillis = 0L
    private val preferences = application.getSharedPreferences("settings", 0)
    private val detector = DeviationDetector()
    private var simulationJob: Job? = null
    private var searchJob: Job? = null
    private var routingJob: Job? = null
    private var points: List<Coordinate> = emptyList()
    private var position = 0
    private val _state = MutableStateFlow(UiState(
        googleKeyConfigured = googleKeyStore.isConfigured,
        theme = ThemeMode.entries.firstOrNull { it.name == preferences.getString("theme", "SYSTEM") } ?: ThemeMode.SYSTEM,
        intensity = RoastIntensity.entries.firstOrNull { it.name == preferences.getString("intensity", "LIGHT") } ?: RoastIntensity.LIGHT,
        wtf = preferences.getBoolean("wtf", true),
        personality = Personality.entries.firstOrNull { it.name == preferences.getString("personality", "DRY") } ?: Personality.DRY,
        thresholds = WtfThresholds(preferences.getFloat("meters", 400f).toDouble(), preferences.getInt("seconds", 60)),
    ))
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch { billing.state.collect { entitlement ->
            _state.update { it.copy(billing = entitlement, personality = if (it.purchaseScreen && entitlement.unlocked) Personality.VOO else allowedPersonality(it.personality, entitlement.unlocked), intensity = if (!entitlement.unlocked && it.intensity == RoastIntensity.FOUL) RoastIntensity.SPICY else it.intensity, purchaseScreen = if (entitlement.unlocked) false else it.purchaseScreen) }
        } }
        viewModelScope.launch {
            liveSearch.state.collect { live ->
                _state.update { it.copy(liveSearch = live, selected = if (it.simulation) it.selected else live.selected) }
            }
        }
        search("")
    }
    fun search(query: String) {
        if (!_state.value.simulation) {
            val s = _state.value
            val bias = searchLocationBias(s.gps, s.locationPermissionGranted, lastGpsFixMillis, android.os.SystemClock.elapsedRealtime())
            liveSearch.search(query, bias)
            // Forward edits immediately; the UI and provider must agree on the
            // latest query without waiting for the second StateFlow collector.
            _state.update { it.copy(liveSearch = liveSearch.state.value, selected = null) }
            return
        }
        _state.update { it.copy(query = query, selected = null) }; searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150)
            val result = provider.search(query)
            if (_state.value.simulation && _state.value.query == query) _state.update { it.copy(destinations = result) }
        }
    }
    fun select(destination: Destination) {
        if (_state.value.simulation && destination in _state.value.destinations && destination.source == DestinationSource.DEMO)
            _state.update { it.copy(selected = destination, message = null) }
    }
    fun selectPlace(suggestion: PlaceSuggestion) { if (!_state.value.simulation) liveSearch.select(suggestion) }
    fun clearLiveSearch() { liveSearch.clear() }
    fun saveGoogleKey(value: String): Boolean {
        val key = value.trim()
        if (!usablePlacesKey(key)) {
            _state.update { it.copy(googleKeyMessage = "Paste the real Google key, not the example placeholder.") }; return false
        }
        return try {
            liveSearch.clear()
            googleKeyStore.save(key)
            _state.update { it.copy(googleKeyConfigured = true, googleKeyMessage = "Google key saved securely. Turn off Demo drive and search for a destination.") }
            true
        } catch (_: Exception) {
            _state.update { it.copy(googleKeyMessage = "Couldn’t save the key on this phone. Please try again.") }; false
        }
    }
    fun removeGoogleKey() {
        liveSearch.clear(); googleKeyStore.clear()
        val configured = googleKeyStore.isConfigured
        _state.update { it.copy(googleKeyConfigured = configured,
            googleKeyMessage = if (configured) "App-saved key removed; a build key is still configured." else "Google key removed. Demo drive still works offline.") }
    }
    fun settings(show: Boolean) { _state.update { it.copy(settings = show) } }
    fun wtf(enabled: Boolean) { preferences.edit().putBoolean("wtf", enabled).apply(); _state.update { it.copy(wtf = enabled) } }
    fun personality(value: Personality) {
        if (value == Personality.VOO && !_state.value.billing.unlocked) { purchaseScreen(true); return }
        preferences.edit().putString("personality", value.name).apply(); _state.update { it.copy(personality = value) } }
    fun purchaseScreen(show: Boolean) { _state.update { it.copy(purchaseScreen = show) } }
    fun intensity(value: RoastIntensity) {
        if (value == RoastIntensity.FOUL && !_state.value.billing.unlocked) { purchaseScreen(true); return }
        preferences.edit().putString("intensity", value.name).apply(); _state.update { it.copy(intensity = value) }
    }
    fun theme(value: ThemeMode) { preferences.edit().putString("theme", value.name).apply(); _state.update { it.copy(theme = value) } }
    override fun onCleared() { billing.close(); super.onCleared() }
    fun thresholds(meters: Double, seconds: Int) {
        preferences.edit().putFloat("meters", meters.toFloat()).putInt("seconds", seconds).apply()
        _state.update { it.copy(thresholds = WtfThresholds(meters, seconds)) }
    }
    fun simulation(enabled: Boolean) {
        stop(); searchJob?.cancel()
        _state.update { it.copy(simulation = enabled, location = if (enabled) Coordinate(37.7749, -122.4194) else it.gps ?: it.location,
            query = "", destinations = emptyList(), message = null, selected = null, liveSearch = DestinationSearchState()) }
        liveSearch.clear()
        if (enabled) search("")
    }
    fun locationPermission(granted: Boolean) {
        _state.update { it.copy(locationPermissionGranted = granted, gps = if (granted) it.gps else null) }
    }
    fun gps(point: Coordinate, accuracy: Float) {
        if (!point.latitude.isFinite() || point.latitude !in -90.0..90.0 || !point.longitude.isFinite() || point.longitude !in -180.0..180.0) return
        lastGpsFixMillis = android.os.SystemClock.elapsedRealtime()
        _state.update { it.copy(gps = point, locationPermissionGranted = true,
            gpsStatus = "Phone GPS · accuracy ${accuracy.toInt()} m", location = if (it.simulation) it.location else point) }
    }
    fun gpsStatus(message: String) { _state.update { it.copy(gpsStatus = message) } }
    fun navigationLaunchFailed() { _state.update { it.copy(message = "Couldn’t open Google Maps. Install Google Maps or a web browser, then try again.") } }
    fun start() {
        val s = _state.value; val destination = s.selected ?: return
        if (!s.simulation && provider.simulationOnly) { _state.update { it.copy(message = "Live routing isn’t connected. Turn on Demo drive to try navigation.") }; return }
        routingJob = viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val route = provider.route(RoutingRequest(s.location, null, destination, s.simulation))
                require(route.providerId == provider.id && route.destinationId == destination.id && route.usableFor(s.simulation)) { "Provider returned an unverified route." }
                activate(route)
                _state.update { it.copy(navigating = true, arrived = false, message = null, decision = null, normalRoute = null) }
                runSimulation()
            } catch (e: CancellationException) { throw e } catch (e: Exception) { _state.update { it.copy(message = e.message ?: "Route unavailable") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    private fun activate(route: Route) {
        detector.reset(); position = 0
        points = route.points.zipWithNext().flatMap { (a, b) ->
            val count = ceil(distance(a, b) / 25).toInt().coerceAtLeast(1)
            (0 until count).map { i -> val t = i.toDouble() / count; Coordinate(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t) }
        } + route.points.last()
        _state.update { it.copy(route = route, remainingMeters = route.distanceMeters, remainingSeconds = route.durationSeconds, nextManeuver = route.maneuvers.first().instruction) }
    }
    private fun runSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive && _state.value.navigating) {
                delay(1000)
                if (_state.value.paused || _state.value.busy) continue
                if (position >= points.size) {
                    _state.update { it.copy(navigating = false, arrived = true, remainingMeters = 0.0, remainingSeconds = 0, nextManeuver = "You’ve arrived", message = "Demo complete. Park safely before using your phone.") }; break
                }
                val point = points[position++]
                val remainingFraction = (1.0 - position.toDouble() / points.size).coerceAtLeast(0.0)
                _state.update { s ->
                    val route = s.route!!
                    val next = route.maneuvers.firstOrNull { maneuver ->
                        val index = points.indices.minByOrNull { distance(points[it], maneuver.at) } ?: points.lastIndex
                        index >= position
                    } ?: route.maneuvers.last()
                    s.copy(location = point, remainingMeters = route.distanceMeters * remainingFraction, remainingSeconds = (route.durationSeconds * remainingFraction).toInt(), nextManeuver = next.instruction)
                }
            }
        }
    }
    fun pause() { _state.update { it.copy(paused = !it.paused) } }
    /** Inject three accurate off-route fixes through the same detector used by provider adapters. */
    fun missTurn() {
        val s = _state.value; val route = s.route ?: return
        if (!s.simulation || !s.navigating || s.busy) return
        val offRoute = Coordinate(s.location.latitude - .002, s.location.longitude - .002)
        _state.update { it.copy(location = offRoute, busy = true) }
        routingJob = viewModelScope.launch {
            try {
                var deviated = false
                repeat(3) { deviated = detector.update(offRoute, route, 5f); delay(150) }
                if (!deviated) { _state.update { it.copy(message = "Still within route tolerance. Try again farther along.") }; return@launch }
                val options = provider.reroute(RoutingRequest(offRoute, 0.0, s.selected!!, true))
                require(options.normal.providerId == provider.id && options.normal.destinationId == s.selected.id) { "Reroute did not match the request." }
                val choice = WtfPolicy.choose(options, s.wtf, true, s.thresholds)
                activate(choice.route)
                _state.update { it.copy(decision = choice, normalRoute = options.normal, message = coPilotMessage(s.personality, s.intensity, _state.value.billing.unlocked)) }
            } catch (e: CancellationException) { throw e } catch (e: Exception) { _state.update { it.copy(message = "Reroute unavailable. Stop the demo and try again.") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun stop() {
        routingJob?.cancel(); simulationJob?.cancel(); detector.reset()
        _state.update { it.copy(navigating = false, route = null, decision = null, normalRoute = null, paused = false, arrived = false, busy = false) }
    }
}
