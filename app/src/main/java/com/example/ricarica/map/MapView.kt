package com.example.ricarica.map
import MapSheetState
import MapViewModel
import MultiRentalTimerCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ricarica.StationInfoCard
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.rental.ActiveRentalsSection
import com.example.ricarica.rental.Rental


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    vm: MapViewModel,
    stations: List<StationItem>,
    rentals: List<Rental>,
    onConfirmRental: (Rental) -> Unit,
    onDeleteReserved: (Rental) -> Unit,
    onTerminateRental: (Rental) -> Unit
) {
    // 1. Calcolo dello stato (Come deciso nel passaggio precedente)
    val uiState = vm.computeSheetState(rentals)

    // 2. Configurazione dinamica
    val (peekHeight, canSkipHidden) = when (uiState) {
        is MapSheetState.Hidden -> 0.dp to false
        is MapSheetState.ActiveRentals -> 130.dp to true  // BLOCCATO: Non si può chiudere
        is MapSheetState.Reserved -> 160.dp to true       // BLOCCATO
        is MapSheetState.StationInfo -> 160.dp to false   // QUESTO SI PUÒ CHIUDERE
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = key(uiState) {
            rememberStandardBottomSheetState(
                initialValue = if (uiState is MapSheetState.Hidden) SheetValue.Hidden else SheetValue.PartiallyExpanded,
                skipHiddenState = canSkipHidden
            )
        }
    )

    // -------------------------------------------------------------------------
    // LOGICA DI RIMBALZO (IL FIX È QUI)
    // -------------------------------------------------------------------------

    // A. Se lo stato cambia (es. da StationInfo a ActiveRentals), forziamo l'apertura
    LaunchedEffect(uiState) {
        if (uiState !is MapSheetState.Hidden) {
            // Se la barra è nascosta ma ora dobbiamo mostrare qualcosa, la tiriamo su
            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                scaffoldState.bottomSheetState.partialExpand()
            }
        } else {
            scaffoldState.bottomSheetState.hide()
        }
    }

    // B. Se l'utente SWIPA GIÙ la scheda stazione, dobbiamo resettare la selezione nel VM.
    // Questo farà scattare il caso A immediatamente dopo.
    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
            // Se l'utente ha nascosto il foglio e c'era una stazione selezionata...
            if (vm.selectedStation.value != null) {
                vm.dismissStation() // ...la deselezioniamo!
            }
        }
    }

    // -------------------------------------------------------------------------

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        // Lasciamo sempre lo swipe abilitato, skipHiddenState gestisce se si può chiudere del tutto o no
        sheetSwipeEnabled = true,
        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight() // IMPORTANTE: dà corpo al foglio per lo swipe
                    .padding(bottom = 12.dp)
            ) {
                when (uiState) {
                    is MapSheetState.Reserved -> {
                        MultiRentalTimerCard(
                            rentals = uiState.rentals,
                            onConfirm = { singleRental -> onConfirmRental(singleRental)},
                            onCancel = { singleRental -> onDeleteReserved(singleRental) },
                            isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
                        )
                    }
                    is MapSheetState.StationInfo -> {
                        val currentId = uiState.station.id
                        val liveStation = stations.find { it.id == currentId } ?: uiState.station

                        StationInfoCard(
                            station = liveStation,
                            onRentalSuccess = { /* Gestito dai dati */ },
                            isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
                        )
                    }
                    is MapSheetState.ActiveRentals -> {
                        ActiveRentalsSection(
                            activeRentals = uiState.rentals,
                            onTerminateClick = onTerminateRental,
                            isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
                        )
                    }
                    MapSheetState.Hidden -> Spacer(Modifier.height(1.dp))
                }
            }
        },
        sheetDragHandle = { BottomSheetDefaults.DragHandle() }
    ) { paddingValues ->
        MainMapContainer(
            stations = stations,
            contentPadding = paddingValues,
            onMarkerClick = { vm.onMarkerClick(it) },
            // IMPORTANTE: Se clicchi sulla mappa vuota, deselezioni la stazione

        )
    }
}