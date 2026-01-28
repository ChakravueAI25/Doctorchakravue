package com.org.doctorchakravue.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.data.SessionManager
import com.org.doctorchakravue.platform.registerFcmTokenAfterLogin
import com.org.doctorchakravue.ui.theme.AppBackground
import com.org.doctorchakravue.ui.theme.AppTheme
import com.org.doctorchakravue.ui.theme.DoctorGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- State ---
data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

// --- ViewModel ---
class LoginViewModel(private val repository: ApiRepository) {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private val sessionManager = SessionManager()

    fun login(email: String, pass: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                withContext(Dispatchers.Default) {
                    repository.login(email, pass)
                }

                // Register FCM token after successful login
                try {
                    registerFcmTokenAfterLogin(repository, sessionManager)
                } catch (e: Exception) {
                    // Don't fail login if FCM registration fails
                    println("FCM registration failed: ${e.message}")
                }

                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

// --- Screen ---
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    showSnackbar: (String) -> Unit = {}
) {
    val viewModel = remember { LoginViewModel(ApiRepository()) }
    val state by viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            showSnackbar("Login Successful!")
            onLoginSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { showSnackbar(it) }
    }

    AppTheme {
        AppBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        border = BorderStroke(2.dp, DoctorGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Login", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2979FF))
                            Text("ChakraVue Healthcare", color = Color(0xFF757575), fontSize = 16.sp, modifier = Modifier.padding(bottom = 24.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    cursorColor = MaterialTheme.colorScheme.onBackground,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            null,
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    cursorColor = MaterialTheme.colorScheme.onBackground,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (state.isLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            } else {
                                Button(
                                    onClick = {
                                        if (email.isBlank() || password.isBlank()) {
                                            showSnackbar("Please enter email and password")
                                            return@Button
                                        }
                                        viewModel.login(email.trim(), password.trim())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DoctorGreen, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(25.dp)
                                ) {
                                    Text("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
