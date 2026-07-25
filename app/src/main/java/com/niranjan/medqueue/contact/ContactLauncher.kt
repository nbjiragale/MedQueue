package com.niranjan.medqueue.contact

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.niranjan.medqueue.autosend.AutoSendPrefs
import com.niranjan.medqueue.data.settings.AppSettings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ── Types ─────────────────────────────────────────────────────────────────────

enum class ContactAction { WHATSAPP, SMS, CALL }

sealed class ContactActionResult {
    object Success              : ContactActionResult()
    /** Sent silently in the background. [parts] is what the carrier bills. */
    data class AutoSent(val parts: Int) : ContactActionResult()
    object InvalidPhone         : ContactActionResult()
    object WhatsAppNotInstalled : ContactActionResult()
    object NoHandler            : ContactActionResult()
    object SmsPermissionNeeded  : ContactActionResult()
    object SendFailed           : ContactActionResult()
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
    if (!isDialable(phone)) return ContactActionResult.InvalidPhone

    return when (action) {
        ContactAction.WHATSAPP -> launchWhatsApp(context, phone, message)
        ContactAction.SMS      -> launchSms(context, phone, message)
        ContactAction.CALL     -> launchCall(context, phone)
    }
}

// ── Intent helpers ────────────────────────────────────────────────────────────

private const val TAG = "ContactLauncher"

private fun launchWhatsApp(context: Context, phone: String, message: String): ContactActionResult {
    // WhatsApp's URL wants bare digits with the country code, no "+".
    val normalizedPhone = normalizeIndianPhone(phone)
    val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())

    // api.whatsapp.com/send resolves directly inside WhatsApp —
    // wa.me adds an extra HTTP redirect that slows things down.
    val intent = Intent(
        Intent.ACTION_VIEW,
        "https://api.whatsapp.com/send?phone=$normalizedPhone&text=$encoded".toUri()
    ).setPackage("com.whatsapp")

    return try {
        context.startActivity(intent)
        // Arm the accessibility service only once WhatsApp is genuinely on its
        // way to the foreground. Arming beforehand meant a failed launch could
        // leave the flag set, and the service would then fire on whatever
        // conversation the user opened next.
        if (AutoSendPrefs.isAutoSendEnabled(context) && AutoSendPrefs.isServiceEnabled(context)) {
            AutoSendPrefs.armAutoSend(context)
        }
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "WhatsApp not installed", e)
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
        val intent = Intent(Intent.ACTION_VIEW, "sms:${toDialString(phone)}".toUri())
            .putExtra("sms_body", message)
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No SMS app installed", e)
        ContactActionResult.NoHandler
    }
}

/**
 * Sends an SMS directly in the background using [SmsManager].
 *
 * The availability template is bilingual, so it always encodes as UCS-2 and
 * currently splits into 7 billable parts — see [estimateSmsParts]. The count
 * is returned so the caller can tell the worker what the tap actually cost.
 */
private fun sendSmsDirect(context: Context, phone: String, message: String): ContactActionResult {
    // Check the permission explicitly. Relying on the SecurityException alone
    // is unreliable: several OEM ROMs drop the message silently instead.
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return ContactActionResult.SmsPermissionNeeded

    return try {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } ?: return ContactActionResult.SendFailed

        val destination = toDialString(phone)
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
        Log.d(TAG, "SMS auto-sent (${parts.size} part(s), estimated ${estimateSmsParts(message)})")
        ContactActionResult.AutoSent(parts.size)
    } catch (e: SecurityException) {
        Log.w(TAG, "SEND_SMS permission not granted", e)
        ContactActionResult.SmsPermissionNeeded
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send SMS directly", e)
        ContactActionResult.SendFailed
    }
}

private fun launchCall(context: Context, phone: String): ContactActionResult {
    return try {
        // ACTION_DIAL only — no CALL permission required
        val intent = Intent(Intent.ACTION_DIAL, "tel:${toDialString(phone)}".toUri())
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No dialler available", e)
        ContactActionResult.NoHandler
    }
}





