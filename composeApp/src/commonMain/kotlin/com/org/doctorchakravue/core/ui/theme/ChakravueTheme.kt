package com.org.doctorchakravue.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MedicalColorScheme = lightColorScheme(
    primary = DoctorGreen,
    onPrimary = DoctorWhite, // Text on green buttons
    secondary = DoctorBlue,
    onSecondary = DoctorWhite,
    background = DoctorWhite,
    surface = DoctorWhite, // Cards are white
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DoctorLightGray, // Input fields background
    error = ErrorRed
)

@Composable
fun ChakravueTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MedicalColorScheme,
        typography = Typography,
        content = content
    )
}
