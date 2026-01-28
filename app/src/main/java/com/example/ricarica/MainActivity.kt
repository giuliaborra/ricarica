package com.example.ricarica
import LoginPage
import ProfileInfoPage
import RegisterPage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ricarica.home.HomePage
import com.example.ricarica.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.*
import com.example.ricarica.profile.AuthViewModel
import com.example.ricarica.ui.theme.RicaricaTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RicaricaTheme {
                SomeApp()
            }
        }
    }
}


@Composable
fun SomeApp() {

    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val authViewModel : AuthViewModel = viewModel ()

    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }



    //ascolta cambiamenti login/logout

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fb ->
            isLoggedIn = fb.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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

                composable("home") { HomePage(viewModel = homeViewModel, navController, isLoggedIn) }
                composable("catalog") { CatalogPage(viewModel = homeViewModel, navController) }
                composable ("profile" ) { ProfileInfoPage (authViewModel, navController) }

                //Pagina per effettuare il login
                composable ( "login") {
                    LoginPage(
                        authViewModel,
                        {navController.navigate("profile")},
                        {navController.navigate("home")}

                    )
                }

                //pagina per registrare un utente
                composable("register") {
                    RegisterPage(
                        authViewModel,
                        {navController.navigate("profile")},
                        {navController.navigate("home")}

                    )
                }
            }
        }
    }
}




