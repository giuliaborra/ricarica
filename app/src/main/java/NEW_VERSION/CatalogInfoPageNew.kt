package com.example.ricarica
import PowerBank
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ricarica.home.HomeViewModel

// --- COLORI ---
private val GreenPrimary = Color(0xFF2E7D32)
private val GreenBackgroundLight = Color(0xFFE8F5E9)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF616161)
private val BackgroundColor = Color(0xFFF5F5F5) // Grigio chiaro per far risaltare le card bianche

@Composable
fun CatalogPageNew(
    viewModel: HomeViewModel,
    navController: NavController
) {
    val typeOfPowerBank: List<PowerBank> = listOf(
        PowerBank.BASIC,
        PowerBank.FAST,
        PowerBank.PRO
    )

    Scaffold(
        topBar = {
            CleanCatalogTopBar {  }
        },
        containerColor = BackgroundColor // Sfondo grigio chiaro
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp), // Spazio laterale
            verticalArrangement = Arrangement.spacedBy(16.dp), // Spazio tra le card
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp) // Padding extra in fondo
        ) {
            items(typeOfPowerBank) { pb ->
                PowerBankCard(pb)
            }
        }
    }
}

// --- CARD RIALZATA CON TUTTE LE INFO ---
@Composable
fun PowerBankCard(pb: PowerBank) {
    val icon = getIconForPowerBank(pb)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // Bordi arrotondati
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // OMBRA/RIALZO
        colors = CardDefaults.cardColors(containerColor = Color.White) // Card Bianca
    ) {
        Column {
            // --- HEADER: Icona, Titolo e Prezzo Minuto ---
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icona Tonda
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(GreenBackgroundLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Titolo e Prezzo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pb.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Text(
                        text = "Tariffa al consumo",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                // Prezzo Grande
                Text(
                    text = "€${pb.pricePerMinute}/min",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // --- FEATURES (Lista puntata) ---
            Column(modifier = Modifier.padding(16.dp)) {
                pb.features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }
                }
            }

            // --- FOOTER: Cauzione e Max Giornaliero ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA)) // Grigio chiarissimo
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(horizontalAlignment = Alignment.Start) {

                }



                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Max Giornaliero", fontSize = 12.sp, color = TextGray)
                    Text(
                        text = "€${String.format("%.2f", pb.maxDailyPrice)}",
                        fontWeight = FontWeight.SemiBold,
                        color = TextBlack
                    )
                }
            }
        }
    }
}


//TOP BAR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanCatalogTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "SCEGLI IL TUO POWERBANK ", // Titolo più accattivante
                color = TextBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundColor,
            scrolledContainerColor = BackgroundColor
        )
    )
}

// --- HELPER ICONE ---
private fun getIconForPowerBank(pb: PowerBank): ImageVector {
    return when (pb) {
        PowerBank.BASIC -> Icons.Outlined.BatteryStd
        PowerBank.FAST -> Icons.Outlined.Speed
        PowerBank.PRO -> Icons.Outlined.Bolt
        else -> Icons.Outlined.BatteryStd
    }
}