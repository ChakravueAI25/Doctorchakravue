package com.org.doctorchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.app.LegalConfig
import com.org.doctorchakravue.ui.theme.AppBackground
import com.org.doctorchakravue.ui.theme.AppTheme

/**
 * One-time consent screen shown after login until the doctor accepts the current
 * Terms version. Pure UI — the caller (App) persists acceptance in [onAccept].
 */
@Composable
fun TermsScreen(onAccept: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var checked by remember { mutableStateOf(false) }

    AppTheme {
        AppBackground {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    "Terms & Conditions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        LegalConfig.SUMMARY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { uriHandler.openUri(LegalConfig.TERMS_URL) }) {
                        Text("Read full Terms of Service", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { uriHandler.openUri(LegalConfig.PRIVACY_URL) }) {
                        Text("Read Privacy Policy", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            checkmarkColor = Color(0xFF334671),
                            uncheckedColor = Color.White
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "I have read and agree to the Terms & Conditions and Privacy Policy",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onAccept,
                    enabled = checked,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF334671),
                        disabledContainerColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Agree & Continue", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
