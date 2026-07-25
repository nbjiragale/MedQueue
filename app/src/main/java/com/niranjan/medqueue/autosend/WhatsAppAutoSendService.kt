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

        /**
         * WhatsApp's resource id for the send button. Matching on this is
         * language-independent, unlike the content description, which is
         * localised and so breaks on any non-English install.
         */
        private const val SEND_VIEW_ID = "com.whatsapp:id/send"

        /** Content description fallback, used only if the view id is absent. */
        private const val SEND_DESC = "Send"

        /** Delay (ms) before navigating back to MedQueue after send. */
        private const val RETURN_DELAY_MS = 1000L

        /** Max attempts to look for the send button before giving up. */
        private const val MAX_RETRIES = 15

        /** Debounce window – ignore rapid duplicate events. */
        private const val DEBOUNCE_MS = 400L

        /** Depth cap for the fallback traversal, to bound work on deep trees. */
        private const val MAX_TREE_DEPTH = 40
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Prevents re-entrance while we're already processing. */
    @Volatile private var processing = false

    /** Retry counter – reset every time a new auto-send is armed. */
    private var retryCount = 0

    /** Pending state seen on the previous event, used to detect a fresh arming. */
    private var wasPending = false

    /** Timestamp of last event we actually processed. */
    private var lastProcessedAt = 0L

    // ─────────────────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (processing) return
        if (!AutoSendPrefs.isAutoSendEnabled(this)) return

        val pending = AutoSendPrefs.isAutoSendPending(this)
        // A fresh arming starts a fresh retry budget. Without this the counter
        // carried over from a previous attempt, so an auto-send could give up
        // almost immediately after a partial failure.
        if (pending && !wasPending) retryCount = 0
        wasPending = pending
        if (!pending) return

        // Debounce: skip events that arrive too quickly
        val now = System.currentTimeMillis()
        if (now - lastProcessedAt < DEBOUNCE_MS) return
        lastProcessedAt = now

        val root = rootInActiveWindow ?: return

        try {
            val sendNode = findSendButton(root)
            if (sendNode == null) {
                retryCount++
                if (retryCount >= MAX_RETRIES) {
                    Log.w(TAG, "Send button not found after $MAX_RETRIES retries – giving up")
                    AutoSendPrefs.clearAutoSendPending(this)
                    retryCount = 0
                }
                return
            }

            processing = true
            val clicked = sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked) {
                // Leave the flag armed so the next event can retry.
                Log.w(TAG, "Send button click was refused – will retry")
                processing = false
                retryCount++
                return
            }

            Log.d(TAG, "Send button clicked")
            retryCount = 0
            AutoSendPrefs.clearAutoSendPending(this)

            // Give WhatsApp a moment to process the send, then return
            handler.postDelayed({
                returnToMedQueue()
                processing = false
            }, RETURN_DELAY_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing event", e)
            processing = false
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
        handler.removeCallbacksAndMessages(null)
        processing = false
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        // Never leave an auto-send armed once we can no longer service it.
        AutoSendPrefs.clearAutoSendPending(this)
        super.onDestroy()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Finds WhatsApp's send button, preferring the stable resource id and
     * falling back to the (localised) content description.
     */
    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Fast path – resource id, language independent.
        root.findAccessibilityNodeInfosByViewId(SEND_VIEW_ID)
            .firstOrNull { it.isClickable && it.isVisibleToUser }
            ?.let { return it }

        // Fallback – content description, for builds where the id has changed.
        root.findAccessibilityNodeInfosByText(SEND_DESC)
            .firstOrNull { it.isSendButton() }
            ?.let { return it }

        // Last resort – bounded tree walk.
        return walkTree(root, depth = 0)
    }

    private fun AccessibilityNodeInfo.isSendButton(): Boolean =
        isClickable &&
        isVisibleToUser &&
        contentDescription?.toString()?.equals(SEND_DESC, ignoreCase = true) == true

    private fun walkTree(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_TREE_DEPTH) return null
        if (node.isSendButton()) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = walkTree(child, depth + 1)
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
