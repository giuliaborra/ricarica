package com.example.ricarica.rental
import PowerBank
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ricarica.DtmfPlayer
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun ActiveRentalCard(
    rental: Rental,
    onTerminateClick: (Rental) -> Unit
) {
    // Stati per la UI che si aggiornano ogni secondo
    var durationString by remember { mutableStateOf("00:00:00") }
    var currentCostString by remember { mutableStateOf("€ 0.00") }

    // Determiniamo la strategia di prezzo UNA SOLA VOLTA all'avvio
    // (o quando cambia il rental, ma quello è improbabile in questa schermata)
    val powerBankStrategy = remember(rental) {
        val typeKey = rental.powerBankTypes.keys.firstOrNull() ?: "BASIC"
        when (typeKey) {
            "FAST" -> PowerBank.FAST
            "PRO" -> PowerBank.PRO
            else -> PowerBank.BASIC // Fallback su BASIC
        }
    }

    val scope = rememberCoroutineScope()
    val dtmfPlayer = remember { DtmfPlayer() }

    // --- CRONOMETRO E CALCOLATRICE IN TEMPO REALE ---
    LaunchedEffect(key1 = rental.startTime) {
        while (true) {
            val now = System.currentTimeMillis()
            val durationMillis = now - rental.startTime

            // 1. FORMATTAZIONE TEMPO (HH:mm:ss)
            val seconds = (durationMillis / 1000) % 60
            val minutes = (durationMillis / (1000 * 60)) % 60
            val hours = (durationMillis / (1000 * 60 * 60))
            durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            // 2. CALCOLO COSTO LIVE (Usa i dati della classe PowerBank)
            // Arrotondiamo i minuti per eccesso (es. 1min 1sec = 2min pagati)
            val totalMinutes = ceil(durationMillis / 60000.0).toLong()

            // Calcolo usando i campi della classe: pricePerMinute e maxDailyPrice
            var liveCost = totalMinutes * powerBankStrategy.pricePerMinute

            // Applica il tetto massimo giornaliero (se serve)
            if (liveCost > powerBankStrategy.maxDailyPrice) {
                liveCost = powerBankStrategy.maxDailyPrice
            }

            // Aggiornamento stringa costo
            currentCostString = String.format("€ %.2f", liveCost)

            // Aspetta 1 secondo prima di ricalcolare
            delay(1000)
        }
    }

    // --- UI ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // HEADER: Titolo e Icona
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Noleggio in Corso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))

                // TYPE
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = powerBankStrategy.title.replace("Power Bank ", ""), // Es: "Basic", "Fast"
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TIMER + COSTO

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // TEMPO
                Column {
                    Text(
                        text = "Tempo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = durationString,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Colonna Destra: COSTO
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Costo Attuale",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currentCostString,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // TERMINA NOLEGGIO
            Button(
                onClick = {
                    val passkey = rental.unlock_code
                    val sequence = "*${passkey}#"


                    //PER RIPRODURRE IL SUONO
                    /*
                    scope.launch{
                        dtmfPlayer.playSequence(sequence) { index ->

                            if (index != -1) {
                                println("Sta suonando il carattere numero: $index")
                            }
                        }
                    }
                    */

                    onTerminateClick(rental) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("RESTITUISCI POWERBANK")
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