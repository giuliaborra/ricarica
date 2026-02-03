package com.example.ricarica.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ricarica.rental.Rental
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfilePage(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val rentalHistory by viewModel.rentalHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- NUOVO: TOP BAR CON FRECCIA INDIETRO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Torna alla Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Il tuo Profilo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        // -------------------------------------------

        // HEADER UTENTE
        ProfileHeader(
            name = userProfile?.userName ?: "Utente",
            email = userProfile?.email ?: "Nessuna email",
            onLogout = {
                viewModel.signOut()
                // Torna alla schermata di login o resetta la navigazione
                navController.navigate("login") {
                    popUpTo(0) // Pulisce tutto lo stack
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // TITOLO SEZIONE STORICO
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Storico Noleggi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // LISTA NOLEGGI
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (rentalHistory.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ShoppingBag, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nessun noleggio trovato", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rentalHistory) { rental ->
                        RentalItemCard(rental)
                    }
                }
            }
        }
    }
}

@Composable
fun RentalItemCard(rental: Rental) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = try { dateFormat.format(Date(rental.startTime)) } catch (e: Exception) { "?" }

    val (statusText, statusColor, containerColor) = when (rental.state) {
        "ACTIVE" -> Triple("In Uso", Color(0xFF4CAF50), Color(0xFFE8F5E9))
        "COMPLETED" -> Triple("Completato", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant)
        "RESERVED" -> Triple("Prenotato", Color(0xFF2196F3), Color(0xFFE3F2FD))
        "CANCELLED" -> Triple("Cancellato", Color.Gray, Color(0xFFF5F5F5))
        else -> Triple(rental.state, Color.Gray, Color.White)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Noleggio #${rental.rentalId.takeLast(5).uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))

                val types = rental.powerBankTypes.keys.joinToString(", ")
                Text(
                    text = "Device: $types",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }

            Surface(
                color = containerColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(name: String, email: String, onLogout: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(email, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}