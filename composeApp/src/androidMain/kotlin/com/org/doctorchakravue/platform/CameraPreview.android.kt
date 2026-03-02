package com.org.doctorchakravue.platform

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.size

@Composable
actual fun CameraPreviewView(
    modifier: Modifier,
    isCameraOff: Boolean
) {
    if (isCameraOff) {
        Box(
            modifier = modifier.fillMaxSize().background(Color(0xFF2D2D44)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VideocamOff,
                contentDescription = "Camera Off",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
    } else {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var previewView by remember { mutableStateOf<PreviewView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    }, ContextCompat.getMainExecutor(context))
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error unbinding camera: ${e.message}")
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this

                    // Start camera
                    startCamera(ctx, this, lifecycleOwner)
                }
            },
            modifier = modifier
        )
    }
}

private fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Use front camera for video calls
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )

            Log.d("CameraPreview", "Camera started successfully")
        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to start camera: ${e.message}")
        }
    }, ContextCompat.getMainExecutor(context))
}

