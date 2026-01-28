package com.example.ricarica.map

import MapViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import com.example.ricarica.rental.RentalTimerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapView(
    vm: MapViewModel,
    stations: List<StationItem>,
    rentals: List<Rental>,
    onConfirmRental: (Rental) -> Unit,
    onDeleteReserved: (Rental) -> Unit,
    onTerminateRental: (Rental) -> Unit
) {
    val selectedStation = vm.selectedStation.value

    var tempRental by remember { mutableStateOf<Rental?>(null) }

    val reservedRental = tempRental ?: rentals.find { it.state == "RESERVED" }
    val activeRentalsList = rentals.filter { it.state == "ACTIVE" }

    LaunchedEffect(rentals) {
        if (rentals.any { it.rentalId == tempRental?.rentalId }) {
            tempRental = null
        }
    }

    val isRentalMode = reservedRental != null || activeRentalsList.isNotEmpty()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = key(isRentalMode) {
            rememberStandardBottomSheetState(
                initialValue = if (isRentalMode) SheetValue.PartiallyExpanded else SheetValue.Hidden,
                skipHiddenState = isRentalMode
            )
        }
    )

    val shouldShowSheet = selectedStation != null || isRentalMode

    LaunchedEffect(shouldShowSheet) {
        if (shouldShowSheet) {
            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                scaffoldState.bottomSheetState.partialExpand()
            }
        } else {
            scaffoldState.bottomSheetState.hide()
        }
    }

    val isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded

    // MODIFICA 1: CALCOLO ALTEZZA DINAMICA
    // Se stiamo mostrando solo la "Active Bar" minimal, l'altezza deve essere ridotta (es. 80dp).
    // Se mostriamo il Timer di prenotazione o le Info Stazione, serve più spazio (es. 160dp).
    val dynamicPeekHeight = when {
        reservedRental != null -> 160.dp       // Serve spazio per il timer
        selectedStation != null -> 160.dp      // Serve spazio per info stazione
        activeRentalsList.isNotEmpty() -> 130.dp // BASTA POCO SPAZIO (solo la pillola colorata)
        else -> 0.dp
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // MODIFICA 2: Usa l'altezza calcolata qui sotto
        sheetPeekHeight = if (shouldShowSheet) dynamicPeekHeight else 0.dp,
        sheetSwipeEnabled = true,
        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                when {
                    reservedRental != null -> {
                        RentalTimerCard(
                            rental = reservedRental,
                            onConfirm = { onConfirmRental(reservedRental) },
                            onCancel = {
                                onDeleteReserved(reservedRental)
                                tempRental = null
                            },
                            isExpanded = isExpanded
                        )
                    }

                    selectedStation != null -> {
                        val freshStation = stations.find { it.id == selectedStation.id }
                        freshStation?.let {
                            StationInfoCard(
                                station = it,
                                onRentalSuccess = { newRental ->
                                    tempRental = newRental
                                },
                                isExpanded = isExpanded,
                            )
                        }
                    }

                    activeRentalsList.isNotEmpty() -> {
                        // MODIFICA 3: Passiamo isExpanded per far cambiare la UI da barra a lista
                        ActiveRentalsSection(
                            activeRentals = activeRentalsList,
                            onTerminateClick = onTerminateRental,
                            isExpanded = isExpanded
                        )
                    }

                    else -> {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        },
        sheetDragHandle = { BottomSheetDefaults.DragHandle() }
    ) { paddingValues ->
        MainMapContainer(
            stations = stations,
            contentPadding = paddingValues,
            onMarkerClick = { stationItem -> vm.onMarkerClick(stationItem) }
        )
    }
}