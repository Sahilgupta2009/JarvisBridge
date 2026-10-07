package com.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JarvisAccessibilityService extends AccessibilityService {

    private static final String WHATSAPP_PACKAGE = "com.whatsapp";

    private volatile long lastAttemptMs = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        CharSequence pkg = event.getPackageName();

        if (pkg == null || !WHATSAPP_PACKAGE.contentEquals(pkg)) {
            return;
        }

        if (!JarvisRequest.isPending()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastAttemptMs < 700) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root == null) {
            return;
        }

        String expected = JarvisRequest.body;

        if (expected == null || expected.trim().isEmpty()) {
            return;
        }

        /*
         * Safety check:
         * Only attempt to press Send if the exact message text
         * is visible in WhatsApp.
         */
        if (!containsExactText(root, expected)) {
            return;
        }

        AccessibilityNodeInfo send = findSendButton(root);

        if (send == null) {
            return;
        }

        lastAttemptMs = now;

        boolean clicked = clickNodeOrParent(send);

        if (!clicked) {
            Rect bounds = new Rect();
            send.getBoundsInScreen(bounds);

            if (!bounds.isEmpty()) {
                clicked = dispatchTap(
                        bounds.centerX(),
                        bounds.centerY()
                );
            }
        }

        if (clicked) {
            String requestId = JarvisRequest.requestId;
            String host = JarvisRequest.callbackHost;
            int port = JarvisRequest.callbackPort;
            String token = JarvisRequest.callbackToken;

            sendResultAsync(
                    host,
                    port,
                    token,
                    requestId,
                    "sent"
            );

            JarvisRequest.clear();
        }
    }

    private boolean clickNodeOrParent(
            AccessibilityNodeInfo node) {

        AccessibilityNodeInfo current = node;

        while (current != null) {

            if (current.isEnabled() && current.isClickable()) {

                if (current.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK)) {

                    return true;
                }
            }

            current = current.getParent();
        }

        return false;
    }

    private AccessibilityNodeInfo findSendButton(
            AccessibilityNodeInfo node) {

        if (node == null) {
            return null;
        }

        String id = safeLower(
                node.getViewIdResourceName()
        );

        String description = "";

        if (node.getContentDescription() != null) {
            description = safeLower(
                    node.getContentDescription().toString()
            );
        }

        String text = "";

        if (node.getText() != null) {
            text = safeLower(
                    node.getText().toString()
            );
        }

        boolean looksLikeSend =
                id.endsWith(":id/send")
                || id.endsWith("/send")
                || "send".equals(description)
                || "send message".equals(description)
                || "send".equals(text);

        if (looksLikeSend && node.isEnabled()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            AccessibilityNodeInfo result =
                    findSendButton(child);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean containsExactText(
            AccessibilityNodeInfo node,
            String expected) {

        if (node == null || expected == null) {
            return false;
        }

        String target = expected.trim();

        if (target.isEmpty()) {
            return false;
        }

        CharSequence nodeText = node.getText();

        if (nodeText != null
                && target.equals(nodeText.toString().trim())) {

            return true;
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null
                && target.equals(description.toString().trim())) {

            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (containsExactText(child, expected)) {
                return true;
            }
        }

        return false;
    }

    private boolean dispatchTap(
            float x,
            float y) {

        Path path = new Path();

        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        path,
                        0,
                        80
                );

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        return dispatchGesture(
                gesture,
                null,
                null
        );
    }

    private void sendResultAsync(
            String host,
            int port,
            String token,
            String requestId,
            String status) {

        if (host == null
                || token == null
                || requestId == null) {

            return;
        }

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        HttpURLConnection connection = null;

                        try {

                            URL url = new URL(
                                    "http://"
                                            + host
                                            + ":"
                                            + port
                                            + "/whatsapp/result"
                            );

                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();

                            connection.setRequestMethod("POST");

                            connection.setConnectTimeout(2500);
                            connection.setReadTimeout(2500);

                            connection.setDoOutput(true);

                            connection.setRequestProperty(
                                    "Content-Type",
                                    "application/json"
                            );

                            JSONObject json =
                                    new JSONObject();

                            json.put(
                                    "token",
                                    token
                            );

                            json.put(
                                    "request_id",
                                    requestId
                            );

                            json.put(
                                    "status",
                                    status
                            );

                            byte[] data =
                                    json.toString()
                                            .getBytes(
                                                    StandardCharsets.UTF_8
                                            );

                            try (OutputStream output =
                                         connection.getOutputStream()) {

                                output.write(data);
                            }

                            connection.getResponseCode();

                        } catch (Exception ignored) {

                            // Callback failure does not crash
                            // the accessibility service.

                        } finally {

                            if (connection != null) {
                                connection.disconnect();
                            }
                        }
                    }
                },
                "jarvis-result-callback"
        ).start();
    }

    private static String safeLower(
            String value) {

        if (value == null) {
            return "";
        }

        return value.toLowerCase();
    }

    @Override
    public void onInterrupt() {
        // Nothing to clean up.
    }
            }
