package com.niranjan.medqueue.autosend

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit

/**
 * Thin SharedPreferences wrapper that manages two flags:
 *
 * 1. **autoSendEnabled** – user-level toggle (Settings screen).
 * 2. **autoSendPending** – per-action flag set right after the WhatsApp
 *    intent is fired and cleared once the AccessibilityService taps Send.
 *
 * The service only acts when BOTH flags are true, so normal WhatsApp
 * usage is never intercepted.
 *
 * The pending flag also **expires**. Without that, a worker who opens
 * WhatsApp from MedQueue and then backs out without sending leaves the flag
 * armed indefinitely — and the next time they open WhatsApp by hand, the
 * service taps Send on whatever conversation happens to be on screen. The
 * window is deliberately short: it only has to cover the app switch.
 */
object AutoSendPrefs {

    private const val TAG                  = "AutoSendPrefs"
    private const val PREFS_NAME           = "medqueue_auto_send"
    private const val KEY_ENABLED          = "auto_send_enabled"
    private const val KEY_PENDING_AT       = "auto_send_pending_at"
    private const val KEY_SMS_ENABLED      = "auto_send_sms_enabled"

    /** How long an armed auto-send stays valid after leaving MedQueue. */
    private const val PENDING_TTL_MS = 60_000L

    // ── WhatsApp user toggle ─────────────────────────────────────────────────

    fun isAutoSendEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setAutoSendEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
        if (!enabled) clearAutoSendPending(context)
    }

    // ── Per-action flag (WhatsApp only) ─────────────────────────────────────

    /** True only while an auto-send armed within [PENDING_TTL_MS] is outstanding. */
    fun isAutoSendPending(context: Context): Boolean {
        val armedAt = prefs(context).getLong(KEY_PENDING_AT, 0L)
        if (armedAt == 0L) return false

        val age = System.currentTimeMillis() - armedAt
        if (age in 0..PENDING_TTL_MS) return true

        // Expired, or the clock moved backwards — disarm so a stale flag can
        // never fire against an unrelated conversation.
        clearAutoSendPending(context)
        return false
    }

    fun armAutoSend(context: Context) {
        prefs(context).edit { putLong(KEY_PENDING_AT, System.currentTimeMillis()) }
    }

    fun clearAutoSendPending(context: Context) {
        prefs(context).edit { remove(KEY_PENDING_AT) }
    }

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
     *
     * Entries are compared as parsed [ComponentName]s rather than as strings,
     * because the platform is free to store either the full or the
     * package-relative form ("pkg/.autosend.Service").
     */
    fun isServiceEnabled(context: Context): Boolean {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expected = ComponentName(
            context.packageName,
            WhatsAppAutoSendService::class.java.name
        )

        return raw.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it.trim()) }
            .any { it == expected }
    }

    /**
     * Opens the system Accessibility Settings page.
     * Returns false when no activity handles the intent (some OEM ROMs).
     */
    fun openAccessibilitySettings(context: Context): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No activity for ACTION_ACCESSIBILITY_SETTINGS", e)
        false
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}


