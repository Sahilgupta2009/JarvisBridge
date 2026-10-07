package com.jarvis.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

object JarvisRequest {
    @Volatile
    var requestId: String? = null

    @Volatile
    var phone: String? = null

    @Volatile
    var name: String? = null

    @Volatile
    var body: String? = null

    @Volatile
    var callbackHost: String? = null

    @Volatile
    var callbackPort: Int = 8765

    @Volatile
    var callbackToken: String? = null

    fun clear() {
        requestId = null
        phone = null
        name = null
        body = null
        callbackHost = null
        callbackToken = null
        callbackPort = 8765
    }
}

class CommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != "com.jarvis.bridge.SEND_WHATSAPP") {
            return
        }

        JarvisRequest.requestId =
            intent.getStringExtra("request_id")

        JarvisRequest.phone =
            intent.getStringExtra("phone")

        JarvisRequest.name =
            intent.getStringExtra("name")

        JarvisRequest.body =
            intent.getStringExtra("body")

        JarvisRequest.callbackHost =
            intent.getStringExtra("callback_host")

        JarvisRequest.callbackPort =
            intent.getIntExtra("callback_port", 8765)

        JarvisRequest.callbackToken =
            intent.getStringExtra("callback_token")
    }
}