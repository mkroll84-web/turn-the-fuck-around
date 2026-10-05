package com.ttfa.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaceSuggestion(val placeId: String, val name: String, val fullText: String, val secondaryText: String)
interface PlaceSearchProvider {
    suspend fun autocomplete(query: String, locationBias: Coordinate?): List<PlaceSuggestion>
    suspend fun resolve(suggestion: PlaceSuggestion): Destination
    fun resetSession()
}
class PlaceSearchException(val userMessage: String) : Exception(userMessage)
data class DestinationSearchState(
    val query: String = "",
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val selected: Destination? = null,
    val loading: Boolean = false,
    val error: String? = null,
)
/** One session per live search. Cancellation AND generation checks reject stale responses. */
class DestinationSearchController(
    private val scope: CoroutineScope,
    private val provider: PlaceSearchProvider,
    private val debounceMillis: Long = 300,
) {
    private var generation = 0L
    private var request: Job? = null
    private val _state = MutableStateFlow(DestinationSearchState())
    val state = _state.asStateFlow()
    fun search(query: String, locationBias: Coordinate?) {
        val version = ++generation
        request?.cancel()
        _state.value = DestinationSearchState(query = query, loading = query.isNotBlank())
        if (query.isBlank()) { provider.resetSession(); return }
        request = scope.launch {
            delay(debounceMillis)
            try {
                val suggestions = provider.autocomplete(query.trim(), locationBias)
                if (version == generation) _state.update { it.copy(suggestions = suggestions, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (version == generation) _state.update { it.copy(loading = false, error = errorMessage(e)) }
            }
        }
    }
    fun select(suggestion: PlaceSuggestion) {
        if (suggestion !in _state.value.suggestions) return
        val version = ++generation
        request?.cancel()
        _state.update { it.copy(selected = null, suggestions = emptyList(), loading = true, error = null) }
        request = scope.launch {
            try {
                val destination = provider.resolve(suggestion)
                require(destination.source == DestinationSource.GOOGLE_PLACES && destination.id == suggestion.placeId &&
                    destination.name.isNotBlank() && destination.formattedAddress.isNotBlank() &&
                    destination.location.latitude.isFinite() && destination.location.latitude in -90.0..90.0 &&
                    destination.location.longitude.isFinite() && destination.location.longitude in -180.0..180.0)
                if (version == generation) _state.value = DestinationSearchState(query = destination.name, selected = destination)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (version == generation) _state.update { it.copy(loading = false, error = errorMessage(e)) }
            }
        }
    }
    fun clear() {
        ++generation; request?.cancel(); provider.resetSession()
        _state.value = DestinationSearchState()
    }
    private fun errorMessage(error: Exception) = (error as? PlaceSearchException)?.userMessage
        ?: "Couldn’t search Google Places. Check your connection and try again."
}
fun usablePlacesKey(value: String): Boolean = value.isNotBlank() && value.none { it.isWhitespace() } &&
    !listOf("PASTE_", "YOUR_", "REPLACE_", "PLACEHOLDER").any { value.uppercase().contains(it) }

/** Only fresh, permission-backed phone fixes bias Google results; never the fake demo origin. */
fun searchLocationBias(gps: Coordinate?, granted: Boolean, fixElapsedMillis: Long, nowElapsedMillis: Long): Coordinate? {
    val age = nowElapsedMillis - fixElapsedMillis
    return gps?.takeIf { granted && age in 0..300_000 && it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
        it.longitude.isFinite() && it.longitude in -180.0..180.0 }
}
