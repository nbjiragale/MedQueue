package com.niranjan.medqueue.data.settings

import android.content.Context
import androidx.core.content.edit

// ── Data holder ──────────────────────────────────────────────────────────────

data class AppSettings(
    val shopName:      String = "",
    val shopAddress:   String = "",
    val contact1Name:  String = "",
    val contact1Phone: String = "",
    val contact2Name:  String = "",
    val contact2Phone: String = ""
)

// ── SharedPreferences helper ─────────────────────────────────────────────────

class SettingsPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME          = "medqueue_settings"
        private const val KEY_SHOP_NAME       = "shopName"
        private const val KEY_SHOP_ADDRESS    = "shopAddress"
        private const val KEY_CONTACT1_NAME   = "contact1Name"
        private const val KEY_CONTACT1_PHONE  = "contact1Phone"
        private const val KEY_CONTACT2_NAME   = "contact2Name"
        private const val KEY_CONTACT2_PHONE  = "contact2Phone"
    }

    /** Persist all settings at once. */
    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_SHOP_NAME,      settings.shopName)
            putString(KEY_SHOP_ADDRESS,   settings.shopAddress)
            putString(KEY_CONTACT1_NAME,  settings.contact1Name)
            putString(KEY_CONTACT1_PHONE, settings.contact1Phone)
            putString(KEY_CONTACT2_NAME,  settings.contact2Name)
            putString(KEY_CONTACT2_PHONE, settings.contact2Phone)
        }
    }

    /** Read current settings. Returns defaults if nothing saved yet. */
    fun read(): AppSettings = AppSettings(
        shopName      = prefs.getString(KEY_SHOP_NAME,      "") ?: "",
        shopAddress   = prefs.getString(KEY_SHOP_ADDRESS,   "") ?: "",
        contact1Name  = prefs.getString(KEY_CONTACT1_NAME,  "") ?: "",
        contact1Phone = prefs.getString(KEY_CONTACT1_PHONE, "") ?: "",
        contact2Name  = prefs.getString(KEY_CONTACT2_NAME,  "") ?: "",
        contact2Phone = prefs.getString(KEY_CONTACT2_PHONE, "") ?: ""
    )

    /** Update only the provided fields; null = leave unchanged. */
    fun update(
        shopName:      String? = null,
        shopAddress:   String? = null,
        contact1Name:  String? = null,
        contact1Phone: String? = null,
        contact2Name:  String? = null,
        contact2Phone: String? = null
    ) {
        prefs.edit {
            shopName?.let      { putString(KEY_SHOP_NAME,      it) }
            shopAddress?.let   { putString(KEY_SHOP_ADDRESS,   it) }
            contact1Name?.let  { putString(KEY_CONTACT1_NAME,  it) }
            contact1Phone?.let { putString(KEY_CONTACT1_PHONE, it) }
            contact2Name?.let  { putString(KEY_CONTACT2_NAME,  it) }
            contact2Phone?.let { putString(KEY_CONTACT2_PHONE, it) }
        }
    }
}
