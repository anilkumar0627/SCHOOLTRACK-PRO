package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.utils.AdMobManager

sealed class Screen {
    object RegisterSchool : Screen()
    object Login : Screen()
    object PrincipalDashboard : Screen()
    object ParentDashboard : Screen()
    object DriverDashboard : Screen()
    object SuperAdminDashboard : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize User Messaging Platform (UMP) Consent and Google AdMob SDK
        AdMobManager.initializeConsentAndAds(this)

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val currentSchool by appViewModel.currentSchool.collectAsState()
            MyApplicationTheme(school = currentSchool) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                val schools by appViewModel.schools.collectAsState()
                val currentUser by appViewModel.currentUser.collectAsState()

                // Intercept state changes to automatically guide registration / login
                LaunchedEffect(schools) {
                    if (schools.isEmpty()) {
                        // Allow bypass/fallback for Super Admin login even if database is empty
                    }
                }

                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        currentScreen = when (currentUser!!.role) {
                            "SUPER_ADMIN" -> Screen.SuperAdminDashboard
                            "PRINCIPAL" -> Screen.PrincipalDashboard
                            "PARENT" -> Screen.ParentDashboard
                            "DRIVER" -> Screen.DriverDashboard
                            else -> Screen.Login
                        }
                    } else {
                        if (schools.isEmpty()) {
                            // If schools is empty, show Register, but allow navigate to login for Super Admin prepop
                            currentScreen = Screen.RegisterSchool
                        } else {
                            currentScreen = Screen.Login
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        is Screen.RegisterSchool -> RegisterSchoolScreen(
                            viewModel = appViewModel,
                            onNavigateToLogin = { currentScreen = Screen.Login }
                        )
                        is Screen.Login -> LoginScreen(
                            viewModel = appViewModel,
                            onNavigateToRegisterSchool = { currentScreen = Screen.RegisterSchool },
                            onLoginSuccess = { /* Routed by LaunchedEffect */ }
                        )
                        is Screen.PrincipalDashboard -> PrincipalDashboardScreen(
                            viewModel = appViewModel,
                            onLogout = {
                                appViewModel.logout()
                                currentScreen = Screen.Login
                            }
                        )
                        is Screen.ParentDashboard -> ParentDashboardScreen(
                            viewModel = appViewModel,
                            onLogout = {
                                appViewModel.logout()
                                currentScreen = Screen.Login
                            }
                        )
                        is Screen.DriverDashboard -> DriverDashboardScreen(
                            viewModel = appViewModel,
                            onLogout = {
                                appViewModel.logout()
                                currentScreen = Screen.Login
                            }
                        )
                        is Screen.SuperAdminDashboard -> SuperAdminDashboardScreen(
                            viewModel = appViewModel,
                            onLogout = {
                                appViewModel.logout()
                                currentScreen = Screen.Login
                            }
                        )
                    }
                }
            }
        }
    }
}
