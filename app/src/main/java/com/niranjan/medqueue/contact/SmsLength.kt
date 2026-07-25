package com.niranjan.medqueue.contact

/**
 * Works out how many SMS parts a message costs.
 *
 * This matters here because the availability template is bilingual and
 * emoji-heavy: a single character outside the GSM-7 alphabet (any Kannada
 * letter, any emoji) forces the whole message into UCS-2, which cuts the
 * per-part budget from 153 characters to 67. A message that looks like one
 * text bills as seven.
 *
 * [SmsManager.divideMessage] is the authority at send time; this is the pure
 * equivalent so the count can be computed and tested off-device.
 */

/** GSM 03.38 basic alphabet — one septet each. */
private const val GSM7_BASIC =
    "@£\$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?" +
    "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà"

/** GSM 03.38 extension table — two septets each (escape + character). */
private const val GSM7_EXTENDED = "^{}\\[~]|€"

private const val GSM7_SINGLE = 160
private const val GSM7_CONCAT = 153
private const val UCS2_SINGLE = 70
private const val UCS2_CONCAT = 67

/**
 * Septet cost of [message] under GSM-7, or null if any character can't be
 * represented — in which case the message must go out as UCS-2.
 */
private fun gsm7Septets(message: String): Int? {
    var septets = 0
    for (ch in message) {
        when {
            GSM7_BASIC.contains(ch)    -> septets += 1
            GSM7_EXTENDED.contains(ch) -> septets += 2
            else                       -> return null
        }
    }
    return septets
}

/**
 * Number of SMS parts [message] will be split into, and therefore the number
 * of messages the sender is billed for.
 *
 * Slightly approximate for UCS-2 text containing emoji: a real split will not
 * cut a surrogate pair in half, so the carrier may use one more part than this
 * reports on the exact boundary.
 */
fun estimateSmsParts(message: String): Int {
    if (message.isEmpty()) return 1

    val septets = gsm7Septets(message)
    return if (septets != null) {
        if (septets <= GSM7_SINGLE) 1
        else (septets + GSM7_CONCAT - 1) / GSM7_CONCAT
    } else {
        // UCS-2 is billed per UTF-16 code unit, so an emoji costs two.
        val units = message.length
        if (units <= UCS2_SINGLE) 1
        else (units + UCS2_CONCAT - 1) / UCS2_CONCAT
    }
}
