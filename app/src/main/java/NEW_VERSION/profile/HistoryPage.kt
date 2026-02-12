package com.example.ricarica.ui.pages // O il tuo package corretto

import PowerBank
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ricarica.profile.AuthViewModel
import com.example.ricarica.rental.Rental
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.ui.draw.shadow



// --- COLORI STYLE ---
private val PowerGreen = Color(0xFF2E7D32)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val BackgroundColor = Color(0xFFF5F5F5)
private val LightUiBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    // 1. Recuperiamo tutti i noleggi
    val allRentals by authViewModel.rentalHistory.collectAsState()

    // 2. Filtriamo solo i COMPLETED e ordiniamo per data (dal più recente)
    val historyRentals = remember(allRentals) {
        allRentals
            .filter { it.state == "COMPLETED" }
            .sortedByDescending { it.startTime }
    }

    // 3. Calcolo statistiche rapide
    val totalSpent = remember(historyRentals) {
        historyRentals.sumOf { calculateFinalCost(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Torna indietro",
                            tint = TextBlack
                        )
                    }
                },
                title = {
                    Text(
                        text = "CRONOLOGIA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },

    ) { paddingValues ->

        if (historyRentals.isEmpty()) {
            EmptyHistoryView(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ITEM 1: Riepilogo Statistiche (Opzionale ma carino)
                item {
                    HistorySummaryCard(
                        count = historyRentals.size,
                        totalSpent = totalSpent
                    )
                }

                // ITEM 2...N: Lista Noleggi
                items(historyRentals) { rental ->
                    HistoryRentalItem(rental = rental)
                }
            }
        }
    }
}

// --- CARD SINGOLA STORICO ---
@Composable
fun HistoryRentalItem(rental: Rental) {
    val durationMillis = (rental.endTime ?: System.currentTimeMillis()) - (rental.startTime ?: 0L)
    val finalCost = calculateFinalCost(rental)
    val dateString = formatDate(rental.startTime ?: 0L)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // RIGA 1: Data e Tipo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LightUiBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            null,
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PowerBank ${rental.type}",
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            fontSize = 16.sp
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }

                // Badge "Completato" (o check verde)
                Text(
                    text = "COMPLETATO",
                    style = MaterialTheme.typography.labelSmall,
                    color = PowerGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(PowerGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(16.dp))

            // RIGA 2: Dati Finali
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Durata
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatHistoryDuration(durationMillis),
                        fontWeight = FontWeight.SemiBold,
                        color = TextBlack
                    )
                }

                // Costo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Euro, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.2f€".format(finalCost),
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

// --- CARD RIEPILOGO (Header) ---
@Composable
fun HistorySummaryCard(count: Int, totalSpent: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PowerGreen),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Totale Speso",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "%.2f€".format(totalSpent),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Un separatore verticale o icona
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Noleggi",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$count",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- VISTA VUOTA ---
@Composable
fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0xFFEEEEEE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Nessuno storico",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "I tuoi noleggi terminati appariranno qui.",
            color = TextGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// --- UTILITIES E CALCOLI ---

// Ricalcola il costo se non è salvato nel DB (Safety check)
fun calculateFinalCost(rental: Rental): Double {
    // Se hai un campo rental.totalCost usalo, altrimenti calcolalo:
    if (rental.totalCost != null) return rental.totalCost!!

    val start = rental.startTime ?: return 0.0
    val end = rental.endTime ?: System.currentTimeMillis()
    val minutes = TimeUnit.MILLISECONDS.toMinutes(end - start)

    val pricePerMinute = when (rental.powerBankTypes?.keys?.firstOrNull() ?: "BASIC") {
        "FAST" -> PowerBank.FAST.pricePerMinute
        "PRO" -> PowerBank.PRO.pricePerMinute
        else -> PowerBank.BASIC.pricePerMinute
    }
    return minutes * pricePerMinute
}

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALY)
    return formatter.format(Date(millis))
}

fun formatHistoryDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    // Se dura meno di un minuto, mostra secondi, altrimenti minuti
    return if (minutes < 1) {
        "< 1 min"
    } else {
        "$minutes min"
    }
}

// Helper per ombra custom (se non usi quella standard)
fun Modifier.shadow(elevation: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.drawBehind {
        drawRect(
            color = Color.Black.copy(alpha = 0.05f),
            size = size
        )
    }
)