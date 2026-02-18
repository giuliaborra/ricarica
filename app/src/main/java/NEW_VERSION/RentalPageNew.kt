package com.example.ricarica.ui.pages
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ricarica.DtmfPlayer
import NEW_VERSION.profile.AuthViewModel
import com.example.ricarica.rental.ModernActiveRentalCard
import com.example.ricarica.rental.RentalViewModel

// --- PALETTE COLORI ---
private val PowerGreen = Color(0xFF2E7D32)
private val BackgroundColor = Color(0xFFF5F5F5)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextBlack = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsPage(
    authViewModel: AuthViewModel,
    rentalViewModel: RentalViewModel,
    navController: NavController
) {
    val allRentals by authViewModel.activeRentals.collectAsState()

    // Filtriamo solo quelli ATTIVI
    val activeOnlyRentals = remember(allRentals) {
        allRentals.filter { it.state == "ACTIVE" }
    }

    // Creiamo il player audio una volta sola
    val dtmfPlayer = remember { DtmfPlayer() }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "I Miei Noleggi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        if (activeOnlyRentals.isNotEmpty()) {
                            Text(
                                text = "${activeOnlyRentals.size} in corso",
                                style = MaterialTheme.typography.labelMedium,
                                color = PowerGreen
                            )
                        }
                    }
                },
                /*
                actions = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.History, contentDescription = "Storico", tint = TextBlack)
                    }
                },
                */

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SurfaceWhite),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { paddingValues ->

        if (activeOnlyRentals.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                navController = navController
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeOnlyRentals) { rental ->
                    // --- QUI LA NUOVA CARD CON IL SUONO ---
                    ModernActiveRentalCard(
                        rental = rental,
                        rentalViewModel = rentalViewModel,
                        dtmfPlayer = dtmfPlayer
                    )
                }
            }
        }
    }
}

// --- EmptyStateView (Stessa di prima) ---
@Composable
fun EmptyStateView(modifier: Modifier = Modifier, navController: NavController) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0xFFEEEEEE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ElectricBolt, // O un'icona vuota
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Nessun noleggio attivo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Non hai PowerBank con te al momento.\nCerca una stazione sulla mappa per iniziare.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("home") },
            colors = ButtonDefaults.buttonColors(containerColor = PowerGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(50.dp)
        ) {
            Text(
                text = "TORNA ALLA MAPPA",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

