package com.ttfa.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Map rendering is separate from routing. OSM tiles do not verify maneuver legality. */
@Composable
fun MapPanel(state: UiState, modifier: Modifier = Modifier, visible: Boolean = true) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val map = remember { MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true)
        isFocusable = false; isFocusableInTouchMode = false
        controller.setZoom(15.0); controller.setCenter(GeoPoint(state.location.latitude, state.location.longitude))
    } }
    DisposableEffect(map, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_RESUME -> map.onResume(); Lifecycle.Event.ON_PAUSE -> map.onPause(); else -> Unit }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); map.onPause(); map.onDetach() }
    }
    AndroidView(factory = { map }, modifier = modifier, update = { view ->
        view.visibility = if (visible) android.view.View.VISIBLE else android.view.View.INVISIBLE
        view.importantForAccessibility = if (visible) android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        if (!visible) return@AndroidView
        view.overlays.clear()
        state.route?.let { route ->
            val line = Polyline(view).apply {
                setPoints(route.points.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.rgb(12, 133, 125); outlinePaint.strokeWidth = 12f
            }; view.overlays.add(line)
        }
        state.selected?.let { d -> view.overlays.add(Marker(view).apply { position = GeoPoint(d.location.latitude, d.location.longitude); title = d.name; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }) }
        view.overlays.add(Marker(view).apply {
            position = GeoPoint(state.location.latitude, state.location.longitude)
            title = if (state.simulation) "Simulated driver" else "Phone GPS"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.OvalShape()).apply {
                paint.color = android.graphics.Color.rgb(255, 94, 64); intrinsicWidth = 32; intrinsicHeight = 32
            }
        })
        if (state.navigating || !state.simulation && state.gps != null) view.controller.setCenter(GeoPoint(state.location.latitude, state.location.longitude))
        view.invalidate()
    })
}
