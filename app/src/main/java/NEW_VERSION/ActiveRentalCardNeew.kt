package com.example.ricarica.rental

import NEW_VERSION.PulsatingEffectTimer
import PowerBank
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
    VERIFYING,   // Controllo se lo sportello si è aperto
    DOOR_OPEN,   // Sportello aperto rilevato -> Inserire PowerBank
    COMPLETED    // Restituzione completata
}

@Composable
fun ModernActiveRentalCard(
    rental: Rental,
    rentalViewModel: RentalViewModel,
    dtmfPlayer: DtmfPlayer
) {
    val scope = rememberCoroutineScope()

    // 1. OSSERVIAMO LO STATO DEL LOCKER DAL VIEWMODEL
    val targetLockerStatus by rentalViewModel.targetLockerStatusFisico.collectAsState()
    val realTimeRentalState by rentalViewModel.returnRentalState.collectAsState()

    // Variabile per memorizzare l'ID del locker assegnato per la restituzione
    var assignedLockerId by remember { mutableStateOf<String?>(null) }

    // Stati UI e Dialog
    var currentState by remember { mutableStateOf(ReturnState.IDLE) }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
        rentalViewModel.watchRentalLifecycle(rental.rentalId, rental.stationId, rental.lockerId)
    }

    // --- LOGICA REATTIVA PER CAMBIO STATO AUTOMATICO ---
    LaunchedEffect(realTimeRentalState, targetLockerStatus) {

        // 1. Se lo stato diventa COMPLETED, successo totale
        if (realTimeRentalState == "COMPLETED") {
            currentState = ReturnState.COMPLETED
            delay(2000) // Mostra messaggio di successo per 2 secondi
            showDialog = false

        }
        // 2. Se siamo nel dialog e il locker diventa fisicamente "OPEN" (aperto da Arduino)
        // Passiamo alla fase di inserimento (DOOR_OPEN)
        else if (showDialog && targetLockerStatus == "OPEN" && currentState != ReturnState.COMPLETED) {
            currentState = ReturnState.DOOR_OPEN
            errorMessage = null // Rimuoviamo eventuali errori precedenti
        }
    }

    // Pulizia quando il dialog si chiude
    DisposableEffect(showDialog) {
        onDispose {
            if (!showDialog && assignedLockerId != null) {
                rentalViewModel.stopWatchingLocker(rental.stationId, assignedLockerId!!)
            }
        }
    }




    // CARD PRINCIPALE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PowerGreen.copy(alpha = 0.1f), CircleShape),
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
            Text("Restituire presso: Stazione ${rental.stationId}", style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            // Info Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightUiBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EnhancedDataBlock(Icons.Default.Timer, formatDuration(durationMillis), "Tempo", PowerOrange, Modifier.weight(1f))
                Divider(color = Color.Gray.copy(0.2f), modifier = Modifier
                    .height(40.dp)
                    .width(1.dp))
                EnhancedDataBlock(Icons.Default.Euro, "%.2f€".format(cost), "Costo", TextBlack, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            //TERMINA NOLEGGIO
            Button(
                onClick = {
                    showDialog = true
                    currentState = ReturnState.PREPARING
                    errorMessage = null

                    rentalViewModel.prepareReturn(
                        rental = rental,

                        onReadyToPlay = { code, lockerId ->
                            returnCode = code
                            assignedLockerId = lockerId
                            currentState = ReturnState.READY_TO_OPEN

                            // MONITORO IL LOCKER SPECIFICO
                            rentalViewModel.watchLockerStatus(rental.stationId, rental.lockerId)
                        },
                        onError = {
                            errorMessage = it
                            currentState = ReturnState.IDLE
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
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
        var isPlaying by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = {
                // Si può chiudere solo se non sta suonando e non sta verificando attivamente
                if (!isPlaying && currentState != ReturnState.VERIFYING && currentState != ReturnState.DOOR_OPEN) {
                    showDialog = false
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
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
                    when (currentState) {

                        //FASE PREAPARAZIONE -> UTENTE SCHIACCIA TERMINE + NOLEGGIO POP SLOT APERTO
                        ReturnState.PREPARING, ReturnState.IDLE -> {
                            CircularProgressIndicator(color = PowerGreen)
                            Spacer(Modifier.height(16.dp))
                            Text("Cerco uno slot libero...", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        ReturnState.VERIFYING -> {
                            // --- FASE 1: ATTESA APERTURA SPORTELLO ---
                            CircularProgressIndicator(color = PowerOrange)
                            Spacer(Modifier.height(16.dp))
                            Text("Ascolto scatto serratura...", fontWeight = FontWeight.Bold, color = PowerGreen)
                            Spacer(Modifier.height(8.dp))
                            Text("Il microfono della stazione sta elaborando il suono.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = TextGray)

                            // *** LOGICA DI TIMEOUT E RIPROVA ***
                            // Se dopo 8 secondi lo stato non è diventato "OPEN" (gestito dal LaunchedEffect globale sopra)
                            // Allora torniamo indietro per far riprovare l'utente.
                            LaunchedEffect(Unit) {
                                delay(8000) // 8 secondi di attesa
                                if (currentState == ReturnState.VERIFYING) {
                                    errorMessage = "Apertura non rilevata. Riprova."
                                    currentState = ReturnState.READY_TO_OPEN // <--- TORNA AL TASTO PLAY
                                }
                            }
                        }

                        ReturnState.DOOR_OPEN -> {
                            // --- FASE 2: SPORTELLO APERTO - INSERIMENTO ---
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(PowerGreen.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, null, tint = PowerGreen, modifier = Modifier.size(50.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Sportello Aperto!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PowerGreen)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Inserisci il Power Bank nello slot e attendi la conferma.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp), color = PowerGreen)
                        }

                        ReturnState.COMPLETED -> {
                            // --- FASE 3: SUCCESSO ---
                            Icon(Icons.Default.CheckCircle, null, tint = PowerGreen, modifier = Modifier.size(80.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Restituzione Completata!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        ReturnState.READY_TO_OPEN -> {
                            // Determiniamo se siamo in stato di errore
                            val isErrorState = errorMessage != null

                            // Colore dinamico: Verde se tutto ok, Arancione se errore
                            val currentStateColor = if (isErrorState) PowerOrange else PowerGreen

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                // --- BLOCCO CENTRALE DINAMICO ---
                                Box(
                                    modifier = Modifier
                                        .size(160.dp) // Box più grande
                                        .background(currentStateColor.copy(alpha = 0.1f), CircleShape), // Sfondo cerchio leggero
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPlaying) {
                                        // ANIMAZIONE SUONO
                                        PulsatingEffectTimer(color = currentStateColor)
                                        Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(40.dp))

                                    } else {
                                        // ICONA STATICA (Cambia se Errore o Normale)
                                        Icon(
                                            imageVector = if (isErrorState) Icons.Default.Warning else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = currentStateColor,
                                            modifier = Modifier.size(60.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                // --- TITOLO ---
                                Text(
                                    text = if (isPlaying) "Invio segnale..."
                                    else if (isErrorState) "Apertura fallita"
                                    else "Slot Pronto",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )

                                Spacer(Modifier.height(8.dp))

                                // --- DESCRIZIONE / ISTRUZIONI ---
                                // Qui spieghiamo bene cosa fare in caso di errore
                                Text(
                                    text = if (isPlaying) "Tieni il telefono vicino allo slot"
                                    else if (isErrorState) "Non abbiamo sentito lo scatto.\nAlza il volume al massimo e riprova."
                                    else "Avvicina il telefono allo slot e premi il pulsante.",
                                    textAlign = TextAlign.Center,
                                    color = TextGray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(Modifier.height(32.dp))

                                // --- PULSANTE ---
                                Button(
                                    onClick = {
                                        if (!isPlaying) {
                                            isPlaying = true
                                            errorMessage = null // Reset errore visivo mentre suona
                                            scope.launch {
                                                val sequence = "#$returnCode*"

                                                // NOTA: Ho corretto la logica di callback qui sotto
                                                dtmfPlayer.playSequence(sequence) {}
                                                    isPlaying = false
                                                    // Logica di controllo immediato
                                                    if (targetLockerStatus == "OPEN") {
                                                        currentState = ReturnState.DOOR_OPEN
                                                    } else {
                                                        currentState = ReturnState.VERIFYING
                                                    }

                                            }
                                        }
                                    },
                                    enabled = !isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PowerGreen, // Il bottone segue il colore dello stato
                                        //disabledContainerColor = Color.Gray
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 2.dp
                                    )
                                ) {
                                    if (isPlaying) {
                                        /*CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        */

                                        //Spacer(Modifier.width(12.dp))
                                        Text("TRASMISSIONE...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        // Icona diversa nel bottone se è un "Riprova"
                                        if (isErrorState) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("RIPROVA APERTURA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("APRI SPORTELLO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tasto Chiudi (disponibile solo se sicuro)
                    if (!isPlaying && currentState != ReturnState.VERIFYING && currentState != ReturnState.DOOR_OPEN && currentState != ReturnState.COMPLETED) {
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = {
                            rentalViewModel.cancelReturnPreparation(rental)
                            showDialog = false
                        }) {
                            Text("Chiudi", color = TextGray)
                        }
                    }
                }
            }
        }
    }
}


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