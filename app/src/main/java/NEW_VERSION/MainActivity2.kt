package com.example.ricarica
import NEW_VERSION.BottomNavBar
import NEW_VERSION.HomePageNew
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ricarica.auth.ModernLoginPage
import com.example.ricarica.auth.ModernRegisterPage
import com.example.ricarica.home.HomeViewModel
import com.example.ricarica.profile.AuthViewModel
import com.example.ricarica.profile.ModernProfilePage
import com.example.ricarica.rental.RentalViewModel
import com.example.ricarica.ui.pages.HistoryPage
import com.example.ricarica.ui.pages.RentalsPage
import com.example.ricarica.ui.theme.RicaricaTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RicaricaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SomeApp()
                }
            }
        }
    }
}

@Composable
fun SomeApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val rentalViewModel: RentalViewModel = viewModel()

    val auth = remember { FirebaseAuth.getInstance() }

    // 1. Controllo immediato all'avvio: Se c'è un utente, è true.
    val isUserInitiallyLoggedIn = remember { auth.currentUser != null }
    val isLoggedIn = remember { mutableStateOf(isUserInitiallyLoggedIn) }

    val isGuestMode = rememberSaveable { mutableStateOf(false) }

    // Listener per aggiornare lo stato se cambia qualcosa (es. logout dal profilo)
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fb ->
            val hasUser = fb.currentUser != null
            isLoggedIn.value = hasUser

            //se l'utente fa login
            if (hasUser) {
                isGuestMode.value = false
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // 2. Logica di Reindirizzamento Automatico (Sicurezza)
    //-
    LaunchedEffect(isLoggedIn.value, isGuestMode.value) {
        val isAuthScreen = navController.currentDestination?.route in listOf("login", "register")

        // Condizione per cacciare l'utente: Non loggato E Non ospite
        if (!isLoggedIn.value && !isGuestMode.value) {
            if (!isAuthScreen) {
                navController.navigate("register") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // 3. Definiamo la rotta di partenza basandoci sullo stato iniziale
    val startRoute = if (isUserInitiallyLoggedIn) "home" else "login"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Mostra la barra solo nelle pagine principali, MAI in login/register
    val showBottomBar = currentRoute in listOf("home", "catalog", "profile", "rentals_list")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("home") {
                HomePageNew(homeViewModel, navController, isGuest = isGuestMode.value)
            }

            composable("catalog") {
                CatalogPageNew(homeViewModel, navController)
            }

            composable("profile") {
                ModernProfilePage(navController, authViewModel, onLogoutClick = { authViewModel.signOut() })
            }

            composable("rentals_list") {
                RentalsPage(authViewModel, rentalViewModel, navController)
            }

            composable("rental_history") {
                HistoryPage(authViewModel)
            }

            // --- AREA AUTENTICAZIONE ---

            composable("login") {
                ModernLoginPage(
                    authViewModel = authViewModel,
                    onSuccess = {
                        // Login riuscito -> Vai alla Home e pulisci login dallo storico
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate("register") },
                    onGuestClick = {
                        isGuestMode.value = true // Attiva modalità ospite
                        navController.navigate("home") {
                            // Pulisci lo stack così back non torna al login
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("register") {
                ModernRegisterPage(
                    authViewModel = authViewModel,
                    onSuccess = {
                        // Registrazione riuscita -> Vai alla Home
                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onBack = {
                        // Clicca su "Accedi" (o torna indietro) -> Vai a Login
                        navController.navigate("login")
                    }
                )
            }
        }
    }
}

