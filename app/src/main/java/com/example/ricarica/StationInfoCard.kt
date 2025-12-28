package com.example.ricarica

import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ricarica.data.model.Station
import com.example.ricarica.data.model.StationItem


@Composable
fun StationInfoCard (
    station: StationItem

) {
    Card {

        // 1. Il nome della stazione
        Text(
            text = "Nome: ${station.station.name}"
        )

        // 2. STATO FUNZIONANTE O MENO
        Text(
            text = "funzionante: ${station.station.enabled}")
        Text("infomazioni della stazione")

    }
}