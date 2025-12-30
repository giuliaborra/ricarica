package com.example.ricarica

import PowerBank
import android.R.attr.navigationIcon
import android.health.connect.datatypes.units.Power
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ricarica.home.HomeViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
@Composable
fun CatalogPage(
    viewModel: HomeViewModel,
    navController: NavController
) {

    val typeOfPowerBank: List<PowerBank> = listOf(

        PowerBank.Basic,
        PowerBank.Fast,
        PowerBank.Pro

    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3EAD2))
    ) {

        CatalogTopBar(
            onBack = { navController.popBackStack("home", false) }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(typeOfPowerBank) { pb ->
                InfoCardPB(powerBank = pb)
            }
        }
    }
}

@Composable
fun CatalogTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color(0xFF6F9752))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- SEZIONE CORRETTA PER L'ICONA INDIETRO ---
        // Chiama IconButton direttamente qui. È un Composable, proprio come Text o Box.
        // Non serve una Column che lo contenga se è l'unico elemento.
        IconButton(onClick = onBack) { // Ho corretto anche {onBack} in onBack
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Torna indietro",
                tint = Color.White // Aggiunto per rendere l'icona visibile sullo sfondo scuro
            )
        }

        // --- Titolo centrato ---

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CATALOGO",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(48.dp))
    }
}
