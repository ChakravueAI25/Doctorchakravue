package com.org.doctorchakravue.platform

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

private const val TAG = "AgoraVideoCall"

// ─────────────────────────────────────────────────────────────────────────────
// Agora engine manager — created once per call, destroyed on end
// ─────────────────────────────────────────────────────────────────────────────
private class AgoraManager(
    private val appId: String,
    private val onRemoteJoined: (SurfaceView) -> Unit,
    private val onRemoteLeft: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var engine: RtcEngine? = null
    var localSurface: SurfaceView? = null
        private set

    private val handler = object : IRtcEngineEventHandler() {
        override fun onError(err: Int) {
            Log.e(TAG, "Agora SDK error $err: ${RtcEngine.getErrorDescription(err)}")
            onError("Agora error $err: ${RtcEngine.getErrorDescription(err)}")
        }
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(TAG, "✅ Joined channel '$channel' uid=$uid")
        }
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "📞 Remote user joined uid=$uid")
            engine?.let { rtc ->
                val surface = SurfaceView(localSurface!!.context.applicationContext)
                surface.setZOrderMediaOverlay(true)
                rtc.setupRemoteVideo(VideoCanvas(surface, VideoCanvas.RENDER_MODE_FIT, uid))
                onRemoteJoined(surface)
            }
        }
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "📵 Remote user left uid=$uid")
            onRemoteLeft()
        }
    }

    fun start(context: android.content.Context, token: String, channelName: String) {
        if (appId.isBlank()) {
            val msg = "appId is EMPTY.\n\nThe /call/token backend response must include an 'app_id' or 'appId' field with your Agora App ID.\n\nCheck Logcat for '[VideoCall] /call/token raw response:' to see what the patient backend returned."
            Log.e(TAG, msg)
            onError(msg)
            return
        }
        if (token.isBlank()) {
            val msg = "Token is EMPTY. Check that /call/token returns a non-empty 'token' field."
            Log.e(TAG, msg)
            onError(msg)
            return
        }

        // Validate Agora App ID format: must be exactly 32 hex characters
        val trimmedAppId = appId.trim()
        val validAppIdPattern = Regex("^[0-9a-fA-F]{32}$")
        if (!validAppIdPattern.matches(trimmedAppId)) {
            val msg = "Invalid Agora App ID format.\n\nReceived: '${trimmedAppId.take(20)}${if (trimmedAppId.length > 20) "..." else ""}' (length=${trimmedAppId.length})\n\nAgora App IDs must be exactly 32 hex characters.\n\nCheck Logcat for '[VideoCall] /call/token raw response:' to see the full backend response."
            Log.e(TAG, "Invalid App ID: '$trimmedAppId' (len=${trimmedAppId.length}). Expected 32-char hex string.")
            onError(msg)
            return
        }

        Log.d(TAG, "start() appId='$trimmedAppId' channel='$channelName' token(len)=${token.length}")
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = trimmedAppId
                mEventHandler = handler
            }
            engine = RtcEngine.create(config)
            engine!!.enableVideo()
            engine!!.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x480,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
            // Set up local preview with front camera
            val surface = SurfaceView(context.applicationContext)
            localSurface = surface
            engine!!.setupLocalVideo(VideoCanvas(surface, VideoCanvas.RENDER_MODE_FIT, 0))
            engine!!.startPreview()

            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                publishCameraTrack = true
                publishMicrophoneTrack = true
                autoSubscribeVideo = true
                autoSubscribeAudio = true
            }
            engine!!.joinChannel(token, channelName, 0, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Agora: ${e.message}", e)
            onError(e.message ?: "Init failed")
        }
    }

    fun muteAudio(mute: Boolean) = engine?.muteLocalAudioStream(mute)
    fun muteVideo(mute: Boolean) {
        engine?.muteLocalVideoStream(mute)
        if (mute) engine?.stopPreview() else engine?.startPreview()
    }

    fun destroy() {
        engine?.stopPreview()
        engine?.leaveChannel()
        RtcEngine.destroy()
        engine = null
        localSurface = null
        Log.d(TAG, "RtcEngine destroyed")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Actual platform composable — entry point called from App.kt navigation
// ─────────────────────────────────────────────────────────────────────────────
@Composable
actual fun PlatformVideoCallScreen(
    appId: String,
    token: String,
    channelName: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current

    // Check if permissions are already granted (stored by OS after first grant)
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionsDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        permissionsDenied = !permissionsGranted
    }

    // Request permissions on first entry if not already granted
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    when {
        permissionsGranted -> CallScreen(appId, token, channelName, onEndCall)
        permissionsDenied  -> PermissionDeniedScreen(onEndCall)
        // else: waiting for system dialog — show nothing
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// The actual call screen — shown only after permissions are granted
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CallScreen(appId: String, token: String, channelName: String, onEndCall: () -> Unit) {
    val context = LocalContext.current

    var isMuted        by remember { mutableStateOf(false) }
    var isCameraOff    by remember { mutableStateOf(false) }
    var remoteJoined   by remember { mutableStateOf(false) }
    var remoteSurface  by remember { mutableStateOf<SurfaceView?>(null) }
    var localSurface   by remember { mutableStateOf<SurfaceView?>(null) }
    var callError      by remember { mutableStateOf<String?>(null) }

    val manager = remember {
        AgoraManager(
            appId         = appId,
            onRemoteJoined = { surface -> remoteSurface = surface; remoteJoined = true },
            onRemoteLeft   = { remoteSurface = null; remoteJoined = false },
            onError        = { msg -> callError = msg }
        )
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "CallScreen: appId='$appId' channel='$channelName' token.len=${token.length}")
        manager.start(context, token, channelName)
        localSurface = manager.localSurface
    }

    DisposableEffect(Unit) {
        onDispose { manager.destroy() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {

        // Full-screen remote video
        if (remoteJoined && remoteSurface != null) {
            AndroidView(factory = { remoteSurface!! }, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (callError != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Error: $callError", color = Color.White, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        Spacer(Modifier.height(16.dp))
                        Text("Waiting for patient to join…", color = Color.White,
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        // Local camera PiP (top-left corner)
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 56.dp)
                .size(width = 120.dp, height = 160.dp)
                .background(Color(0xFF2D2D44), RoundedCornerShape(12.dp))
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            if (!isCameraOff && localSurface != null) {
                AndroidView(factory = { localSurface!! }, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.VideocamOff, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }
        }

        // Header
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Video Call", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (remoteJoined) "Connected" else "Connecting…", color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodyMedium)
        }

        // Control buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                active = isMuted
            ) { isMuted = !isMuted; manager.muteAudio(isMuted) }

            FloatingActionButton(onClick = { manager.destroy(); onEndCall() },
                containerColor = Color.Red, modifier = Modifier.size(72.dp)) {
                Icon(Icons.Default.CallEnd, "End Call", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            CallButton(
                icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = if (isCameraOff) "Cam On" else "Cam Off",
                active = isCameraOff
            ) { isCameraOff = !isCameraOff; manager.muteVideo(isCameraOff) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shown when the user denies camera/mic permissions
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionDeniedScreen(onEndCall: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.VideocamOff, null, tint = Color.Red, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            Text("Camera & Microphone\nPermission Required", color = Color.White,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Grant camera and microphone access in Settings to make video calls.",
                color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)) { Text("Go Back") }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small reusable mute/camera control button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CallButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(onClick = onClick,
            containerColor = if (active) Color(0xFF3D3D5C) else Color(0xFF2D2D44),
            modifier = Modifier.size(56.dp)) {
            Icon(icon, label, tint = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}
