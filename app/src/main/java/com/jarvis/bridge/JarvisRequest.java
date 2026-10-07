package com.jarvis.bridge;

public final class JarvisRequest {

    private JarvisRequest() {}

    public static volatile String requestId;
    public static volatile String phone;
    public static volatile String name;
    public static volatile String body;

    public static volatile String callbackHost;
    public static volatile int callbackPort = 8765;
    public static volatile String callbackToken;

    public static boolean isPending() {
        return requestId != null
                && body != null
                && !body.isEmpty();
    }

    public static void clear() {
        requestId = null;
        phone = null;
        name = null;
        body = null;
        callbackHost = null;
        callbackPort = 8765;
        callbackToken = null;
    }
}