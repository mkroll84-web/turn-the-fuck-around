package com.ttfa

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.ttfa.domain.Coordinate
import com.ttfa.ui.*
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val model: NavigationViewModel by viewModels()
    private val manager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { model.gps(Coordinate(location.latitude, location.longitude), location.accuracy) }
        override fun onProviderEnabled(provider: String) { model.gpsStatus("Waiting for a phone GPS fix…") }
        @Suppress("DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
        override fun onProviderDisabled(provider: String) { model.gpsStatus("Location service disabled. Enable phone location.") }
    }
    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { allowed -> allowed }) startGps() else model.gpsStatus("Location permission declined. Demo still works.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "TTFA-Android-MVP/0.1 (com.ttfa)"
        Configuration.getInstance().osmdroidBasePath = java.io.File(cacheDir, "maps")
        Configuration.getInstance().osmdroidTileCache = java.io.File(cacheDir, "maps/tiles")
        setContent {
            val state by model.state.collectAsState()
            TtfaApp(state, model) { requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
        }
    }
    override fun onStart() { super.onStart(); startGps() }
    override fun onStop() { manager.removeUpdates(listener); super.onStop() }
    private fun startGps() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        try {
            manager.removeUpdates(listener)
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
            if (providers.isEmpty()) model.gpsStatus("Enable location in your phone settings.")
            else {
                model.gpsStatus("Waiting for a phone GPS fix…")
                providers.forEach { manager.requestLocationUpdates(it, 1000L, 3f, listener) }
            }
        } catch (_: SecurityException) { model.gpsStatus("Location permission required.") }
    }
}
