package com.org.doctorchakravue.data

/**
 * In-memory holder for the pending video call parameters.
 * Avoids URL-encoding issues when passing Agora tokens through navigation routes.
 * The values are set just before navigating to the call screen and cleared on end.
 */
object PendingCallData {
    var appId: String = ""
    var token: String = ""
    var channelName: String = ""

    fun set(appId: String, token: String, channelName: String) {
        this.appId = appId
        this.token = token
        this.channelName = channelName
    }

    fun clear() {
        appId = ""
        token = ""
        channelName = ""
    }
}
