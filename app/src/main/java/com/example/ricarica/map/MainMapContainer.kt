package com.example.ricarica.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import bitmapDescriptorFromVector
import com.example.ricarica.R
import com.example.ricarica.data.model.StationItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MainMapContainer(
    stations: List<StationItem>,
    contentPadding: PaddingValues,
    onMarkerClick: (StationItem) -> Unit
) {
    val torino = LatLng(45.0703, 7.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(torino, 14f)
    }

    // 4. Crea lo scope per le coroutines
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val customMarkerIcon = remember {
        bitmapDescriptorFromVector(context, R.drawable.new_marker)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = true
        )
    ) {
        stations.forEach { item ->
            val pos = item.station.position ?: return@forEach

            Marker(
                state = rememberMarkerState(position = LatLng(pos.lat, pos.lng)),
                title = item.station.name,
                icon = customMarkerIcon,
                onClick = {
                    // 5. Avvia la sequenza Animazione -> Apertura
                    scope.launch {
                        // A. Muovi la camera e aspetta che finisca (1000ms = 1 secondo)
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                LatLng(pos.lat, pos.lng),
                                17f // Livello di zoom desiderato quando ti avvicini
                            ),
                            durationMs = 800
                        )

                        // B. Solo ORA che l'animazione è finita, apri la scheda
                        onMarkerClick(item)
                    }
                    true // Ritorna true per dire che hai gestito il click
                }
            )
        }
    }
}