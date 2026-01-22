package com.example.ricarica.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ricarica.rental.Rental
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun RentalBottomBar(
    rental: Rental,
    onTimerExpired: () -> Unit
) {
    // Calcola il tempo rimanente partendo da endTime del database
    var timeLeft by remember { mutableLongStateOf(rental.endTime - System.currentTimeMillis()) }

    // Aggiorna il timer ogni secondo
    LaunchedEffect(rental) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft = rental.endTime - System.currentTimeMillis()
        }
        onTimerExpired() // Quando arriva a zero
    }

    // Formattazione minuti:secondi
    val formattedTime = remember(timeLeft) {
        val min = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
        val sec = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
        String.format("%02d:%02d", min, sec)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Altezza barra
            .background(Color(0xFF4CAF50)) // Sfondo VERDE
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Codice Sblocco:", color = Color.White, fontSize = 12.sp)
            Text(
                text = rental.passkey ?: "---",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = formattedTime,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}