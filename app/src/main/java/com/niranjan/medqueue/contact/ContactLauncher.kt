package com.niranjan.medqueue.contact

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.net.toUri
import com.niranjan.medqueue.autosend.AutoSendPrefs
import com.niranjan.medqueue.data.settings.AppSettings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ── Types ─────────────────────────────────────────────────────────────────────

enum class ContactAction { WHATSAPP, SMS, CALL }

sealed class ContactActionResult {
    object Success              : ContactActionResult()
    object AutoSent             : ContactActionResult()
    object InvalidPhone         : ContactActionResult()
    object WhatsAppNotInstalled : ContactActionResult()
    object NoHandler            : ContactActionResult()
    object SmsPermissionNeeded  : ContactActionResult()
}

// ── Message builders ──────────────────────────────────────────────────────────

/**
 * Builds the bilingual (English + Kannada) availability message
 * with WhatsApp markdown formatting (*bold*, _italic_).
 */
fun buildWhatsAppMessage(settings: AppSettings): String {
    val contact2Line = if (settings.contact2Phone.isNotBlank())
        "📞 ${settings.contact2Name}: ${settings.contact2Phone}\n" else ""

    return "*✅ Your Medicines Are Ready!*\n\n" +
        "Dear Customer, the medicines you requested are now in stock and ready for pickup.\n\n" +
        "⏰ _Please collect at your earliest convenience._\n\n" +
        "Thank you for trusting us with your health. 🙏\n" +
        "*_\"Health is wealth\"_*\n" +
        "——————————————\n" +
        "*✅ ನಿಮ್ಮ ಔಷಧಿಗಳು ಸಿದ್ಧವಾಗಿವೆ!*\n\n" +
        "ಪ್ರಿಯ ಗ್ರಾಹಕರೇ, ನೀವು ಕೇಳಿದ ಔಷಧಿಗಳು ಈಗ ಲಭ್ಯವಿದ್ದು, ತೆಗೆದುಕೊಳ್ಳಲು ಸಿದ್ಧವಾಗಿವೆ.\n\n" +
        "*_ಆರೋಗ್ಯವೇ ಭಾಗ್ಯ_*\n" +
        "——————————————\n" +
        "🏥 *${settings.shopName}*\n" +
        "📍 _${settings.shopAddress}_\n" +
        "📞 ${settings.contact1Name}: ${settings.contact1Phone}\n" +
        contact2Line
}

/**
 * Builds the same bilingual message but as plain text (no markdown).
 * Used for SMS where *bold* / _italic_ markers look ugly.
 */
fun buildSmsMessage(settings: AppSettings): String {
    val contact2Line = if (settings.contact2Phone.isNotBlank())
        "📞 ${settings.contact2Name}: ${settings.contact2Phone}\n" else ""

    return "✅ Your Medicines Are Ready!\n\n" +
        "Dear Customer, the medicines you requested are now in stock and ready for pickup.\n\n" +
        "⏰ Please collect at your earliest convenience.\n\n" +
        "Thank you for trusting us with your health. 🙏\n" +
        "\"Health is wealth\"\n" +
        "——————————————\n" +
        "✅ ನಿಮ್ಮ ಔಷಧಿಗಳು ಸಿದ್ಧವಾಗಿವೆ!\n\n" +
        "ಪ್ರಿಯ ಗ್ರಾಹಕರೇ, ನೀವು ಕೇಳಿದ ಔಷಧಿಗಳು ಈಗ ಲಭ್ಯವಿದ್ದು, ತೆಗೆದುಕೊಳ್ಳಲು ಸಿದ್ಧವಾಗಿವೆ.\n\n" +
        "ಆರೋಗ್ಯವೇ ಭಾಗ್ಯ\n" +
        "——————————————\n" +
        "🏥 ${settings.shopName}\n" +
        "📍 ${settings.shopAddress}\n" +
        "📞 ${settings.contact1Name}: ${settings.contact1Phone}\n" +
        contact2Line
}

/**
 * Picks the right message variant based on the contact action.
 */
