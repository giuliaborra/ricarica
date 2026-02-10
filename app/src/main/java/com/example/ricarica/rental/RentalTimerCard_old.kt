package com.example.ricarica.rental
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ricarica.DtmfPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun RentalTimerCard(
    rental: Rental,
    onTimerExpired: () -> Unit = {},
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isExpanded: Boolean
) {

    val scope = rememberCoroutineScope()
    val dtmfPlayer = remember { DtmfPlayer() }

    var timeLeft by remember { mutableStateOf("20:00") }

    LaunchedEffect(key1 = rental.endTime) {
        while (System.currentTimeMillis() < rental.endTime) {
            val now = System.currentTimeMillis()
            val remainingMillis = rental.endTime - now
            val minutes = (remainingMillis / 1000) / 60
            val seconds = (remainingMillis / 1000) % 60
            timeLeft = String.format("%02d:%02d", minutes, seconds)
            delay(1000L)
        }
        timeLeft = "00:00"
        onTimerExpired()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp), // Padding ridotto per il "peek"
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SEMPRE VISIBILE (Sia ridotto che espanso) ---
        Text(
            text = "Prenotazione in corso",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Timer leggermente più piccolo se la barra è chiusa per risparmiare spazio
        Text(
            text = timeLeft,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (isExpanded) 64.sp else 48.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (timeLeft.startsWith("00")) Color.Red else MaterialTheme.colorScheme.primary
        )

        // --- VISIBILE SOLO QUANDO ESPANSO ---
        // Usiamo AnimatedVisibility per una transizione fluida
        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recati alla stazione e inserisci la passkey:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // BOX DELLA PASSKEY
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = rental.unlock_code.toString(),
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    )
                }

                Text(
                    text = "Scade il: ${formatTime(rental.endTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BOTTONI
                // ... dentro RentalTimerCard ...

                Button(
                    onClick = {


                            // 2. IL SUONO È FINITO. Ora puoi confermare.

                            onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ritira (Concludi prenotazione)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annulla Prenotazione")
                }
            }
        }
    }
}

// Funzione utile per mostrare l'orario di scadenza (es. "15:30")
fun formatTime(millis: Long): String {
    val date = java.util.Date(millis)
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}