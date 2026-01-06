package com.example.ricarica.home
import MapViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ricarica.map.MapView


@Composable
fun HomePage(
    viewModel: HomeViewModel,
    navController: NavController,
    isLoggedIn: Boolean
) {
    val stations = viewModel.stationList.value
    val mapVm: MapViewModel = viewModel()

    var showLoginPopup by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(5f)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            MapView(vm = mapVm, stations = stations)
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


