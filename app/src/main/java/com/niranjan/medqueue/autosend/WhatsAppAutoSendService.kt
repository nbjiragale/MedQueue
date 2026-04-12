package com.niranjan.medqueue.autosend

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.niranjan.medqueue.MainActivity

/**
 * Accessibility service that auto-taps the **Send** button inside WhatsApp
 * when [AutoSendPrefs.isAutoSendPending] is true.
 *
 * Safety guarantees:
 * - Only listens to `com.whatsapp` (declared in XML config).
 * - Only acts when **both** user-toggle AND per-action flag are set.
 * - Clears the pending flag immediately after tapping, so it never
 *   fires twice or interferes with normal WhatsApp usage.
 * - Returns the user to MedQueue after sending.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    companion object {
        private const val TAG = "WA_AutoSend"

        /** Content description WhatsApp uses for the send button. */
        private const val SEND_DESC = "Send"

        /** Delay (ms) before navigating back to MedQueue after send. */
        private const val RETURN_DELAY_MS = 1000L

        /** Max attempts to look for the send button before giving up. */
        private const val MAX_RETRIES = 15

        /** Debounce window – ignore rapid duplicate events. */
        private const val DEBOUNCE_MS = 400L
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Prevents re-entrance while we're already processing. */
    @Volatile private var processing = false

    /** Retry counter – reset every time a new pending flag is set. */
    private var retryCount = 0

    /** Timestamp of last event we actually processed. */
    private var lastProcessedAt = 0L

    // ─────────────────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (processing) return
        if (!AutoSendPrefs.isAutoSendEnabled(this)) return
        if (!AutoSendPrefs.isAutoSendPending(this)) return

        // Debounce: skip events that arrive too quickly
        val now = System.currentTimeMillis()
        if (now - lastProcessedAt < DEBOUNCE_MS) return
        lastProcessedAt = now

        val root = rootInActiveWindow ?: return

        try {
            val sendNode = findSendButton(root)
            if (sendNode != null) {
                processing = true
                retryCount = 0
                Log.d(TAG, "Found Send button – clicking")

                sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                AutoSendPrefs.clearAutoSendPending(this)

                // Give WhatsApp a moment to process the send, then return
                handler.postDelayed({
                    returnToMedQueue()
                    processing = false
                }, RETURN_DELAY_MS)
            } else {
                retryCount++
                if (retryCount >= MAX_RETRIES) {
                    Log.w(TAG, "Send button not found after $MAX_RETRIES retries – giving up")
                    AutoSendPrefs.clearAutoSendPending(this)
                    retryCount = 0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing event", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
        processing = false
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Recursively searches the node tree for a clickable node whose
     * content-description equals "Send" (case-insensitive).
     */
    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Fast path – search by content description text
        val candidates = root.findAccessibilityNodeInfosByText(SEND_DESC)
        for (node in candidates) {
            val desc = node.contentDescription?.toString() ?: continue
            if (desc.equals(SEND_DESC, ignoreCase = true) && node.isClickable) {
                return node
            }
        }

        // Slow path – full tree traversal as fallback
        return walkTree(root)
    }

    private fun walkTree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()
        if (desc != null && desc.equals(SEND_DESC, ignoreCase = true) && node.isClickable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = walkTree(child)
            if (result != null) return result
        }
        return null
    }

    /** Brings MedQueue back to the foreground. */
    private fun returnToMedQueue() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to return to MedQueue", e)
        }
    }
}
