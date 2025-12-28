package com.example.ricarica.home
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ricarica.MapView



@Composable
fun HomePage(viewModel: HomeViewModel) {

    val stations = viewModel.stationList.value

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
            MapView(stations = stations)
        }

        // 2. LA BARRA INFERIORE (1/4 dello schermo)
        BottomBarWithButtons(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // <-- DICE A QUESTA BARRA DI PRENDERE 1 PARTE DELLO SPAZIO
        )

    }
}

@Composable
fun BottomBarWithButtons (modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraSmall // Bordi non arrotondati
    ) {
        // La Row dispone i bottoni orizzontalmente.
        Row(
            modifier = Modifier
                .fillMaxSize() // Occupa tutto lo spazio dato dal weight(1f)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround, // Distribuisce lo spazio equamente
            verticalAlignment = Alignment.CenterVertically // Centra i bottoni verticalmente
        ) {


            // Bottone 1 ==> navigo nella pagina catalogo
            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f) // Ogni bottone occupa la stessa larghezza
                    .height(50.dp)
            ) {

                Spacer(modifier = Modifier.width(8.dp))
                Text("Azione 1", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp)) // Spazio tra i bottoni

            // Bottone 2 ==> navigo nella pagine profilo
            OutlinedButton( // Usiamo un tipo diverso per varietà
                onClick = { },
                modifier = Modifier
                    .weight(1f) // Ogni bottone occupa la stessa larghezza
                    .height(50.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("Azione 2", fontWeight = FontWeight.Bold)
            }
        }
    }
}