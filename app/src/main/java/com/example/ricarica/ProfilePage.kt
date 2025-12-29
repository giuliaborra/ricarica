import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Pagina profilo minimale per testare la navigazione.
 * Contiene solo un titolo e il pulsante per tornare indietro.
 *
 * @param onNavigateBack Funzione da chiamare per eseguire la navigazione all'indietro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoPage(
    navController: NavController
) {
    // Scaffold fornisce la struttura base e la TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo") },
                // L'icona di navigazione che useremo per tornare indietro
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Box per centrare il contenuto sia verticalmente che orizzontalmente
        Box(
            modifier = Modifier
                .padding(innerPadding) // Applica il padding per non sovrapporsi alla TopAppBar
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centra il contenuto del Box
        ) {
            Text("pagina del profilo")
        }
    }
}


