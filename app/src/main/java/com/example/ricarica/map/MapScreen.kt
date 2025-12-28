package com.example.ricarica.map
import MapViewModel
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ricarica.StationInfoCard
import com.example.ricarica.data.model.StationItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapView(
    vm: MapViewModel,
    stations: List<StationItem>

) {
    val selectedStation = vm.selectedStation.value
    val torino = LatLng(45.0703, 7.6869)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(torino, 12f)
    }

    //voglio chiudere le informazioni della mia stazione
    if (selectedStation != null) {
        ModalBottomSheet(
            onDismissRequest = vm::dismissStation
        ) {
            StationInfoCard(selectedStation)
        }
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
                snippet = item.id,

                onClick = {
                    // --- 2. AGGIUNGI QUI IL TUO LOG DI DEBUG ---
                    Log.d("MapScreen_MarkerClick", "CLICK! Marker premuto: ID=${item.id}, Nome='${item.station.name}'")

                    // 3. Chiama la funzione del ViewModel come prima
                    vm.onMarkerClick(item)

                    // Restituisce true per indicare che l'evento è stato gestito.
                    true
                }
            )
        }
    }
}
