package com.example.ricarica.rental
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActiveRentalCard(
    rental: Rental,
    onTerminateClick: (Rental) -> Unit
) {
    var durationString by remember { mutableStateOf("00:00:00") }
    var currentCost by remember { mutableStateOf("0.00€") } // Esempio se vuoi mostrare costo in tempo reale

    // Cronometro: Calcola il tempo trascorso ogni secondo
    LaunchedEffect(key1 = rental.startTime) {
        while (true) {
            val now = System.currentTimeMillis()
            val durationMillis = now - rental.startTime

            // Formattazione HH:mm:ss
            val seconds = (durationMillis / 1000) % 60
            val minutes = (durationMillis / (1000 * 60)) % 60
            val hours = (durationMillis / (1000 * 60 * 60))

            durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            // Esempio calcolo costo (es. 1€ l'ora)
            // val cost = (durationMillis / (1000.0 * 60 * 60)) * 1.0
            // currentCost = String.format("%.2f€", cost)

            delay(1000L)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con Icona
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Noleggio Attivo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Grande
            Text(
                text = durationString,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Tempo trascorso",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dettagli Power Bank (Tipo e Quantità)
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Iniziato il:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatDateTime(rental.startTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mostra i tipi di powerbank noleggiati
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Dispositivi:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))

                // Cicla sulla mappa dei tipi (es: BASIC: 1, FAST: 0)
                rental.powerBankTypes.filter { it.value > 0 }.forEach { (type, quantity) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = type, // Es. "FAST"
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = "x$quantity",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottone Termina
            Button(
                onClick = { onTerminateClick(rental) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Termina Noleggio")
            }
        }
    }
}

// Helper per formattare la data
fun formatDateTime(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Composable
fun CollapsedActiveRentalBar(rentals: List<Rental>) {
    // Calcoliamo il tempo per il primo noleggio (giusto per avere un feedback vivo)
    val firstRental = rentals.firstOrNull()
    var timerText by remember { mutableStateOf("00:00") }

    LaunchedEffect(firstRental) {
        firstRental?.let { rental ->
            while (true) {
                val now = System.currentTimeMillis()
                val duration = now - rental.startTime
                val minutes = (duration / 1000) / 60
                val hours = minutes / 60
                // Formato compatto: solo ore:minuti o minuti
                timerText = if (hours > 0) {
                    String.format("%d:%02dh", hours, minutes % 60)
                } else {
                    String.format("%d min", minutes)
                }
                delay(10000L) // Aggiorniamo ogni 10s per risparmiare risorse in background
            }
        }
    }

    // --- DESIGN A "PILLOLA GALLEGGIANTE" ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp) // Margine dai bordi laterali
            .height(56.dp), // Altezza standard ergonomica
        shape = RoundedCornerShape(30), // Bordi completamente rotondi (a pillola)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer, // Colore vivo ma non troppo forte
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // L'ombra lo fa "galleggiare" sulla mappa
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Icona e Testo a sinistra
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cerchietto icona
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "In Carica",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Mostriamo il timer piccolino per dare feedback che è attivo
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 2. Indicatore visuale a destra (Freccina o Badge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rentals.size > 1) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text("+${rentals.size - 1}")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Icona che suggerisce lo scorrimento
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Apri",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}