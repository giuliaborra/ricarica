package com.example.ricarica
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ricarica.data.model.StationItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapView(
    stations: List<StationItem>
) {
    val torino = LatLng(45.0703, 7.6869)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(torino, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        stations.forEach { item ->
            val pos = item.station.position ?: return@forEach

            val markerState = rememberMarkerState(position = LatLng(pos.lat, pos.lng))

            Marker(
                state = markerState,
                title = item.station.name,
                snippet = item.id
            )
        }
    }
}
