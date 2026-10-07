package com.jarvis.bridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class JarvisAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var lastClickTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName != "com.whatsapp") {
            return
        }

        if (JarvisRequest.requestId == null) {
            return
        }

        // Avoid hammering the WhatsApp UI repeatedly.
        val now = System.currentTimeMillis()

        if (now - lastClickTime < 1000) {
            return
        }

        val root = rootInActiveWindow ?: return

        val expectedMessage = JarvisRequest.body ?: return

        // Safety check: make sure the message we're about to send
        // actually appears somewhere in the WhatsApp accessibility tree.
        if (!containsText(root, expectedMessage)) {
            return
        }

        val sendButton = findSendButton(root) ?: return

        lastClickTime = now

        var clicked = false

        // First try the accessibility click action.
        if (sendButton.isClickable) {
            clicked = sendButton.performAction(
                AccessibilityNodeInfo.ACTION_CLICK
            )
        }

        // Some Android/WhatsApp versions expose the node but don't
        // implement ACTION_CLICK correctly. Walk upward to a clickable
        // parent.
        if (!clicked) {
            var parent = sendButton.parent

            while (parent != null) {

                if (parent.isClickable) {

                    clicked = parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )

                    if (clicked) {
                        break
                    }
                }

                parent = parent.parent
            }
        }

        // Final fallback: tap the center of the Send button.
        if (!clicked) {
            val bounds = Rect()

            sendButton.getBoundsInScreen(bounds)

            if (!bounds.isEmpty) {
                clicked = dispatchTap(
                    bounds.centerX().toFloat(),
                    bounds.centerY().toFloat()
                )
            }
        }

        if (clicked) {

            val requestId = JarvisRequest.requestId ?: return

            val host = JarvisRequest.callbackHost ?: return
            val port = JarvisRequest.callbackPort
            val token = JarvisRequest.callbackToken ?: return

            sendResultAsync(
                host = host,
                port = port,
                token = token,
                requestId = requestId,
                status = "sent"
            )

            JarvisRequest.clear()
        }
    }

    override fun onInterrupt() {
    }

    private fun findSendButton(
        node: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {

        if (node == null) return null

        val id = node.viewIdResourceName
            ?.lowercase()
            ?: ""

        val description = node.contentDescription
            ?.toString()
            ?.lowercase()
            ?: ""

        val text = node.text
            ?.toString()
            ?.lowercase()
            ?: ""

        /*
         * We intentionally don't rely on one exact WhatsApp resource ID.
         * WhatsApp can change its UI between releases.
         */

        val looksLikeSend =
            id.endsWith(":id/send") ||
            id.endsWith("/send") ||
            description == "send" ||
            description == "send message" ||
            text == "send"

        if (looksLikeSend && node.isEnabled) {
            return node
        }

        for (i in 0 until node.childCount) {

            val child = node.getChild(i)

            val result = findSendButton(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    private fun containsText(
        node: AccessibilityNodeInfo?,
        expected: String
    ): Boolean {

        if (node == null) return false

        val target = expected.trim()

        if (target.isEmpty()) return false

        val nodeText =
            node.text?.toString() ?: ""

        val nodeDescription =
            node.contentDescription?.toString() ?: ""

        if (nodeText.contains(target, ignoreCase = false)) {
            return true
        }

        if (nodeDescription.contains(target, ignoreCase = false)) {
            return true
        }

        for (i in 0 until node.childCount) {

            if (containsText(node.getChild(i), expected)) {
                return true
            }
        }

        return false
    }

    private fun dispatchTap(
        x: Float,
        y: Float
    ): Boolean {

        val path = Path()

        path.moveTo(x, y)

        val stroke = GestureDescription.StrokeDescription(
            path,
            0,
            80
        )

        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        return dispatchGesture(
            gesture,
            null,
            null
        )
    }

    private fun sendResultAsync(
        host: String,
        port: Int,
        token: String,
        requestId: String,
        status: String
    ) {

        Thread {

            try {

                val url = URL(
                    "http://$host:$port/whatsapp/result"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.connectTimeout = 2000
                connection.readTimeout = 2000

                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val json = JSONObject()

                json.put("token", token)
                json.put("request_id", requestId)
                json.put("status", status)

                val bytes =
                    json.toString().toByteArray(Charsets.UTF_8)

                connection.outputStream.use {
                    it.write(bytes)
                }

                connection.responseCode

                connection.disconnect()

            } catch (_: Exception) {
                // JARVIS will timeout if the callback cannot be reached.
            }

        }.start()
    }
}