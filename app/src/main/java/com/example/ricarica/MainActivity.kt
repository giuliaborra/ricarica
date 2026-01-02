package com.example.ricarica
import ProfileInfoPage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ricarica.home.HomePage
import com.example.ricarica.home.HomeViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SomeApp()
        }
    }
}


@Composable
fun SomeApp() {

    val navController = rememberNavController()
    val viewModel: HomeViewModel = viewModel()

    Column(modifier = Modifier.fillMaxSize()) {

        // 3/4 SCHERMO → CONTENUTO (HOME / CATALOGO)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(4f)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {

                composable("home") {
                    HomePage(viewModel = viewModel)
                }

                composable("catalog") {
                    CatalogPage(viewModel = viewModel, navController)
                }

                composable ("profile" ) {
                    ProfileInfoPage (navController)
                }
            }
        }

        // 1/4 SCHERMO → BOTTOM BAR PERSISTENTE
        BottomBarWithButtons(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onCatalogClick = {navController.navigate("catalog") },
            onProfileClick = {navController.navigate("profile") },

            //GESTIONE LETTURA DEL QR CODE
            onQRClick = {}
        )
    }
}
@Composable
fun BottomBarWithButtons(
    modifier: Modifier = Modifier,
    onCatalogClick: () -> Unit,
    onProfileClick: () -> Unit,
    onQRClick: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Pulsanti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Button(
                    onClick = onCatalogClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Catalogo")
                }

                OutlinedButton(
                    onClick = onQRClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("QRCODE")
                }



                OutlinedButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Profilo")
                }
            }
        }
    }
}



