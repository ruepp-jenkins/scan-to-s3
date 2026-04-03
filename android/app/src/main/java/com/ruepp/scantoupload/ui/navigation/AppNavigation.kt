package com.ruepp.scantoupload.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruepp.scantoupload.data.preferences.ServerConfig
import com.ruepp.scantoupload.data.preferences.TokenManager
import com.ruepp.scantoupload.ui.screens.LoginScreen
import com.ruepp.scantoupload.ui.screens.UploadScreen
import com.ruepp.scantoupload.viewmodel.LoginViewModel
import com.ruepp.scantoupload.viewmodel.UploadViewModel

@Composable
fun AppNavigation(
    tokenManager: TokenManager,
    serverConfig: ServerConfig,
    sharedUris: List<Uri>
) {
    val navController = rememberNavController()

    val startDestination = if (tokenManager.isTokenValid()) "upload" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            val viewModel = remember { LoginViewModel(tokenManager, serverConfig) }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("upload") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("upload") {
            val viewModel = remember { UploadViewModel(tokenManager, serverConfig) }
            UploadScreen(
                viewModel = viewModel,
                sharedUris = sharedUris,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("upload") { inclusive = true }
                    }
                }
            )
        }
    }
}
