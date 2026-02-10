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
    onCancel: (Rental) -> Unit
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
    val progress = (timeLeftMillis.toFloat() / maxTimeMillis.toFloat()).coerceIn(0f, 1f)

    val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis)
    val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis) % 60
    val timeString = String.format("%02d:%02d", minutesLeft, secondsLeft)
    val barColor = if (minutesLeft > 5) PowerGreen else ErrorColor

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
                    text = "Prenotazione Attiva",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = PowerGreen,
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
                text = "Scade in: $timeString",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = barColor,
                modifier = Modifier.align(Alignment.End),

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
                // COLONNA PRINCIPALE DEL DIALOG
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(), // Assicura che la colonna usi tutto lo spazio
                    horizontalAlignment = Alignment.CenterHorizontally, // Centra orizzontalmente i figli
                    verticalArrangement = Arrangement.Center
                ) {

                    if (!isTransmitting) {
                        // --- FASE 1: ISTRUZIONI ---
                        Text(
                            text = "Ritiro PowerBank",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            textAlign = TextAlign.Center // Centratura Testo
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Avvicina il telefono alla stazione e premi il pulsante qui sotto.",
                            textAlign = TextAlign.Center, // Centratura Testo
                            color = TextGray
                        )
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = {
                                isTransmitting = true
                                scope.launch {

                                    val sequence = "*${rental.unlock_code}#"

                                    dtmfPlayer.playSequence(sequence) {}
                                        //onConfirm(rental)
                                        showDialog = false
                                        isTransmitting = false

                                }
                            },
                            enabled = timeLeftMillis > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PowerGreen)
                        ) {
                            Text(
                                text = if (timeLeftMillis > 0) "RITIRA ORA" else "SCADUTO",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (timeLeftMillis > 0) {
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { onCancel(rental) }) {
                                Text("Annulla Prenotazione", color = TextGray)
                            }
                        }

                    } else {
                        // --- FASE 2: TRASMISSIONE (ONDE) ---
                        // Centratura forzata di tutto il blocco

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(150.dp) // Box quadrato per centrare l'animazione
                        ) {
                            PulsatingEffectTimer(color = PowerGreen)
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = "Trasmissione in corso...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PowerGreen,
                            textAlign = TextAlign.Center, // <-- FONDAMENTALE PER CENTRARE IL TESTO
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Mantieni il telefono vicino",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            textAlign = TextAlign.Center, // <-- FONDAMENTALE PER CENTRARE IL TESTO
                            modifier = Modifier.fillMaxWidth()
                        )
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