package com.example.ricarica.home

/*

@Composable
fun HomePage(
    viewModel: HomeViewModel,
    navController: NavController,
    isLoggedIn: Boolean
) {

    val stations = viewModel.stationList.value
    var showLoginPopup by remember { mutableStateOf(false) }

    //REF VIEW MODEL
    val mapVm: MapViewModel = viewModel()
    val rentalVm: RentalViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()




    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(5f)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            MapScreen(
                vm = mapVm,
                stations = stations,
                rentals = authVm.activeRentals.collectAsState().value,
                onConfirmRental = { rentalToConfirm ->
                    rentalVm.confirmPickup(rentalToConfirm)
                },
                onDeleteReserved = { rentalToDelete ->
                    rentalVm.deleteReserved(rentalToDelete)
                },
                onTerminateRental = {rentalToReturn ->
                    rentalVm.terminateRental(rentalToReturn,
                        onError = {error("errore nella restituzione")},
                        onSuccess = {})}
            )
        }

        BottomBarWithButtons(
          modifier = Modifier
            .fillMaxWidth()
                .weight(1f),
            onCatalogClick = { navController.navigate("catalog") },
            onProfileClick = {
                if (isLoggedIn) {
                    navController.navigate("profile")
                } else {
                    showLoginPopup = true
                }
            },
            onQRClick = {}
        )
    }

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
}



*/