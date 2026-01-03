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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ricarica.home.HomePage
import com.example.ricarica.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.*



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

    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var showLoginPopup by remember { mutableStateOf(false) }

    // ascolta cambiameninPti login/logout
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fb ->
            isLoggedIn = fb.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

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
            )
            {

                composable("home") { HomePage(viewModel = viewModel) }
                composable("catalog") { CatalogPage(viewModel = viewModel, navController) }
                composable ("profile" ) { ProfileInfoPage (navController) }

                //AGGIUNGO PAGINA DI LOGIN
                composable ( "login") {}

                //AGGIUNGO PAGINA DI REGISTRAZIONE
                composable("register") {  }
            }
        }

        //POP U

        if (showLoginPopup) {
            AlertDialog(
                onDismissRequest = { showLoginPopup = false },
                title = { Text("Accesso richiesto") },
                text = { Text("Per vedere il profilo devi accedere o registrarti.") },
                confirmButton = {
                    Button(onClick = {
                        showLoginPopup = false
                        navController.navigate("login")
                    }) { Text("Accedi") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showLoginPopup = false
                        navController.navigate("register")
                    }) { Text("Registrati") }
                }
            )
        }





        // 1/4 SCHERMO → BOTTOM BAR PERSISTENTE
        BottomBarWithButtons(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onCatalogClick = {navController.navigate("catalog") },
            onProfileClick = {

                if (isLoggedIn) {

                    navController.navigate("profile")
                } else {
                    showLoginPopup = true } },

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



