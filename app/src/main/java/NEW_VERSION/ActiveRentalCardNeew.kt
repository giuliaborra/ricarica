package com.example.ricarica.rental

import PowerBank
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning // Aggiunta icona Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// --- COLORI STYLE ---
private val PowerGreen = Color(0xFF2E7D32)
private val PowerOrange = Color(0xFFEF6C00)
private val ErrorColor = Color(0xFFB00020)
private val CardBackground = Color.White
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val LightUiBg = Color(0xFFF5F5F5)

private enum class ReturnState {
    IDLE,
    PREPARING,
    READY_TO_OPEN,
    VERIFYING // <--- NUOVO STATO: Stiamo controllando se si è chiuso
}

@Composable
fun ModernActiveRentalCard(
    rental: Rental,
    rentalViewModel: RentalViewModel,
    dtmfPlayer: DtmfPlayer
) {
    val scope = rememberCoroutineScope()

    // Stati UI e Dialog
    var currentState by remember { mutableStateOf(ReturnState.IDLE) }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dati recuperati dal server
    var returnCode by remember { mutableIntStateOf(0) }

    // Logica Timer e Costi (Invariata)
    val powerBankStrategy = remember(rental) {
        val typeKey = rental.powerBankTypes?.keys?.firstOrNull() ?: "BASIC"
        when (typeKey) {
            "FAST" -> PowerBank.FAST
            "PRO" -> PowerBank.PRO
            else -> PowerBank.BASIC
        }
    }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    val durationMillis = (currentTime - (rental.startTime ?: currentTime)).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    val cost = minutes * powerBankStrategy.pricePerMinute

    // Osserviamo il lifecycle del noleggio
    LaunchedEffect(rental.rentalId) {
        rentalViewModel.watchRentalLifecycle(rental.rentalId)
    }

    // Se lo stato diventa COMPLETED mentre il dialog è aperto, chiudiamo tutto (Successo)
    LaunchedEffect(rental.state) {
        if (rental.state == "COMPLETED") {
            showDialog = false

        }
    }

    // --- CARD PRINCIPALE (Invariata) ---
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(PowerGreen.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ElectricBolt, null, tint = PowerGreen, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(powerBankStrategy.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextBlack)
                    Text("Tariffa: ${powerBankStrategy.pricePerMinute}€/min", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Restituire presso: Stazione ${rental.stationId}", style = MaterialTheme.typography.labelSmall, color = PowerOrange, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            // Info Block
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LightUiBg).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EnhancedDataBlock(Icons.Default.Timer, formatDuration(durationMillis), "Tempo", PowerOrange, Modifier.weight(1f))
                Divider(color = Color.Gray.copy(0.2f), modifier = Modifier.height(40.dp).width(1.dp))
                EnhancedDataBlock(Icons.Default.Euro, "%.2f€".format(cost), "Costo", TextBlack, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Bottone Termina
            Button(
                onClick = {
                    showDialog = true
                    currentState = ReturnState.PREPARING
                    errorMessage = null

                    rentalViewModel.prepareReturn(
                        rental = rental,
                        onReadyToPlay = { code ->
                            returnCode = code
                            currentState = ReturnState.READY_TO_OPEN
                        },
                        onError = {
                            errorMessage = it
                            currentState = ReturnState.IDLE
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
            ) {
                Icon(Icons.Outlined.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("TERMINA NOLEGGIO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // --- DIALOG MODALE ---
    if (showDialog) {
        // Variabile locale per gestire la riproduzione audio
        var isPlaying by remember { mutableStateOf(false) }

        Dialog(
            // Impediamo di chiudere mentre sta verificando o suonando
            onDismissRequest = {
                if (!isPlaying && currentState != ReturnState.VERIFYING) showDialog = false
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentState) {
                        ReturnState.PREPARING, ReturnState.IDLE -> {
                            CircularProgressIndicator(color = PowerGreen)
                            Spacer(Modifier.height(16.dp))
                            Text("Cerco uno slot libero...", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        ReturnState.VERIFYING -> {
                            // --- STATO DI VERIFICA DOPO IL SUONO ---
                            CircularProgressIndicator(color = PowerOrange)
                            Spacer(Modifier.height(16.dp))
                            Text("Verifico la chiusura...", fontWeight = FontWeight.Bold, color = PowerOrange)

                            // Logica di Timeout: Se dopo 8 secondi non è chiuso, diamo errore
                            LaunchedEffect(Unit) {
                                delay(8000) // Aspetta 8 secondi
                                // Se siamo ancora qui, vuol dire che lo stato non è diventato COMPLETED
                                if (rental.state != "COMPLETED") {
                                    errorMessage = "Riconsegna non rilevata. Assicurati che il telefono sia vicino e riprova."
                                    currentState = ReturnState.READY_TO_OPEN
                                }
                            }
                        }

                        ReturnState.READY_TO_OPEN -> {
                            // --- ZONA VISIVA ---
                            Box(
                                modifier = Modifier.height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPlaying) {
                                    PulsatingEffect(color = PowerGreen)
                                    Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(40.dp))
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.LockOpen, null, tint = PowerGreen, modifier = Modifier.size(64.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("Slot Trovato!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = if(isPlaying) "Trasmissione in corso..." else "Avvicina il telefono e premi il pulsante.",
                                textAlign = TextAlign.Center,
                                color = if(isPlaying) PowerGreen else TextGray,
                                fontWeight = if(isPlaying) FontWeight.Bold else FontWeight.Normal
                            )

                            Spacer(Modifier.height(32.dp))

                            // --- BOTTONE UNICO (Start / Replay) ---
                            Button(
                                onClick = {
                                    if (!isPlaying) {
                                        isPlaying = true
                                        errorMessage = null // Reset errore precedente
                                        scope.launch {
                                            val sequence = "#$returnCode*"

                                            // 1. Riproduci la sequenza
                                            dtmfPlayer.playSequence(sequence) {}
                                                // 2. Quando finisce il suono:
                                                isPlaying = false
                                                // 3. Passa allo stato di verifica
                                                currentState = ReturnState.VERIFYING

                                        }
                                    }
                                },
                                enabled = !isPlaying,
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PowerGreen,
                                    disabledContainerColor = Color.Gray
                                ),
                            ) {
                                if (isPlaying) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("INVIO SEGNALE...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("APRI SPORTELLO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // --- VISUALIZZAZIONE ERRORE (Se la verifica fallisce) ---
                    if (errorMessage != null && currentState == ReturnState.READY_TO_OPEN) {
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = ErrorColor,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!isPlaying && currentState != ReturnState.VERIFYING) {
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { showDialog = false }) {
                            Text("Chiudi", color = TextGray)
                        }
                    }
                }
            }
        }
    }
}

// ... Resto delle funzioni (PulsatingEffect, EnhancedDataBlock, formatDuration) rimangono uguali ...

// --- ANIMAZIONE RIPPLE ---
@Composable
private fun PulsatingEffect(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 2.4f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Restart)
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Restart)
    )

    Box(modifier = Modifier
        .size(80.dp)
        .background(color, CircleShape))
    Canvas(modifier = Modifier
        .size(80.dp)
        .scale(scale)) {
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 2)
    }
}

// --- UTILS UI ---
@Composable
fun EnhancedDataBlock(icon: ImageVector, value: String, label: String, accentColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextBlack, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}