package com.example.ricarica.map

import MapViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ricarica.StationInfoCard
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.rental.MultiRentalTimerCard
import com.example.ricarica.rental.Rental

// private val x = Color(0xFFE8F5E9) // Non serve più se usiamo Transparent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewModern(
    vm: MapViewModel,
    stations: List<StationItem>,
    rentals: List<Rental>,
    isGuest: Boolean,
    onLoginRequest: () -> Unit,
    onConfirmRental: (Rental) -> Unit,
    onDeleteReserved: (Rental) -> Unit
) {
    var showGuestDialog by remember { mutableStateOf(false) }

    // Calcolo stato UI
    val uiState = vm.computeSheetState(rentals)

    val scaffoldState = rememberBottomSheetScaffoldState()

    // Calcolo dinamico altezza (Peek Height)
    // Assicurati che 240.dp sia sufficiente per mostrare la "StationInfoCardPartially"
    val dynamicPeekHeight = when (uiState) {
        is MapSheetState.Reserved -> 180.dp
        is MapSheetState.StationInfo -> 230.dp
        MapSheetState.Hidden -> 0.dp
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = dynamicPeekHeight,

        // 1. IMPORTANTE: Sfondo trasparente
        // Così si vede solo la tua Card arrotondata e non il rettangolo del foglio sotto
        sheetContainerColor = Color.Transparent,

        // 2. IMPORTANTE: Rimuovi la maniglia esterna
        // Perché l'hai già aggiunta dentro StationInfoCard
        sheetDragHandle = null,

        sheetSwipeEnabled = true,
        sheetContent = {
            // Box contenitore trasparente
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                when (uiState) {
                    is MapSheetState.Reserved -> {
                        // Nota: Se anche questa card deve avere la maniglia,
                        // dovrai aggiungerla dentro MultiRentalTimerCard come hai fatto per l'altra.
                        MultiRentalTimerCard(
                            rentals = uiState.rentals,
                            onConfirm = { onConfirmRental(it) },
                            onCancel = { onDeleteReserved(it) },
                            onTimerExpired = { onDeleteReserved(it) }
                        )
                    }
                    is MapSheetState.StationInfo -> {
                        val currentId = uiState.station.id
                        val liveStation = stations.find { it.id == currentId } ?: uiState.station

                        StationInfoCard(
                            station = liveStation,
                            onDismiss = {
                                vm.dismissStation()
                                // Opzionale: se vuoi che il foglio scenda anche visivamente
                                // coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            },
                            onRentalSuccess = { vm.dismissStation() },
                            // Passiamo lo stato corretto per l'espansione
                            isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
                        )
                    }
                    MapSheetState.Hidden -> Spacer(Modifier.height(1.dp))
                }
            }
        }
    ) { paddingValues ->


        MainMapContainer(
            stations = stations,
            contentPadding = paddingValues,
            onMarkerClick = { station ->
                if (isGuest) {
                    showGuestDialog = true
                } else {
                    vm.onMarkerClick(station)
                }
            }
        )


        if (showGuestDialog) {
            AlertDialog(
                onDismissRequest = { showGuestDialog = false },
                title = { Text("Accesso Richiesto") },
                text = { Text("Per noleggiare un PowerBank o vedere i dettagli della stazione devi accedere o registrarti.") },
                confirmButton = {
                    Button(onClick = {
                        showGuestDialog = false
                        onLoginRequest()
                    }) { Text("Accedi") }
                },
                dismissButton = {
                    TextButton(onClick = { showGuestDialog = false }) { Text("Chiudi") }
                },
                containerColor = Color.White
            )
        }
    }
}