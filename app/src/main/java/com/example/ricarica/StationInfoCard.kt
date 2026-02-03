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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Input
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewModelScope

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
    isExpanded: Boolean,
    rentalViewModel: RentalViewModel = viewModel(),
    onRentalSuccess: (List<Rental>) -> Unit // <--- Callback per avvisare la Home
) {
    // STATO SELEZIONE UTENTE: Quanti ne vuole comprare ora
    val selectionState = remember { mutableStateMapOf<PowerBank, Int>() }

    // CALCOLO DINAMICO DELLE DISPONIBILITÀ (Reattivo al DB)
    // Ogni volta che 'station' cambia (aggiornamento Firebase), questo blocco viene rieseguito.
    // ELIMINA IL REMEMBER. Scrivi solo questo:
    val lockers = station.station.lockers.values
    val counts: Map<String, Int> = mapOf(
        "BASIC" to lockers.count { it.state == "OCCUPIED" && it.type == "BASIC" },
        "FAST" to lockers.count { it.state == "OCCUPIED" && it.type == "FAST" },
        "PRO" to lockers.count { it.state == "OCCUPIED" && it.type == "PRO" }
    )



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
    )

    {
        if(isExpanded) {

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )
            {
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
                val types = listOf(PowerBank.BASIC, PowerBank.FAST, PowerBank.PRO)

                types.forEach { type ->

                    val typeKey = when (type) {
                        is PowerBank.BASIC -> "BASIC"
                        is PowerBank.FAST -> "FAST"
                        is PowerBank.PRO -> "PRO"
                    }

                    val available = counts[typeKey] ?: 0
                    val selected = selectionState[type] ?: 0

                    PowerBankSelectionRow(
                        powerBank = type,
                        availableCount = available,
                        currentSelection = selected,
                        onSelectionChanged = { newQty ->
                            selectionState[type] = newQty
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BOTTONE PROCEDI
                Button(
                    onClick = {
                        val passkey =
                        rentalViewModel.createRental(

                            stationId = station.id,
                            rawSelection = selectionState,
                            onSuccess = { rental ->
                                selectionState.clear()
                                onRentalSuccess(rental)
                            },
                            onError = { errorMsg ->
                                // Qui potresti mostrare un Toast o Snackbar
                                println("ERRORE: $errorMsg") },
                            onNotLoggedIn = {}
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

        else {
            StationInfoCardPartially(
                station = station

            )

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


@Composable
fun StationInfoCardPartially(
    station: StationItem
) {
    // 1. Logica di conteggio (Corretta)
    val powerBanksAvailable = station.station.lockers.values.count { it.state == LockerStatus.OCCUPIED }
    val emptySlotsAvailable = station.station.lockers.values.count { it.state == LockerStatus.FREE }

    // 2. LAYOUT VISIVO AD ALTO CONTRASTO
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- PRELIEVO: SFONDO VERDE SCURO, TESTO BIANCO ---
        HighContrastStatItem(
            icon = Icons.Default.BatteryChargingFull,
            count = powerBanksAvailable,
            label = "Preleva",
            mainColor = Color.White,       // Testo
            bgColor = Color(0xFF2E7D32)    // Sfondo Verde
        )

        // --- LINEA DIVISORIA ---
        Divider(
            modifier = Modifier
                .height(50.dp)
                .width(1.dp),
            color = Color.LightGray.copy(alpha = 0.5f)
        )

        // --- RESTITUZIONE: SFONDO ARANCIONE SCURO, TESTO BIANCO ---
        HighContrastStatItem(
            icon = Icons.Default.Input,
            count = emptySlotsAvailable,
            label = "Restituisci",
            mainColor = Color.White,       // Testo
            bgColor = Color(0xFFEF6C00)    // Sfondo Arancione
        )
    }
}

// Nuovo componente "Badge" per la massima visibilità
@Composable
fun HighContrastStatItem(
    icon: ImageVector,
    count: Int,
    label: String,
    mainColor: Color,
    bgColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp)) // Angoli arrotondati
            .background(bgColor)             // Colore di sfondo pieno
            .width(130.dp)                   // Larghezza fissa per simmetria
            .padding(vertical = 14.dp),      // Spaziatura interna
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = mainColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = mainColor
                )
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = mainColor.copy(alpha = 0.9f),
                letterSpacing = 1.sp,
                fontSize = 11.sp
            )
        }
    }
}

