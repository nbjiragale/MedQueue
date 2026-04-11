package com.niranjan.medqueue.data.settings

import android.content.Context
import androidx.core.content.edit

// ── Data holder ──────────────────────────────────────────────────────────────

data class AppSettings(
    val shopName: String = "",
    val yogeshName: String = "",
    val yogeshPhone: String = "",
    val ownerPhone: String = ""
)

// ── SharedPreferences helper ─────────────────────────────────────────────────

class SettingsPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME      = "medqueue_settings"
        private const val KEY_SHOP_NAME   = "shopName"
        private const val KEY_YOGESH_NAME = "yogeshName"
        private const val KEY_YOGESH_PHONE = "yogeshPhone"
        private const val KEY_OWNER_PHONE = "ownerPhone"
    }

    /** Persist all settings at once. */
    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_SHOP_NAME,    settings.shopName)
            putString(KEY_YOGESH_NAME,  settings.yogeshName)
            putString(KEY_YOGESH_PHONE, settings.yogeshPhone)
            putString(KEY_OWNER_PHONE,  settings.ownerPhone)
        }
    }

    /** Read current settings. Returns defaults if nothing saved yet. */
    fun read(): AppSettings = AppSettings(
        shopName    = prefs.getString(KEY_SHOP_NAME,    "") ?: "",
        yogeshName  = prefs.getString(KEY_YOGESH_NAME,  "") ?: "",
        yogeshPhone = prefs.getString(KEY_YOGESH_PHONE, "") ?: "",
        ownerPhone  = prefs.getString(KEY_OWNER_PHONE,  "") ?: ""
    )

    /** Update only the provided fields; null = leave unchanged. */
    fun update(
        shopName:    String? = null,
        yogeshName:  String? = null,
        yogeshPhone: String? = null,
        ownerPhone:  String? = null
    ) {
        prefs.edit {
            shopName?.let    { putString(KEY_SHOP_NAME,    it) }
            yogeshName?.let  { putString(KEY_YOGESH_NAME,  it) }
            yogeshPhone?.let { putString(KEY_YOGESH_PHONE, it) }
            ownerPhone?.let  { putString(KEY_OWNER_PHONE,  it) }
        }
    }
}




