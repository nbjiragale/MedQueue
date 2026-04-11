package com.niranjan.medqueue.contact

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ── Types ─────────────────────────────────────────────────────────────────────

enum class ContactAction { WHATSAPP, SMS, CALL }

sealed class ContactActionResult {
    object Success              : ContactActionResult()
    object InvalidPhone         : ContactActionResult()
    object WhatsAppNotInstalled : ContactActionResult()
    object NoHandler            : ContactActionResult()
}

// ── Message builder (single source of truth) ─────────────────────────────────

/**
 * Builds the standard availability message.
 * All 3 contact actions reuse this — no duplication.
 */
fun buildMessage(customerName: String, medicineName: String): String =
    "Hello $customerName, your $medicineName is now available. Please visit."

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

// ── Intent helpers ────────────────────────────────────────────────────────────

private fun launchWhatsApp(context: Context, phone: String, message: String): ContactActionResult {
    return try {
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val intent = Intent(Intent.ACTION_VIEW, "https://wa.me/91$phone?text=$encoded".toUri())
            .setPackage("com.whatsapp")
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
        ContactActionResult.WhatsAppNotInstalled
    }
}

private fun launchSms(context: Context, phone: String, message: String): ContactActionResult {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, "sms:$phone".toUri())
            .putExtra("sms_body", message)
        context.startActivity(intent)
        ContactActionResult.Success
    } catch (e: ActivityNotFoundException) {
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





