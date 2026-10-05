package com.ttfa.domain

import java.net.URLEncoder

/** Places resolves a destination, not road legality. Google Maps owns the real driving route. */
fun googleMapsDirectionsUrl(destination: Destination): String {
    require(destination.source == DestinationSource.GOOGLE_PLACES && destination.id.isNotBlank())
    require(destination.location.latitude.isFinite() && destination.location.latitude in -90.0..90.0 &&
        destination.location.longitude.isFinite() && destination.location.longitude in -180.0..180.0)
    val id = URLEncoder.encode(destination.id, "UTF-8")
    return "https://www.google.com/maps/dir/?api=1&destination=${destination.location.latitude},${destination.location.longitude}" +
        "&destination_place_id=$id&travelmode=driving&dir_action=navigate"
}
