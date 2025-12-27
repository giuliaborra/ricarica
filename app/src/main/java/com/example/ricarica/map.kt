package com.example.ricarica

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Preview
@Composable
fun MapTestScreen() {
    val torino = LatLng(45.0703, 7.6869)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(torino, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true
        )
    )
}
