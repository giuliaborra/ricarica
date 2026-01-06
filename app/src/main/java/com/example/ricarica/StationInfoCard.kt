package com.example.ricarica

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.ricarica.data.model.Station
import com.example.ricarica.data.model.StationItem

class StationViewModel : ViewModel() {
}


@Composable
fun StationInfoCard(
    station: StationItem
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(text = "Stazione: ${station.station.name}")
            Text(text = "Funzionante: ${station.station.enabled}")

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Locker:")

            val lockers = station.station.lockers

            if (lockers.isEmpty()) {
                Text("Nessun locker presente")
            } else {
                lockers.forEach { (lockerId, locker) ->

                    if (locker.powerBankId!=null) {
                        Text(
                            text = "Locker $lockerId → ${locker.powerBankId}"
                        )
                    }
                }
            }
        }
    }
}