fun buildMessage(settings: AppSettings, action: ContactAction = ContactAction.WHATSAPP): String =
    when (action) {
        ContactAction.WHATSAPP -> buildWhatsAppMessage(settings)
        ContactAction.SMS      -> buildSmsMessage(settings)
        ContactAction.CALL     -> buildWhatsAppMessage(settings) // not used for calls, fallback
    }

// ── Action dispatcher ─────────────────────────────────────────────────────────

fun launchContactAction(
    context: Context,
    action: ContactAction,
    phone: String,
    message: String
): ContactActionResult {
    if (phone.isBlank()) return ContactActionResult.InvalidPhone

    return when (action) {
        ContactAction.WHATSAPP -> launchWhatsApp(context, phone, message)
        ContactAction.SMS      -> launchSms(context, phone, message)
        ContactAction.CALL     -> launchCall(context, phone)
    }
}

// ── Phone normalisation ───────────────────────────────────────────────────────

/**
 * Strips non-digit characters and ensures the phone number includes
 * the Indian country code (91) exactly once.
 *
 * Examples:
 *   "9876543210"      → "919876543210"
 *   "+91 98765 43210" → "919876543210"
 *   "09876543210"     → "919876543210"
 *   "919876543210"    → "919876543210"
 */
private fun normalizeIndianPhone(raw: String): String {
    val digits = raw.replace(Regex("[^\\d]"), "")
    return when {
        digits.length >= 12 && digits.startsWith("91") -> digits                // already has 91
        digits.length == 11 && digits.startsWith("0")  -> "91${digits.drop(1)}" // leading 0
        digits.length == 10                             -> "91$digits"           // local number
        else                                            -> digits                // fallback as-is
    }
}

// ── Intent helpers ────────────────────────────────────────────────────────────

private fun launchWhatsApp(context: Context, phone: String, message: String): ContactActionResult {
    val normalizedPhone = normalizeIndianPhone(phone)
    return try {
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())

        // If auto-send is on, tell the accessibility service to tap Send
        if (AutoSendPrefs.isAutoSendEnabled(context) && AutoSendPrefs.isServiceEnabled(context)) {
            AutoSendPrefs.setAutoSendPending(context, true)
        }

        // api.whatsapp.com/send resolves directly inside WhatsApp —
        // wa.me adds an extra HTTP redirect that slows things down.
        val intent = Intent(
            Intent.ACTION_VIEW,
            "https://api.whatsapp.com/send?phone=$normalizedPhone&text=$encoded".toUri()
        ).setPackage("com.whatsapp")
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        AutoSendPrefs.clearAutoSendPending(context)
        ContactActionResult.WhatsAppNotInstalled
    }
}

private fun launchSms(context: Context, phone: String, message: String): ContactActionResult {
    // If auto-send SMS is enabled, send directly via SmsManager
    if (AutoSendPrefs.isSmsAutoSendEnabled(context)) {
        return sendSmsDirect(context, phone, message)
    }

    // Otherwise open the SMS app with prefilled message (original behavior)
    return try {
        val intent = Intent(Intent.ACTION_VIEW, "sms:$phone".toUri())
            .putExtra("sms_body", message)
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        ContactActionResult.NoHandler
    }
}

/**
 * Sends an SMS directly in the background using [SmsManager].
 * Handles multi-part messages automatically (the template is > 160 chars).
 */
private fun sendSmsDirect(context: Context, phone: String, message: String): ContactActionResult {
    return try {
        @Suppress("DEPRECATION")
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        Log.d("ContactLauncher", "SMS auto-sent to $phone (${parts.size} part(s))")
        ContactActionResult.AutoSent
    } catch (e: SecurityException) {
        // Permission not granted
        Log.w("ContactLauncher", "SEND_SMS permission not granted", e)
        ContactActionResult.SmsPermissionNeeded
    } catch (e: Exception) {
        Log.e("ContactLauncher", "Failed to send SMS directly", e)
        ContactActionResult.NoHandler
    }
}

private fun launchCall(context: Context, phone: String): ContactActionResult {
    return try {
        // ACTION_DIAL only — no CALL permission required
        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        ContactActionResult.NoHandler
    }
}





