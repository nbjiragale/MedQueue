package com.niranjan.medqueue.contact

import com.niranjan.medqueue.data.settings.AppSettings

// ══════════════════════════════════════════════════════════════════════════════
// MESSAGE TEMPLATES
//
// The availability message used to be a string literal in this package, so the
// only way to reword it was to ship a new build. Shops phrase things their own
// way — and the SMS variant is billed per part, so the shopkeeper has a direct
// financial reason to shorten it. Both templates are now editable in Settings.
// ══════════════════════════════════════════════════════════════════════════════

/**
 * A placeholder the shopkeeper can drop into a template.
 *
 * Kept free of string resources on purpose: this file is pure Kotlin so the
 * rendering can be unit-tested off-device. The UI supplies the labels.
 */
enum class TemplateToken(val token: String) {
    SHOP("{shop}"),
    ADDRESS("{address}"),
    CONTACT("{contact}"),
    CONTACT2("{contact2}"),
    CUSTOMER("{customer}"),
    MEDICINES("{medicines}")
}

/** Stands in for a customer who was saved without a name. */
const val CUSTOMER_FALLBACK = "Customer"

/**
 * The stock WhatsApp template.
 *
 * Deliberately renders byte-for-byte what the app sent before templates
 * existed: an upgrade must not silently change the wording of a message that
 * goes to real customers. `{customer}` and `{medicines}` are available but not
 * used here for the same reason — the old text said "Dear Customer" flatly, so
 * that is what the default still says.
 */
const val DEFAULT_WHATSAPP_TEMPLATE: String =
    "*✅ Your Medicines Are Ready!*\n" +
    "\n" +
    "Dear Customer, the medicines you requested are now in stock and ready for pickup.\n" +
    "\n" +
    "⏰ _Please collect at your earliest convenience._\n" +
    "\n" +
    "Thank you for trusting us with your health. 🙏\n" +
    "*_\"Health is wealth\"_*\n" +
    "——————————————\n" +
    "*✅ ನಿಮ್ಮ ಔಷಧಿಗಳು ಸಿದ್ಧವಾಗಿವೆ!*\n" +
    "\n" +
    "ಪ್ರಿಯ ಗ್ರಾಹಕರೇ, ನೀವು ಕೇಳಿದ ಔಷಧಿಗಳು ಈಗ ಲಭ್ಯವಿದ್ದು, ತೆಗೆದುಕೊಳ್ಳಲು ಸಿದ್ಧವಾಗಿವೆ.\n" +
    "\n" +
    "*_ಆರೋಗ್ಯವೇ ಭಾಗ್ಯ_*\n" +
    "——————————————\n" +
    "🏥 *{shop}*\n" +
    "📍 _{address}_\n" +
    "📞 {contact}\n" +
    "📞 {contact2}\n"

/** The stock SMS template — same words, no WhatsApp markdown. */
const val DEFAULT_SMS_TEMPLATE: String =
    "✅ Your Medicines Are Ready!\n" +
    "\n" +
    "Dear Customer, the medicines you requested are now in stock and ready for pickup.\n" +
    "\n" +
    "⏰ Please collect at your earliest convenience.\n" +
    "\n" +
    "Thank you for trusting us with your health. 🙏\n" +
    "\"Health is wealth\"\n" +
    "——————————————\n" +
    "✅ ನಿಮ್ಮ ಔಷಧಿಗಳು ಸಿದ್ಧವಾಗಿವೆ!\n" +
    "\n" +
    "ಪ್ರಿಯ ಗ್ರಾಹಕರೇ, ನೀವು ಕೇಳಿದ ಔಷಧಿಗಳು ಈಗ ಲಭ್ಯವಿದ್ದು, ತೆಗೆದುಕೊಳ್ಳಲು ಸಿದ್ಧವಾಗಿವೆ.\n" +
    "\n" +
    "ಆರೋಗ್ಯವೇ ಭಾಗ್ಯ\n" +
    "——————————————\n" +
    "🏥 {shop}\n" +
    "📍 {address}\n" +
    "📞 {contact}\n" +
    "📞 {contact2}\n"

/** The pair of templates in force, as edited by the shopkeeper. */
data class MessageTemplates(
    val whatsApp: String = DEFAULT_WHATSAPP_TEMPLATE,
    val sms: String = DEFAULT_SMS_TEMPLATE
) {
    companion object {
        val DEFAULT = MessageTemplates()
    }

    fun forAction(action: ContactAction): String = when (action) {
        ContactAction.SMS -> sms
        // A call sends nothing; the WhatsApp text is only a harmless fallback.
        else              -> whatsApp
    }
}

/**
 * Substitutes [TemplateToken]s into [template].
 *
 * A line whose only substance was a token that resolved to nothing is dropped
 * whole. That is what keeps `📞 {contact2}` from leaving a bare receiver emoji
 * on its own line for the many shops that never fill in a second contact — the
 * behaviour the hand-written builder used to get with an `if`.
 */
fun renderTemplate(
    template: String,
    settings: AppSettings,
    customerName: String = "",
    medicines: List<String> = emptyList()
): String {
    val values: Map<TemplateToken, String> = mapOf(
        TemplateToken.SHOP      to settings.shopName,
        TemplateToken.ADDRESS   to settings.shopAddress,
        TemplateToken.CONTACT   to contactLine(settings.contact1Name, settings.contact1Phone),
        TemplateToken.CONTACT2  to contactLine(settings.contact2Name, settings.contact2Phone),
        TemplateToken.CUSTOMER  to customerName.trim().ifBlank { CUSTOMER_FALLBACK },
        TemplateToken.MEDICINES to medicines.filter { it.isNotBlank() }.joinToString(", ")
    )

    return template.split("\n").mapNotNull { line ->
        var carriedToken = false
        var rendered = line
        for ((token, value) in values) {
            if (rendered.contains(token.token)) {
                carriedToken = true
                rendered = rendered.replace(token.token, value)
            }
        }
        if (carriedToken && rendered.none(Char::isLetterOrDigit)) null else rendered
    }.joinToString("\n")
}

/** "Name: 9876543210", or just the number, or nothing at all. */
private fun contactLine(name: String, phone: String): String = when {
    phone.isBlank() -> ""
    name.isBlank()  -> phone
    else            -> "$name: $phone"
}
