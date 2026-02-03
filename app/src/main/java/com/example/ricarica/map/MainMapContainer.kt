package com.example.ricarica.map
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ricarica.data.model.StationItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MainMapContainer(
    stations: List<StationItem>,
    contentPadding: PaddingValues,
    onMarkerClick: (StationItem) -> Unit
) {
    val torino = LatLng(45.0703, 7.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(torino, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding, // Importante per non coprire i loghi Google con la sheet
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false, // Spesso rimosse per un look più moderno
            myLocationButtonEnabled = true
        )
    ) {
        stations.forEach { item ->
            val pos = item.station.position ?: return@forEach
            Marker(
                state = rememberMarkerState(position = LatLng(pos.lat, pos.lng)),
                title = item.station.name,
                onClick = {
                    onMarkerClick(item)
                    true // "Consuma" il click così la camera non si sposta da sola
                }
            )
        }
    }
}