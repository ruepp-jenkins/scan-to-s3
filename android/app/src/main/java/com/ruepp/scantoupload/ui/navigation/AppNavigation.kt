package com.ruepp.scantoupload.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruepp.scantoupload.data.preferences.ServerConfig
import com.ruepp.scantoupload.ui.screens.SetupScreen
import com.ruepp.scantoupload.ui.screens.UploadScreen
import com.ruepp.scantoupload.viewmodel.SetupViewModel
import com.ruepp.scantoupload.viewmodel.UploadViewModel

@Composable
fun AppNavigation(
    serverConfig: ServerConfig,
    sharedUris: List<Uri>
) {
    val navController = rememberNavController()

    val startDestination = if (serverConfig.isConfigured()) "upload" else "setup"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") {
            val viewModel: SetupViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SetupViewModel(serverConfig) as T
                    }
                }
            )
            SetupScreen(
                viewModel = viewModel,
                onSetupSuccess = {
                    navController.navigate("upload") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("upload") {
            val viewModel: UploadViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return UploadViewModel(serverConfig) as T
                    }
                }
            )
            UploadScreen(
                viewModel = viewModel,
                sharedUris = sharedUris,
                onDisconnect = {
                    navController.navigate("setup") {
                        popUpTo("upload") { inclusive = true }
                    }
                }
            )
        }
    }
}
