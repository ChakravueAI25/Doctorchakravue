package com.org.doctorchakravue.app

/**
 * AppScreen - Defines all navigation routes in the app.
 */
sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object Dashboard : AppScreen("dashboard")
    object Patients : AppScreen("patients")
    object Adherence : AppScreen("adherence")
    object Notifications : AppScreen("notifications")
    object Profile : AppScreen("profile")
    object PainScaleHistory : AppScreen("pain_scale_history")
    object VideoCallList : AppScreen("video_call_list")

    // Parameterized routes
    object SubmissionDetail : AppScreen("submission/{data}") {
        fun createRoute(data: String) = "submission/$data"
    }

    object VideoCallDetail : AppScreen("video_call_detail/{callId}") {
        fun createRoute(callId: String) = "video_call_detail/$callId"
    }

    object Call : AppScreen("call/{appId}/{token}/{channelName}") {
        fun createRoute(appId: String, token: String, channelName: String) =
            "call/$appId/$token/$channelName"
    }
}
