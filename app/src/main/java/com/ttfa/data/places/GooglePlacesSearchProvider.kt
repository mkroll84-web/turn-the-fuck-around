package com.ttfa.data.places

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.ttfa.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Uses Places API (New) only. No SDK initialization or network calls in offline demo mode. */
class GooglePlacesSearchProvider(context: Context, private val keyStore: GooglePlacesKeyStore) : PlaceSearchProvider {
    private val context = context.applicationContext
    private var client: PlacesClient? = null
    private var activeKey: String? = null
    private var session: AutocompleteSessionToken? = null
    private fun client(): PlacesClient {
        val key = keyStore.apiKey()
        if (!usablePlacesKey(key)) throw PlaceSearchException("Google search needs an API key. Add it in Settings → Google Places.")
        if (client == null || activeKey != key) {
            if (Places.isInitialized()) Places.deinitialize()
            Places.initializeWithNewPlacesApiEnabled(context, key)
            client = Places.createClient(context)
            activeKey = key
            session = null
        }
        return client!!
    }
    override suspend fun autocomplete(query: String, locationBias: Coordinate?): List<PlaceSuggestion> {
        val client = client()
        val token = session ?: AutocompleteSessionToken.newInstance().also { session = it }
        val cancellation = CancellationTokenSource()
        val builder = FindAutocompletePredictionsRequest.builder()
            .setQuery(query).setSessionToken(token).setCancellationToken(cancellation.token)
        locationBias?.let {
            val origin = LatLng(it.latitude, it.longitude)
            // Bias, not restriction: distant full addresses and cities can still appear.
            builder.setLocationBias(CircularBounds.newInstance(origin, 30_000.0)).setOrigin(origin)
        }
        // No type/country filters: addresses, businesses, landmarks and localities are all eligible.
        try {
            return client.findAutocompletePredictions(builder.build()).await().autocompletePredictions.map {
                PlaceSuggestion(it.placeId, it.getPrimaryText(null).toString(), it.getFullText(null).toString(), it.getSecondaryText(null).toString())
            }
        } catch (e: CancellationException) { throw e }
        catch (e: ApiException) { throw apiError(e) }
        finally { cancellation.cancel() }
    }
    override suspend fun resolve(suggestion: PlaceSuggestion): Destination {
        val client = client()
        val token = session ?: AutocompleteSessionToken.newInstance()
        val cancellation = CancellationTokenSource()
        val request = FetchPlaceRequest.builder(suggestion.placeId, listOf(
            Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION,
        )).setSessionToken(token).setCancellationToken(cancellation.token).build()
        try {
            val place = client.fetchPlace(request).await().place
            val location = place.location ?: throw PlaceSearchException("Google couldn’t locate this result. Choose another suggestion.")
            val id = place.id ?: throw PlaceSearchException("Google didn’t return a place ID. Please try again.")
            return Destination(id, place.displayName?.takeIf { it.isNotBlank() } ?: suggestion.name,
                Coordinate(location.latitude, location.longitude),
                place.formattedAddress?.takeIf { it.isNotBlank() } ?: suggestion.fullText,
                DestinationSource.GOOGLE_PLACES, place.attributions.orEmpty())
        } catch (e: CancellationException) { throw e }
        catch (e: ApiException) { throw apiError(e) }
        finally { cancellation.cancel(); if (session == token) session = null }
    }
    override fun resetSession() { session = null }
    private fun apiError(error: ApiException): PlaceSearchException = when (error.statusCode) {
        CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT -> PlaceSearchException("Google search couldn’t connect. Check your internet and try again.")
        CommonStatusCodes.DEVELOPER_ERROR, 9011, 9012 -> PlaceSearchException("Google rejected the search key. Check billing, Places API (New), and the Android key restrictions in README_FOR_MELISSA.md.")
        9010 -> PlaceSearchException("Google search reached its usage limit. Check the project’s quota and billing.")
        else -> PlaceSearchException("Google search is unavailable (code ${error.statusCode}). Check your connection and Google key setup, then try again.")
    }
}
