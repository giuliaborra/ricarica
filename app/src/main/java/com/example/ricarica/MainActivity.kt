package com.example.ricarica
//import com.example.ricarica.home.HomePage
/*

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
                composable ("profile" ) { ProfilePage (authViewModel, navController) }

                //Pagina per effettuare il login
                composable ( "login") {
                    LoginPage(
                        authViewModel,
                        {navController.navigate("home")},
                        {navController.navigate("register")}

                    )
                }

                //pagina per registrare un utente
                composable("register") {
                    RegisterPage(
                        authViewModel,
                        {navController.navigate("profile")},
                        {navController.navigate("login")}

                    )
                }
            }
        }
    }
}
*/


