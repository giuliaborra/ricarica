package com.example.ricarica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    val thisNavController = rememberNavController()
    val viewModel: HomeViewModel = viewModel()

    NavHost(navController = thisNavController, startDestination = "home") {
        composable( "home") {
            HomePage (
                viewModel,
                onCatalogClick = {thisNavController.navigate("catalog")}
            )
        }

        composable ( "catalog")  {
                CatalogPage (
                    viewModel,
                    onBack = {thisNavController.navigate(route = "home")}

            )

        }

    }
}
