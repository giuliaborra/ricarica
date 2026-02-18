package com.example.ricarica.rental
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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

// --- COLORI STANDARD (Identici all'ActiveRentalCard) ---
private val PowerGreen = Color(0xFF2E7D32)
private val PowerOrange = Color(0xFFEF6C00)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val ErrorColor = Color(0xFFB00020)

private enum class PickupState {
    READY_TO_OPEN, // Stato iniziale
    VERIFYING,     // Ascolto serratura
    DOOR_OPEN,     // Sportello aperto (Preleva)
    ACTIVE         // Noleggio confermato
}

@Composable
fun MultiRentalTimerCard(
    rentals: List<Rental>,
    onConfirm: (Rental) -> Unit,
    onCancel: (Rental) -> Unit,
    rentalViewModel: RentalViewModel
) {
    val rental = rentals.firstOrNull() ?: return
    val scope = rememberCoroutineScope()

    val dtmfPlayer = remember { DtmfPlayer() }
    DisposableEffect(Unit) {
        onDispose { dtmfPlayer.release() }
    }

    // --- DATI LIVE ---
    val targetLockerStatus by rentalViewModel.targetLockerStatusFisico.collectAsState()
    val realTimeRentalState by rentalViewModel.returnRentalState.collectAsState()

    // --- STATI UI ---
    var showDialog by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf(PickupState.READY_TO_OPEN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

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


    LaunchedEffect(realTimeRentalState, targetLockerStatus) {
        // 1. Successo Totale: Noleggio diventato ACTIVE
        if (realTimeRentalState == "ACTIVE") {
            currentState = PickupState.ACTIVE
            delay(2000)
            showDialog = false

        }
        // 2. Successo Parziale: Sportello aperto
        else if (showDialog && targetLockerStatus == "OPEN" && currentState != PickupState.ACTIVE) {
            currentState = PickupState.DOOR_OPEN
            errorMessage = null
        }
    }

    // Osserviamo il lifecycle del noleggio
    LaunchedEffect(rental.rentalId) {
        rentalViewModel.watchRentalLifecycle(rental.rentalId, rental.stationId, rental.lockerId)
    }

    // Pulizia
    DisposableEffect(showDialog) {
        onDispose {
            if (!showDialog) {
                rentalViewModel.stopWatchingLocker(rental.stationId, rental.lockerId)

                // Reset stati
                currentState = PickupState.READY_TO_OPEN
                errorMessage = null
                isPlaying = false
            }
        }
    }

    // --- CARD DI ANTEPRIMA (Timer) ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                showDialog = true
                // Inizia il monitoraggio appena apri il popup
                rentalViewModel.watchLockerStatus(rental.stationId, rental.lockerId)

            },
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

    // --- POPUP UNIFORMATO ---
    if (showDialog) {
        Dialog(
            onDismissRequest = {
                // Non chiudere se sta lavorando
                if (!isPlaying && currentState != PickupState.VERIFYING && currentState != PickupState.DOOR_OPEN) {
                    showDialog = false
                }
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

                    // CASO 1: TEMPO SCADUTO (Logica speciale)
                    if (timeLeftMillis <= 0) {
                        Text("Tempo Scaduto", style = MaterialTheme.typography.headlineSmall, color = ErrorColor, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Il tempo per il ritiro è terminato. Annulla la prenotazione.", textAlign = TextAlign.Center, color = TextGray)
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { onCancel(rental); showDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("ANNULLA PRENOTAZIONE", fontWeight = FontWeight.Bold)
                        }
                    }
                    // CASO 2: FLUSSO NORMALE (Identico a ActiveRentalCard)
                    else {
                        when (currentState) {

                            // --- FASE 1: PRONTO ---
                            PickupState.READY_TO_OPEN -> {
                                val isErrorState = errorMessage != null
                                val stateColor = if (isErrorState) PowerOrange else PowerGreen

                                // Blocco Icona Grande
                                Box(
                                    modifier = Modifier
                                        .size(160.dp) // Dimensione unificata
                                        .background(stateColor.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPlaying) {
                                        PulsatingEffectTimer(color = stateColor)
                                        // IMPORTANTE: Bianco su sfondo colorato pieno
                                        Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(50.dp))
                                    } else {
                                        Icon(
                                            if (isErrorState) Icons.Default.Warning else Icons.Default.LockOpen,
                                            null, tint = stateColor, modifier = Modifier.size(60.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                Text(
                                    if (isPlaying) "Invio segnale..." else if (isErrorState) "Apertura fallita" else "Slot Pronto",
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextBlack
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (isPlaying) "Tieni il telefono vicino allo slot"
                                    else if (isErrorState) "Non abbiamo sentito lo scatto.\nAlza il volume e riprova."
                                    else "Avvicina il telefono allo slot e premi il pulsante.",
                                    textAlign = TextAlign.Center, color = TextGray
                                )

                                Spacer(Modifier.height(32.dp))

                                // Bottone Azione
                                Button(
                                    onClick = {
                                        if (!isPlaying) {
                                            isPlaying = true
                                            errorMessage = null
                                            scope.launch {
                                                val sequence = "*${rental.unlock_code}#"
                                                dtmfPlayer.playSequence(sequence) {}
                                                    isPlaying = false
                                                    // Controllo Immediato
                                                    if (targetLockerStatus == "OPEN") {
                                                        currentState = PickupState.DOOR_OPEN
                                                    } else {
                                                        currentState = PickupState.VERIFYING

                                                }
                                            }
                                        }
                                    },
                                    enabled = !isPlaying,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PowerGreen),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                ) {
                                    if (isPlaying) {
                                        /*
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(24.dp)
                                        )*/
                                        //Spacer(Modifier.width(12.dp))
                                        Text("TRASMISSIONE...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        if (isErrorState) {
                                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("RIPROVA APERTURA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("RITIRA ORA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (!isPlaying) {
                                    Spacer(Modifier.height(16.dp))
                                    TextButton(onClick = { onCancel(rental); showDialog = false }) {
                                        Text("Annulla Prenotazione", color = TextGray)
                                    }
                                }
                            }

                            // --- FASE 2: VERIFICA ---
                            PickupState.VERIFYING -> {
                                CircularProgressIndicator(color = PowerGreen)
                                Spacer(Modifier.height(16.dp))
                                Text("Ascolto scatto serratura...", fontWeight = FontWeight.Bold, color = PowerGreen)
                                Spacer(Modifier.height(8.dp))
                                Text("Il microfono sta elaborando il suono.", style = MaterialTheme.typography.bodySmall, color = TextGray)

                                LaunchedEffect(Unit) {
                                    delay(12000) // 12 Secondi timeout
                                    if (currentState == PickupState.VERIFYING && targetLockerStatus != "OPEN") {
                                        errorMessage = "Apertura non rilevata. Riprova."
                                        currentState = PickupState.READY_TO_OPEN
                                    }
                                }
                            }

                            // --- FASE 3: SPORTELLO APERTO ---
                            PickupState.DOOR_OPEN -> {
                                Box(
                                    modifier = Modifier.size(100.dp).background(PowerGreen.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, null, tint = PowerGreen, modifier = Modifier.size(50.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("Sportello Aperto!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PowerGreen)
                                Spacer(Modifier.height(8.dp))
                                Text("Prendi il Power Bank.\nIl noleggio partirà automaticamente.", textAlign = TextAlign.Center, color = TextGray)
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = PowerGreen)
                            }

                            // --- FASE 4: SUCCESSO ---
                            PickupState.ACTIVE -> {
                                Icon(Icons.Default.CheckCircle, null, tint = PowerGreen, modifier = Modifier.size(80.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Noleggio Avviato!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Tasto Chiudi Emergenza
                    if (currentState == PickupState.DOOR_OPEN) {
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = { showDialog = false }) { Text("Chiudi", color = TextGray) }
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
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Restart)
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Restart)
    )
    Box(modifier = Modifier.size(80.dp).background(color, CircleShape))
    Canvas(modifier = Modifier.size(80.dp).scale(scale)) {
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 2)
    }
}