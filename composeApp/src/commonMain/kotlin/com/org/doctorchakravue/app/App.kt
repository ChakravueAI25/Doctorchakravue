package com.org.doctorchakravue.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.data.PendingCallData
import com.org.doctorchakravue.data.SessionManager
import kotlinx.coroutines.launch
import com.org.doctorchakravue.model.AdherencePatient
import com.org.doctorchakravue.model.Submission
import com.org.doctorchakravue.platform.PlatformVideoCallScreen
import com.org.doctorchakravue.ui.*
import com.org.doctorchakravue.ui.theme.BottomNavBar
import com.org.doctorchakravue.ui.theme.AppTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        val navController = rememberNavController()
        val repository = remember { ApiRepository() }
        val sessionManager = remember { SessionManager() }
        val scope = rememberCoroutineScope()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")

        val startDestination = remember {
            when {
                !repository.isLoggedIn() -> "login"
                !sessionManager.hasAcceptedTerms(repository.getDoctorId(), LegalConfig.TERMS_VERSION) -> "terms"
                else -> "dashboard"
            }
        }

        val showBottomNav = Navigator.shouldShowBottomNav(currentRoute)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(
                        currentScreen = currentRoute ?: "dashboard",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (showBottomNav) padding else PaddingValues()),
                enterTransition = NavigationAnimations.enterTransition,
                exitTransition = NavigationAnimations.exitTransition,
                popEnterTransition = NavigationAnimations.popEnterTransition,
                popExitTransition = NavigationAnimations.popExitTransition
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            val dest = if (sessionManager.hasAcceptedTerms(repository.getDoctorId(), LegalConfig.TERMS_VERSION))
                                "dashboard" else "terms"
                            navController.navigate(dest) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("terms") {
                    TermsScreen(
                        onAccept = {
                            val id = repository.getDoctorId()
                            sessionManager.setTermsAccepted(id, LegalConfig.TERMS_VERSION)
                            scope.launch { repository.recordConsent(id, LegalConfig.TERMS_VERSION) }
                            navController.navigate("dashboard") {
                                popUpTo("terms") { inclusive = true }
                            }
                        }
                    )
                }

                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToSubmissionDetail = { submission ->
                            val json = Json.encodeToString(submission)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("submission/$encodedJson")
                        },
                        onNavigateToAdherence = { patient ->
                            val json = Json.encodeToString(patient)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("adherence_detail/$encodedJson")
                        },
                        onNavigateToPainScaleHistory = { navController.navigate("pain_scale_history") },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToVideoCallList = { navController.navigate("video_call_list") },
                        onNavigateToSlitLamp = { navController.navigate("slit_lamp") }
                    )
                }

                composable("patients") {
                    PatientsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDashboard = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("pain_scale_history") {
                    PainScaleHistoryScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToSubmissionDetail = { submission ->
                            val json = Json.encodeToString(submission)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("submission/$encodedJson")
                        }
                    )
                }

                composable("notifications") {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDashboard = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    )
                }

                composable("submission/{data}") { backStackEntry ->
                    val data = backStackEntry.arguments?.getString("data")?.replace("%2F", "/")
                    if (data != null) {
                        val submission = Json.decodeFromString<Submission>(data)
                        PainScaleDetailScreen(
                            submission = submission,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable("call") {
                    PlatformVideoCallScreen(
                        appId = PendingCallData.appId,
                        token = PendingCallData.token,
                        channelName = PendingCallData.channelName,
                        onEndCall = {
                            PendingCallData.clear()
                            navController.popBackStack()
                        }
                    )
                }

                composable("video_call_list") {
                    VideoCallListScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = { callId -> navController.navigate("video_call_detail/$callId") },
                        onStartCall = { appId, token, channelName ->
                            PendingCallData.set(appId, token, channelName)
                            navController.navigate("call")
                        }
                    )
                }

                composable("video_call_detail/{callId}") { backStackEntry ->
                    val callId = backStackEntry.arguments?.getString("callId") ?: ""
                    VideoCallDetailScreen(
                        callId = callId,
                        onBack = { navController.popBackStack() },
                        onStartCall = { appId, token, channelName ->
                            PendingCallData.set(appId, token, channelName)
                            navController.navigate("call")
                        }
                    )
                }

                composable("adherence") {
                    AdherenceScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDashboard = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToDetail = { patient ->
                            // Pass only the id (ObjectId hex = URL-safe); detail screen refetches.
                            patient.patientId?.let { navController.navigate("adherence_detail/$it") }
                        }
                    )
                }

                composable("adherence_detail/{patientId}") { backStackEntry ->
                    val patientId = backStackEntry.arguments?.getString("patientId")
                    val repository = remember { ApiRepository() }

                    var patient: AdherencePatient? by remember { mutableStateOf(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    var hasError by remember { mutableStateOf(false) }

                    LaunchedEffect(patientId) {
                        if (patientId.isNullOrBlank()) {
                            hasError = true
                            isLoading = false
                        } else {
                            patient = repository.getPatientAdherence(patientId)
                            hasError = patient == null
                            isLoading = false
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (hasError) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Error loading patient data",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { navController.popBackStack() }) {
                                    Text("Go Back")
                                }
                            }
                        }
                    } else if (patient != null) {
                        PatientAdherenceDetailScreen(
                            patient = patient!!,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable("slit_lamp") {
                    SlitLampScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
