package com.niranjan.medqueue.data.settings

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import com.niranjan.medqueue.R
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════════════
// APP LANGUAGE
//
// The shop picks the UI language in Settings rather than inheriting the phone's.
// A shared counter phone is often set to whatever the last person left it on,
// and the shopkeeper reading the queue is not necessarily that person.
// ══════════════════════════════════════════════════════════════════════════════

/**
 * The languages the shopkeeper-facing UI is translated into.
 *
 * [tag] must match a `values-<tag>` resource folder, and [labelRes] is the
 * language's own name — a picker that spelled "ಕನ್ನಡ" as "Kannada" would only
 * be readable by someone who already reads the language they are leaving.
 */
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    ENGLISH("en", R.string.language_english),
    KANNADA("kn", R.string.language_kannada),
    HINDI("hi", R.string.language_hindi);

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        /**
         * English, deliberately — not the phone's locale. Every string has an
         * English original, so this is the one choice that can never render
         * half-translated.
         */
        val DEFAULT = ENGLISH

        fun fromTag(tag: String?): AppLanguage =
            AppLanguage.entries.firstOrNull { it.tag == tag } ?: DEFAULT
    }
}

/**
 * Applies the saved [AppLanguage] to a [Context].
 *
 * Per-app locales (`AppCompatDelegate.setApplicationLocales`) would need
 * appcompat and an `AppCompatActivity`; this app is Compose-only on a plain
 * `ComponentActivity`, so the language is carried by an overridden
 * configuration instead. Activities pick it up in `attachBaseContext`;
 * everything outside an activity — notifications, most notably — has to ask
 * for a wrapped context explicitly.
 */
object AppLocale {

    /** Wraps [context] in the saved language. */
    fun wrap(context: Context): Context =
        wrap(context, SettingsPrefs(context).language())

    /** Wraps [context] in [language], ignoring what is saved. */
    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = language.locale

        // Keeps SimpleDateFormat and friends in step with the UI: the queue
        // would otherwise show Kannada labels beside English month names.
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(config)
    }
}
