package com.example.ricarica.home
import MapViewModel
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ricarica.map.MapView



@Composable
fun HomePage(
    viewModel: HomeViewModel,

) {

    val stations = viewModel.stationList.value
    val mapVm: MapViewModel = viewModel()

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. AREA DELLA MAPPA (3/4 dello schermo)
        // Usiamo un Box come segnaposto per la tua mappa (es. Google Maps, OpenStreetMap).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f) // <-- DICE A QUESTO BOX DI PRENDERE 3 PARTI DELLO SPAZIO
                .background(Color.LightGray), // Colore di sfondo per renderlo visibile
            contentAlignment = Alignment.Center
        ) {
            MapView(
                vm = mapVm,
                stations = stations)
        }

        // 2. LA BARRA INFERIORE (1/4 dello schermo)
        /*
        BottomBarWithButtons(
            onCatalogClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // <-- DICE A QUESTA BARRA DI PRENDERE 1 PARTE DELLO SPAZIO
        )*/

    }
}

