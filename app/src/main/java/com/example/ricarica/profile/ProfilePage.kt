import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ricarica.profile.AuthState
import com.example.ricarica.profile.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoPage(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val authState by authViewModel.authState.collectAsState()
    val profile by authViewModel.userProfile.collectAsState()

    // Se l’utente non è loggato, non ha senso stare qui
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedOut) {
            navController.navigate("home") {
                popUpTo("profile") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }

                is AuthState.LoggedOut -> {
                    // Questo caso dura pochissimo perché LaunchedEffect naviga a login
                    Text("Non sei loggato")
                }

                is AuthState.LoggedIn -> {
                    if (profile == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Caricamento profilo...")
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Ciao ${profile!!.userName}")
                            Text("Email: ${profile!!.email}")


                            Button(onClick = {
                                authViewModel.signOut()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }) {
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        }
    }
}
