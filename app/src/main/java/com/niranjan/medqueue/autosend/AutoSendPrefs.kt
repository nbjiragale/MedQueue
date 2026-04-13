package com.niranjan.medqueue.autosend

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.edit

/**
 * Thin SharedPreferences wrapper that manages two flags:
 *
 * 1. **autoSendEnabled** – user-level toggle (Settings screen).
 * 2. **autoSendPending** – per-action flag set right before the WhatsApp
 *    intent is fired and cleared once the AccessibilityService taps Send.
 *
 * The service only acts when BOTH flags are true, so normal WhatsApp
 * usage is never intercepted.
 */
object AutoSendPrefs {

    private const val PREFS_NAME           = "medqueue_auto_send"
    private const val KEY_ENABLED          = "auto_send_enabled"
    private const val KEY_PENDING          = "auto_send_pending"
    private const val KEY_SMS_ENABLED      = "auto_send_sms_enabled"

    // ── WhatsApp user toggle ─────────────────────────────────────────────────

    fun isAutoSendEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setAutoSendEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    // ── Per-action flag (WhatsApp only) ─────────────────────────────────────

    fun isAutoSendPending(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PENDING, false)

    fun setAutoSendPending(context: Context, pending: Boolean) {
        prefs(context).edit { putBoolean(KEY_PENDING, pending) }
    }

    fun clearAutoSendPending(context: Context) =
        setAutoSendPending(context, false)

    // ── SMS user toggle ──────────────────────────────────────────────────────

    fun isSmsAutoSendEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SMS_ENABLED, false)

    fun setSmsAutoSendEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_SMS_ENABLED, enabled) }
    }

    // ── Accessibility service helpers ────────────────────────────────────────

    /**
     * Returns true when our WhatsAppAutoSendService is turned on
     * in the device's Accessibility settings.
     */
    fun isServiceEnabled(context: Context): Boolean {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expected = ComponentName(
            context.packageName,
            "com.niranjan.medqueue.autosend.WhatsAppAutoSendService"
        ).flattenToString()

        return raw.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** Opens the system Accessibility Settings page. */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}


