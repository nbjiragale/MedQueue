package com.niranjan.medqueue.contact

/**
 * Phone-number normalisation shared by every outbound channel.
 *
 * Numbers are stored exactly as the shop worker typed them (digits only, as
 * enforced by the form) and normalised here at dispatch time, so the stored
 * value never changes shape underneath the UI.
 */

private const val INDIA_CC = "91"
private const val LOCAL_LENGTH = 10

/**
 * Strips non-digits and ensures the number carries the Indian country code
 * exactly once. Returns digits only — the form WhatsApp's URL expects.
 *
 * Examples:
 *   "9876543210"      → "919876543210"
 *   "+91 98765 43210" → "919876543210"
 *   "09876543210"     → "919876543210"
 *   "0091 9876543210" → "919876543210"
 *   "919876543210"    → "919876543210"
 *
 * Anything unrecognisable is returned as bare digits rather than being
 * mangled — better to hand the dialler something odd than something wrong.
 */
fun normalizeIndianPhone(raw: String): String {
    var digits = raw.filter(Char::isDigit)

    // "00" is the international access prefix spelled out long-hand.
    if (digits.startsWith("00")) digits = digits.drop(2)

    return when {
        digits.length == LOCAL_LENGTH ->
            INDIA_CC + digits

        digits.length == LOCAL_LENGTH + 1 && digits.startsWith("0") ->
            INDIA_CC + digits.drop(1)

        digits.length == LOCAL_LENGTH + INDIA_CC.length && digits.startsWith(INDIA_CC) ->
            digits

        else -> digits
    }
}

/**
 * E.164 form for `sms:` and `tel:` URIs — "+919876543210".
 *
 * Falls back to plain digits when the number isn't a recognisable Indian
 * number, since a bare local number still dials correctly.
 */
fun toDialString(raw: String): String {
    val normalized = normalizeIndianPhone(raw)
    val isCountryCoded = normalized.length == LOCAL_LENGTH + INDIA_CC.length &&
            normalized.startsWith(INDIA_CC)
    return if (isCountryCoded) "+$normalized" else normalized
}

/** True when there are enough digits to be worth handing to another app. */
fun isDialable(raw: String): Boolean =
    raw.count(Char::isDigit) >= LOCAL_LENGTH

/**
 * Human-readable form for the UI — "+91 98765 43210".
 *
 * Grouping matters here because the redesign promotes the phone number to the
 * primary line of every list row, where an unbroken 10-digit run is hard to
 * scan. Anything that isn't a recognisable Indian number is returned as typed.
 */
fun formatForDisplay(raw: String): String {
    val normalized = normalizeIndianPhone(raw)
    val isCountryCoded = normalized.length == LOCAL_LENGTH + INDIA_CC.length &&
            normalized.startsWith(INDIA_CC)
    if (!isCountryCoded) return raw

    val local = normalized.drop(INDIA_CC.length)
    return "+$INDIA_CC ${local.take(5)} ${local.drop(5)}"
}
