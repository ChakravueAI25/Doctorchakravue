package com.org.doctorchakravue

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.app.ui.splash.SplashScreen
import com.org.doctorchakravue.app.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Detect if this is a cold start (app killed/first launch)
        val isColdStart = savedInstanceState == null
        Log.d("SplashScreen", "isColdStart = $isColdStart")

        setContent {
            var showAnimatedSplash by remember { mutableStateOf(isColdStart) }

            Log.d("SplashScreen", "showAnimatedSplash = $showAnimatedSplash")

            if (showAnimatedSplash) {
                // Show custom animated splash screen
                Log.d("SplashScreen", "Showing DoctorChakravueSplashScreen")
                SplashScreen(
                    onSplashComplete = {
                        Log.d("SplashScreen", "Animation finished, hiding splash")
                        showAnimatedSplash = false
                    }
                )
            } else {
                // Show main app
                Log.d("SplashScreen", "Showing App")
                App()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}