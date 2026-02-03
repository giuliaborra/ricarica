import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ricarica.rental.Rental



@Composable
fun MultiRentalTimerCard(
    rentals: List<Rental>, // Lista di prenotazioni
    onConfirm: (Rental) -> Unit, // Callback per confermare il SINGOLO ritiro
    onCancel: (Rental) -> Unit, // Callback per cancellare il SINGOLO ordine
    isExpanded: Boolean
) {
    // Prendiamo il primo per calcolare il timer (scadono quasi insieme)
    val referenceRental = rentals.firstOrNull() ?: return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // TITOLO E TIMER (Comuni a tutti)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prenotazione (${rentals.size} oggetti)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Qui metti il tuo countdown logic usando referenceRental.endTime
                Text(
                    text = "Scade in: 19:50", // Tuo countdown reale qui
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LISTA SCROLLABILE DEI CODICI
            // Se sono tanti, usa LazyColumn, ma per 2-3 va bene Column + scroll
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                rentals.forEach { rental ->
                    RentalItemRow(
                        rental = rental,
                        onConfirm = { onConfirm(rental) },
                        onCancel = { onCancel(rental) }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun RentalItemRow(
    rental: Rental,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // INFO SINISTRA: Tipo e Codice
        Column {
            Text(
                text = rental.type ?: "PowerBank", // Campo aggiunto prima
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Codice Locker: ${rental.unlock_code}",
                style = MaterialTheme.typography.headlineSmall, // Bello grande
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // BOTTONI DESTRA: Ritira o Cancella
        Row {
            // Tasto Cancella (icona o testo piccolo)
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Annulla", tint = Color.Gray)
            }

            // Tasto Ritira
            Button(
                onClick = {

                    onConfirm },
                modifier = Modifier.padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Ritira")
            }
        }
    }
}