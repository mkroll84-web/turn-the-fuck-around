package com.ttfa.domain

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DestinationSearchTest {
    private val suggestion = PlaceSuggestion("unit-place-id", "Test Coffee", "Test Coffee, 123 Example St, Example City", "123 Example St, Example City")
    private val destination = Destination("unit-place-id", "Test Coffee", Coordinate(40.7, -74.0), "123 Example St, Example City", DestinationSource.GOOGLE_PLACES)
    private inner class Provider : PlaceSearchProvider {
        val calls = mutableListOf<Pair<String, Coordinate?>>()
        var resolves = 0
        var resets = 0
        var failure: Exception? = null
        var resolved = destination
        var slowAutocomplete = false
        var slowResolve = false
        override suspend fun autocomplete(query: String, locationBias: Coordinate?): List<PlaceSuggestion> {
            calls += query to locationBias
            failure?.let { throw it }
            if (slowAutocomplete && query == "old") withContext(NonCancellable) { delay(1000) }
            return listOf(suggestion.copy(name = query))
        }
        override suspend fun resolve(suggestion: PlaceSuggestion): Destination {
            resolves++
            if (slowResolve) withContext(NonCancellable) { delay(1000) }
            failure?.let { throw it }
            return resolved
        }
        override fun resetSession() { resets++ }
    }
    @Test fun addressesBusinessesLandmarksAndCitiesAllReachProvider() = runTest {
        val provider = Provider(); val search = DestinationSearchController(this, provider)
        val queries = listOf("1600 Amphitheatre Parkway, Mountain View", "1600 Amp", "coffee", "Statue of Liberty", "New York")
        queries.forEach { search.search(it, null); advanceUntilIdle(); assertEquals(it, search.state.value.query) }
        assertEquals(queries, provider.calls.map { it.first })
    }
    @Test fun rapidTypingIsDebouncedAndTextNeverBlocked() = runTest {
        val provider = Provider(); val search = DestinationSearchController(this, provider)
        search.search("1", null); advanceTimeBy(100); search.search("12", null); advanceTimeBy(100); search.search("123", null)
        assertEquals("123", search.state.value.query)
        assertTrue(provider.calls.isEmpty())
        advanceUntilIdle()
        assertEquals(listOf("123"), provider.calls.map { it.first })
        assertFalse(search.state.value.loading)
    }
    @Test fun selectionSavesIdNameAddressAndCoordinates() = runTest {
        val provider = Provider(); val search = DestinationSearchController(this, provider)
        search.search("coffee", null); advanceUntilIdle()
        search.select(search.state.value.suggestions.single()); advanceUntilIdle()
        val selected = search.state.value.selected!!
        assertEquals("unit-place-id", selected.placeId)
        assertEquals("Test Coffee", selected.name)
        assertEquals("123 Example St, Example City", selected.formattedAddress)
        assertEquals(Coordinate(40.7, -74.0), selected.location)
        assertTrue(search.state.value.suggestions.isEmpty()); assertFalse(search.state.value.loading)
    }
    @Test fun editingSelectionDisablesNavigationUntilAnotherResolvedPlace() = runTest {
        val provider = Provider(); val search = DestinationSearchController(this, provider)
        search.search("coffee", null); advanceUntilIdle(); search.select(search.state.value.suggestions.single()); advanceUntilIdle()
        assertNotNull(search.state.value.selected)
        search.search("another address", null)
        assertNull(search.state.value.selected); advanceUntilIdle()
        assertNull(search.state.value.selected)
    }
    @Test fun staleNonCooperativeSearchCannotReplaceNewerQuery() = runTest {
        val provider = Provider().apply { slowAutocomplete = true }; val search = DestinationSearchController(this, provider)
        search.search("old", null); advanceTimeBy(301); runCurrent()
        search.search("new", null); advanceUntilIdle()
        assertEquals("new", search.state.value.query)
        assertEquals("new", search.state.value.suggestions.single().name)
    }
    @Test fun clearingOrSwitchingModeCancelsPendingLiveResultsAndSession() = runTest {
        val provider = Provider().apply { slowAutocomplete = true }; val search = DestinationSearchController(this, provider)
        search.search("old", null); advanceTimeBy(301); runCurrent(); search.clear(); advanceUntilIdle()
        assertEquals(DestinationSearchState(), search.state.value); assertTrue(provider.resets > 0)
    }
    @Test fun staleDetailsCannotSelectDestinationAfterNewTyping() = runTest {
        val provider = Provider().apply { slowResolve = true }; val search = DestinationSearchController(this, provider)
        search.search("coffee", null); advanceUntilIdle(); search.select(search.state.value.suggestions.single()); runCurrent()
        search.search("different", null); advanceUntilIdle()
        assertEquals("different", search.state.value.query); assertNull(search.state.value.selected)
    }
    @Test fun missingKeyAndNetworkErrorsRetainInputWithoutFakeSuggestions() = runTest {
        val provider = Provider().apply { failure = PlaceSearchException("Add a Google key in Settings") }
        val search = DestinationSearchController(this, provider)
        search.search("real address", null); advanceUntilIdle()
        assertEquals("real address", search.state.value.query)
        assertEquals("Add a Google key in Settings", search.state.value.error)
        assertTrue(search.state.value.suggestions.isEmpty()); assertNull(search.state.value.selected); assertFalse(search.state.value.loading)
    }
    @Test fun invalidDetailsNeverEnableNavigation() = runTest {
        val provider = Provider().apply { resolved = destination.copy(location = Coordinate(Double.NaN, -74.0)) }
        val search = DestinationSearchController(this, provider)
        search.search("coffee", null); advanceUntilIdle(); search.select(search.state.value.suggestions.single()); advanceUntilIdle()
        assertNull(search.state.value.selected); assertNotNull(search.state.value.error)
    }
    @Test fun unlistedSuggestionCannotBeSelected() = runTest {
        val provider = Provider(); val search = DestinationSearchController(this, provider)
        search.select(suggestion); advanceUntilIdle()
        assertEquals(0, provider.resolves); assertNull(search.state.value.selected)
    }
    @Test fun locationBiasIsOptionalAndRequiresPermissionAndFreshRealFix() = runTest {
        val gps = Coordinate(40.7, -74.0)
        assertNull(searchLocationBias(gps, false, 1000, 2000))
        assertNull(searchLocationBias(gps, true, 1000, 302000))
        assertNull(searchLocationBias(gps, true, 1000, 500))
        assertNull(searchLocationBias(null, true, 1000, 2000))
        assertEquals(gps, searchLocationBias(gps, true, 1000, 2000))
        val provider = Provider(); val search = DestinationSearchController(this, provider)
        search.search("coffee", gps); advanceUntilIdle(); assertEquals(gps, provider.calls.single().second)
    }
    @Test fun liveNavigationUsesResolvedCoordinatesAndGooglePlaceId() {
        val url = googleMapsDirectionsUrl(destination.copy(id = "place/id +suffix"))
        assertTrue(url.contains("destination=40.7,-74.0")); assertTrue(url.contains("destination_place_id=place%2Fid+%2Bsuffix"))
        assertTrue(url.contains("travelmode=driving")); assertTrue(url.contains("dir_action=navigate"))
        try { googleMapsDirectionsUrl(destination.copy(source = DestinationSource.DEMO)); fail("No fake navigation handoff") } catch (_: IllegalArgumentException) { }
    }
    @Test fun emptyOrPlaceholderKeysAreNotUsable() {
        assertFalse(usablePlacesKey("")); assertFalse(usablePlacesKey("PASTE_YOUR_GOOGLE_MAPS_PLATFORM_API_KEY_HERE"))
        assertFalse(usablePlacesKey("key with spaces"))
    }
}
