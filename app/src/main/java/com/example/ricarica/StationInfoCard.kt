package com.example.ricarica

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.rental.Rental
import com.example.ricarica.rental.RentalViewModel // Assicurati che il package sia giusto
import PowerBank // La tua sealed class

// 1. Definiamo gli stati per sicurezza (se non li hai in un altro file Utils)
object LockerStatus {
    const val FREE = "FREE"           // Vuoto (Restituzione)
    const val OCCUPIED = "OCCUPIED"   // Pieno (Noleggio disponibile)
    const val RESERVED = "RESERVED"   // Prenotato (Timer in corso)
    const val BLOCKED = "BLOCKED"     // Guasto
}

@Composable
fun StationInfoCard(
    station: StationItem,
    //isExpanded: Boolean,
    rentalViewModel: RentalViewModel = viewModel(),
    onRentalSuccess: (Rental) -> Unit // <--- Callback per avvisare la Home
) {
    // STATO SELEZIONE UTENTE: Quanti ne vuole comprare ora
    val selectionState = remember { mutableStateMapOf<PowerBank, Int>() }

    // CALCOLO DINAMICO DELLE DISPONIBILITÀ (Reattivo al DB)
    // Ogni volta che 'station' cambia (aggiornamento Firebase), questo blocco viene rieseguito.
    val counts = remember(station) {
        val lockers = station.station.lockers.values

        // Regola d'oro: È disponibile SOLO se è "OCCUPIED" (cioè c'è il PB dentro ed è libero)
        val basicCount = lockers.count {
            it.type?.contains("Basic", ignoreCase = true) == true &&
                    (it.state == LockerStatus.OCCUPIED)
        }

        val fastCount = lockers.count {
            it.type?.contains("Fast", ignoreCase = true) == true &&
                    it.state == LockerStatus.OCCUPIED
        }

        val proCount = lockers.count {
            it.type?.contains("Pro", ignoreCase = true) == true &&
                    it.state == LockerStatus.OCCUPIED
        }

        mapOf(
            PowerBank.Basic to basicCount,
            PowerBank.Fast to fastCount,
            PowerBank.Pro to proCount
        )
    }

    // Calcolo totale selezionato (per abilitare il bottone)
    val totalSelected by remember {
        derivedStateOf { selectionState.values.sum() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Intestazione
            Text(
                text = station.station.name ?: "Stazione di Ricarica",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            // LISTA DEI TIPI DI POWER BANK
            // Creiamo una riga per ogni tipo definito nella tua Sealed Class
            val types = listOf(PowerBank.Basic, PowerBank.Fast, PowerBank.Pro)

            types.forEach { type ->
                val available = counts[type] ?: 0
                val selected = selectionState[type] ?: 0

                PowerBankSelectionRow(
                    powerBank = type,
                    availableCount = available,
                    currentSelection = selected,
                    onSelectionChanged = { newQty ->
                        // Aggiorniamo la selezione
                        selectionState[type] = newQty
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTTONE PROCEDI
            Button(
                onClick = {
                    rentalViewModel.createRental(
                        stationId = station.id, // Usa l'ID corretto (uid o id)
                        rawSelection = selectionState,
                        onSuccess = { rental ->
                            // Resettiamo la selezione
                            selectionState.clear()
                            // Avvisiamo la Home Page per far partire il Timer
                            onRentalSuccess(rental)
                        },
                        onError = { errorMsg ->
                            // Qui potresti mostrare un Toast o Snackbar
                            println("ERRORE: $errorMsg")
                        },
                        onNotLoggedIn = {
                            println("Login richiesto")
                            // Qui dovresti gestire il login (es. callback onShowLogin)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                // Il bottone è attivo SOLO se ho selezionato qualcosa (> 0)
                enabled = totalSelected > 0
            ) {
                Text("PROCEDI AL NOLEGGIO" + if (totalSelected > 0) " ($totalSelected)" else "")
            }
        }
    }
}

// COMPONENTE RIGA (UI Pura - Non ha stato interno)
@Composable
fun PowerBankSelectionRow(
    powerBank: PowerBank,
    availableCount: Int,
    currentSelection: Int,
    onSelectionChanged: (Int) -> Unit
) {
    // Calcoliamo quanti ne rimangono visivamente
    val remainingVisual = availableCount - currentSelection

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LATO SINISTRO: Info
        Column(modifier = Modifier.weight(1f)) {
            Text(text = powerBank.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = powerBank.features.firstOrNull() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // LATO DESTRO: Controlli (+ / -)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Tasto MENO
                IconButton(
                    onClick = { if (currentSelection > 0) onSelectionChanged(currentSelection - 1) },
                    enabled = currentSelection > 0
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Rimuovi")
                }

                Text(
                    text = "$currentSelection",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )

                // Tasto PIÙ
                // Si abilita solo se la selezione è minore del totale disponibile (REALE)
                IconButton(
                    onClick = { if (currentSelection < availableCount) onSelectionChanged(currentSelection + 1) },
                    enabled = currentSelection < availableCount
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                }
            }

            // Testo disponibilità
            Text(
                text = if (availableCount > 0) "$remainingVisual disponibili" else "Non disponibili",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (remainingVisual > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}