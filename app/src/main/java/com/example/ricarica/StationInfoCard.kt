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
import com.example.ricarica.data.model.StationItem
import PowerBank // Riferimento alla sealed class in PowerBank.kt
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun StationInfoCard(
    station: StationItem,
    isExpanded: Boolean
) {
    // Calcolo dinamico con LOG di verifica
    val counts = remember(station) {
        val lockers = station.station.lockers.values

        //CONTEGGIO FILTRATO
        val basicCount = lockers.count { it.powerBankId != null && it.type?.lowercase() == "basic" }
        val fastCount = lockers.count { it.powerBankId != null && it.type?.lowercase() == "fast" }
        val proCount = lockers.count { it.powerBankId != null && it.type?.lowercase() == "pro" }

        //creo mappa tipo : contatore
        val map = mapOf(
            PowerBank.Basic to basicCount,
            PowerBank.Fast to fastCount,
            PowerBank.Pro to proCount
        )
        map
    }

    //questa funzione mi calcola il numero totale di powerBank dimenticandosi del loro tipo --> serve per la modalità non in espansione
    val totalAvailable = counts.values.sum()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Aggiunto per scrollare se i dati sono molti
        ) {
            Text(
                text = station.station.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (!isExpanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Powerbank disponibili: $totalAvailable",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(Modifier.height(16.dp))
                Text("Seleziona tipologia:", style = MaterialTheme.typography.titleMedium)

                val types = listOf(PowerBank.Basic, PowerBank.Fast, PowerBank.Pro)

                types.forEach { type ->
                    val availableForType = counts[type] ?: 0
                    PowerBankSelectionRow(
                        powerBank = type,
                        availableCount = availableForType
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { /* Noleggio */ },
                    modifier = Modifier.fillMaxWidth(),

                    //DA CAMBIARE
                    enabled = totalAvailable > 0
                ) {
                    Text("PROCEDI AL NOLEGGIO")
                }
            }
        }
    }
}

@Composable
fun PowerBankSelectionRow(
    powerBank: PowerBank,
    availableCount: Int
) {
    // Quantità selezionata dall'utente per questo specifico tipo
    var selectedQuantity by remember { mutableIntStateOf(0) }

    val remainingCount = availableCount - selectedQuantity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Parte Sinistra: Titolo e caratteristiche dal file PowerBank.kt
        Column(modifier = Modifier.weight(1f)) {
            Text(text = powerBank.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = powerBank.features.firstOrNull() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Parte Destra: Selettore [- 0 +] e disponibilità
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bottone MENO: si disattiva se la selezione è a zero
                IconButton(
                    onClick = { if (selectedQuantity > 0) selectedQuantity-- },
                    enabled = selectedQuantity > 0
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Rimuovi")
                }

                Text(
                    text = "$selectedQuantity",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )

                // Bottone PIÙ: si disattiva se si raggiunge la disponibilità massima del tipo
                IconButton(
                    onClick = { if (selectedQuantity < availableCount) selectedQuantity++ },
                    enabled = selectedQuantity < availableCount
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                }
            }

            // Testo disponibilità sotto i pulsanti
            Text(
                text = if (availableCount > 0) "$remainingCount disponibili" else "zero disponibili",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (remainingCount > 0)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}