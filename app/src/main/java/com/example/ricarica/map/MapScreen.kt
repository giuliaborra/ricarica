package com.example.ricarica.map

import MapViewModel
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ricarica.StationInfoCard
import com.example.ricarica.data.model.StationItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

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

    // Gestisce lo stato dello scaffold
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false // Permette di nasconderla completamente
        )
    )

    // Monitoriamo l'espansione per la StationInfoCard
    val isExpanded by remember {
        derivedStateOf { scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded }
    }

    // Quando selezioni una stazione, mostriamo la sheet (se era nascosta)
    LaunchedEffect(selectedStation) {
        if (selectedStation != null) {
            scaffoldState.bottomSheetState.partialExpand()
        } else {
            scaffoldState.bottomSheetState.hide()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (selectedStation != null) 150.dp else 0.dp, // Altezza fissa quando ridotta
        sheetContent = {
            if (selectedStation != null) {
                StationInfoCard(
                    station = selectedStation,
                    isExpanded = isExpanded
                )
            } else {
                // Contenuto vuoto per evitare crash quando non c'è selezione
                Spacer(Modifier.height(1.dp))
            }
        },
        sheetDragHandle = { BottomSheetDefaults.DragHandle() }
    ) { paddingValues ->
        // IL CONTENUTO PRINCIPALE (MAPPA)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues, // Evita che i tasti Google finiscano sotto la sheet
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            stations.forEach { item ->
                val pos = item.station.position ?: return@forEach
                Marker(
                    state = rememberMarkerState(position = LatLng(pos.lat, pos.lng)),
                    title = item.station.name,
                    onClick = {
                        vm.onMarkerClick(item)
                        true
                    }
                )
            }
        }
    }
}