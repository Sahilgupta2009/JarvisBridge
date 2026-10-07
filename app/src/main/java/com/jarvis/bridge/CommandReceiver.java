package com.jarvis.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CommandReceiver extends BroadcastReceiver {

    public static final String ACTION_SEND_WHATSAPP =
            "com.jarvis.bridge.SEND_WHATSAPP";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        if (!ACTION_SEND_WHATSAPP.equals(intent.getAction())) {
            return;
        }

        JarvisRequest.requestId =
                intent.getStringExtra("request_id");

        JarvisRequest.phone =
                intent.getStringExtra("phone");

        JarvisRequest.name =
                intent.getStringExtra("name");

        JarvisRequest.body =
                intent.getStringExtra("body");

        JarvisRequest.callbackHost =
                intent.getStringExtra("callback_host");

        JarvisRequest.callbackPort =
                intent.getIntExtra(
                        "callback_port",
                        8765
                );

        JarvisRequest.callbackToken =
                intent.getStringExtra("callback_token");
    }
}
