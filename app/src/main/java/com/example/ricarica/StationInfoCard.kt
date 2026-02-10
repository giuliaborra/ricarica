package com.example.ricarica

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.rental.Rental
import com.example.ricarica.rental.RentalViewModel
import PowerBank

object LockerStatus {
    const val FREE = "FREE"
    const val OCCUPIED = "OCCUPIED"
    const val RESERVED = "RESERVED"
    const val BLOCKED = "BLOCKED"
}

@Composable
fun StationInfoCard(
    station: StationItem,
    isExpanded: Boolean, // Questo arriva dal BottomSheet state
    rentalViewModel: RentalViewModel = viewModel(),
    onRentalSuccess: (List<Rental>) -> Unit,
    onDismiss: () -> Unit // <--- FONDAMENTALE PER CHIUDERE IL FOGLIO
) {
    // STATO SELEZIONE
    val selectionState = remember { mutableStateMapOf<PowerBank, Int>() }

    // CALCOLO DISPONIBILITÀ (Reattivo)
    val lockers = station.station.lockers.values
    val counts: Map<String, Int> = mapOf(
        "BASIC" to lockers.count { it.state == "OCCUPIED" && it.type == "BASIC" },
        "FAST" to lockers.count { it.state == "OCCUPIED" && it.type == "FAST" },
        "PRO" to lockers.count { it.state == "OCCUPIED" && it.type == "PRO" }
    )

    val totalSelected by remember { derivedStateOf { selectionState.values.sum() } }

    // CARD CONTENITORE PRINCIPALE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Rimuovi il padding orizzontale se vuoi che tocchi i bordi, o lascialo per effetto "floating"
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp) // Più rotondo
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HEADER COMUNE (Nome Stazione + Tasto X) ---
            // Visibile sia da chiuso che da aperto per coerenza
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.station.name ?: "Stazione Ricarica",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (!isExpanded) {
                        Text(
                            text = "Scorri su per noleggiare",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // TASTO CHIUDI (La X che ti serve!)
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONTENUTO DINAMICO ---
            if (isExpanded) {
                // VISTA ESPANSA: LISTA SELEZIONE
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

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
                        onSelectionChanged = { newQty -> selectionState[type] = newQty }
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        rentalViewModel.createRental(
                            stationId = station.id,
                            rawSelection = selectionState,
                            onSuccess = { rental ->
                                selectionState.clear()
                                onRentalSuccess(rental)
                            },
                            onError = { println("ERRORE: $it") },
                            onNotLoggedIn = {}
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32) // Verde PowerShare
                    ),
                    enabled = totalSelected > 0
                ) {
                    Text(
                        text = "PROCEDI AL NOLEGGIO" + if (totalSelected > 0) " ($totalSelected)" else "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Spazio extra in fondo per evitare che la navbar copra il bottone
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // VISTA RIDOTTA: SOLO STATISTICHE
                StationInfoCardPartially(station = station)
            }
        }
    }
}

// --- ROW SELEZIONE (Invariata ma pulita) ---
@Composable
fun PowerBankSelectionRow(
    powerBank: PowerBank,
    availableCount: Int,
    currentSelection: Int,
    onSelectionChanged: (Int) -> Unit
) {
    val remainingVisual = availableCount - currentSelection

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), // Più aria
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = powerBank.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = powerBank.features.firstOrNull() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (currentSelection > 0) onSelectionChanged(currentSelection - 1) },
                enabled = currentSelection > 0,
                modifier = Modifier.background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)).size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "$currentSelection",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { if (currentSelection < availableCount) onSelectionChanged(currentSelection + 1) },
                enabled = currentSelection < availableCount,
                modifier = Modifier.background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)).size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// --- VISTA RIDOTTA (Statistiche Grafiche) ---
@Composable
fun StationInfoCardPartially(
    station: StationItem
) {
    val powerBanksAvailable = station.station.lockers.values.count { it.state == LockerStatus.OCCUPIED }
    val emptySlotsAvailable = station.station.lockers.values.count { it.state == LockerStatus.FREE }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HighContrastStatItem(
            icon = Icons.Default.BatteryChargingFull,
            count = powerBanksAvailable,
            label = "Preleva",
            mainColor = Color.White,
            bgColor = Color(0xFF2E7D32) // Verde
        )

        Divider(
            modifier = Modifier
                .height(40.dp)
                .width(1.dp),
            color = Color.LightGray
        )

        HighContrastStatItem(
            icon = Icons.Default.Input,
            count = emptySlotsAvailable,
            label = "Restituisci",
            mainColor = Color.White,
            bgColor = Color(0xFFEF6C00) // Arancione
        )
    }
}

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
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .width(140.dp) // Leggermente più largo
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = mainColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineMedium, // Più grande
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