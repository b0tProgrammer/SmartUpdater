package com.mohith.smartUpdater

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohith.smartUpdater.ui.theme.SmartUpdaterTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartUpdaterTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "chooseMedia") {
                    composable("chooseMedia") {
                        ChooseMedia(navController)
                    }
                    composable("options") {
                        OptionsScreen(navController)
                    }
                    composable("addCase") {
                        AddCase(navController)
                    }
                    composable(route = "removeCase") {
                        RemoveCase(navController)
                    }
                    composable(route = "modifyCase") {
                        ModifyCase(navController)
                    }
                }
            }
        }
    }
}

