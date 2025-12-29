package com.example.ricarica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ricarica.home.HomeViewModel

@Composable
fun CatalogPage(
    viewModel: HomeViewModel,
    thisnavController: NavController

) {
    // Temporaneo: lista finta
    val powerbanks = listOf(
        "RicaRica BASIC",
        "RicaRica FAST",
        "RicaRica PRO"
    )

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        TextButton(
            onClick = { thisnavController.popBackStack("home", false) }
        ) {
            Text("Indietro")
        }
        Spacer(Modifier.height(8.dp))
        Text("Catalogo PowerBank")

        Spacer(Modifier.height(16.dp))
        powerbanks.forEach { pb ->
            Text(pb)
            Spacer(Modifier.height(8.dp))
        }
    }
}
