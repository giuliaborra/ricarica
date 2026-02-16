package com.example.ricarica.rental

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ricarica.DtmfPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import watchRentalStatus
import java.util.concurrent.TimeUnit

// --- COLORI ---
private val PowerGreen = Color(0xFF2E7D32)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val ErrorColor = Color(0xFFB00020)

@Composable
fun MultiRentalTimerCard(
    rentals: List<Rental>,
    onConfirm: (Rental) -> Unit,
    onCancel: (Rental) -> Unit,
    // onTimerExpired non serve più obbligatoriamente se gestisci tutto col tasto,
    // ma puoi lasciarlo vuoto nel MapScreen se non vuoi cambiare quel file.
    onTimerExpired: (Rental) -> Unit
) {
    val rental = rentals.firstOrNull() ?: return
    val scope = rememberCoroutineScope()

    val dtmfPlayer = remember { DtmfPlayer() }
    DisposableEffect(Unit) {
        onDispose { dtmfPlayer.release() }
    }

    var showDialog by remember { mutableStateOf(false) }
    var isTransmitting by remember { mutableStateOf(false) }

    // --- TIMER 20 MINUTI ---
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val maxTimeMillis = 20 * 60 * 1000L
    val elapsedTime = currentTime - (rental.startTime ?: currentTime)
    val timeLeftMillis = (maxTimeMillis - elapsedTime).coerceAtLeast(0L)

    // NOTA: HO RIMOSSO LA LOGICA "if (timeLeftMillis <= 0) onTimerExpired"
    // Ora la cancellazione è manuale tramite il bottone.

    val progress = (timeLeftMillis.toFloat() / maxTimeMillis.toFloat()).coerceIn(0f, 1f)

    val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis)
    val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis) % 60
    val timeString = String.format("%02d:%02d", minutesLeft, secondsLeft)
    val barColor = if (minutesLeft > 5) PowerGreen else ErrorColor

    // ... all'interno di MultiRentalTimerCard, prima del layout ...

    val currentRental = rentals.firstOrNull()

    DisposableEffect(currentRental?.rentalId) {
        val rentalId = currentRental?.rentalId
        if (rentalId != null) {
            val ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("rentals/$rentalId/state")

            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(s: com.google.firebase.database.DataSnapshot) {
                    if (s.getValue(String::class.java) == "ACTIVE") {
                        onConfirm(currentRental)
                    }
                }
                override fun onCancelled(e: com.google.firebase.database.DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        } else {
            onDispose { }
        }
    }

    // --- CARD COMPATTA ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (timeLeftMillis > 0) "Prenotazione Attiva" else "Prenotazione Scaduta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (timeLeftMillis > 0) TextBlack else ErrorColor
                )
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = if (timeLeftMillis > 0) PowerGreen else ErrorColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = Color(0xFFEEEEEE),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (timeLeftMillis > 0) "Scade in: $timeString" else "Tempo Scaduto",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = barColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }

    // --- DIALOG MODALE ---
    if (showDialog) {
        Dialog(
            onDismissRequest = { if (!isTransmitting) showDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = !isTransmitting,
                dismissOnClickOutside = !isTransmitting
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    if (!isTransmitting) {
                        Text(
                            text = if (timeLeftMillis > 0) "Ritiro PowerBank" else "Tempo Scaduto",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeftMillis > 0) TextBlack else ErrorColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (timeLeftMillis > 0)
                                "Avvicina il telefono alla stazione e premi il pulsante qui sotto."
                            else "Il tempo per il ritiro è terminato. Annulla la prenotazione per liberare lo slot.",
                            textAlign = TextAlign.Center,
                            color = TextGray
                        )
                        Spacer(Modifier.height(32.dp))

                        // --- BOTTONE MODIFICATO ---
                        Button(
                            onClick = {
                                if (timeLeftMillis > 0) {
                                    // LOGICA RITIRO NORMALE
                                    isTransmitting = true
                                    scope.launch {

                                        val sequence = "*${rental.unlock_code}#"
                                        dtmfPlayer.playSequence(sequence) {}
                                        //watchRentalStatus(rental.rentalId, { onConfirm(rental) })
                                            showDialog = false
                                            isTransmitting = false

                                    }
                                } else {
                                    // LOGICA CANCELLAZIONE (SCADUTO)
                                    onCancel(rental)
                                    showDialog = false
                                }
                            },
                            // Sempre abilitato (tranne durante trasmissione suono)
                            enabled = !isTransmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                // VERDE se attivo, ROSSO se scaduto
                                containerColor = if (timeLeftMillis > 0) PowerGreen else ErrorColor
                            )
                        ) {
                            Text(
                                text = if (timeLeftMillis > 0) "RITIRA ORA" else "SCADUTO - ANNULLA",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tasto Annulla secondario (Mostralo solo se c'è ancora tempo,
                        // perché se è scaduto il tasto principale fa già da annulla)
                        if (timeLeftMillis > 0) {
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { onCancel(rental) }) {
                                Text("Annulla Prenotazione", color = TextGray)
                            }
                        }

                    } else {
                        // ... (Parte Onde Sonar identica a prima) ...
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                            PulsatingEffectTimer(color = PowerGreen)
                            Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Apertura in corso...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PowerGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("Mantieni il telefono vicino allo slot", style = MaterialTheme.typography.bodySmall, color = TextGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
// --- ANIMAZIONE RIPPLE ---
@Composable
fun PulsatingEffectTimer(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        )
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(color, CircleShape)
    )

    Canvas(modifier = Modifier.size(80.dp).scale(scale)) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = size.minDimension / 2
        )
    }
}