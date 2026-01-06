package com.example.ricarica.home
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun BottomBarWithButtons(
    modifier: Modifier = Modifier,
    onCatalogClick: () -> Unit,
    onProfileClick: () -> Unit,
    onQRClick: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center // Centriamo il contenuto
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BOTTONE CATALOGO
                Button(
                    onClick = onCatalogClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    // Usiamo il colore onPrimary per il contrasto (testo bianco su verde)
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent // Resta integrato nella barra
                    )
                ) {
                    Text(
                        text = "Catalogo",
                        style = MaterialTheme.typography.labelSmall // Collega a Type.kt
                    )
                }

                // BOTTONE QRCODE (Il pezzo forte)
                Button(
                    onClick = onQRClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    // Usiamo il 'secondary' (verde chiaro) per evidenziarlo
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ),
                    shape = CircleShape // Lo rendiamo tondo come su Figma
                ) {
                    Text(
                        text = "QRCODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // BOTTONE PROFILO
                Button(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = "Profilo",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
