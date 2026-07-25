package com.niranjan.medqueue

import com.niranjan.medqueue.contact.buildSmsMessage
import com.niranjan.medqueue.contact.estimateSmsParts
import com.niranjan.medqueue.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsLengthTest {

    @Test
    fun `short ascii message is a single part`() {
        assertEquals(1, estimateSmsParts("Your medicines are ready."))
        assertEquals(1, estimateSmsParts(""))
    }

    @Test
    fun `gsm7 boundary is 160 characters`() {
        assertEquals(1, estimateSmsParts("a".repeat(160)))
        assertEquals(2, estimateSmsParts("a".repeat(161)))
        assertEquals(2, estimateSmsParts("a".repeat(306)))
        assertEquals(3, estimateSmsParts("a".repeat(307)))
    }

    @Test
    fun `extension table characters cost two septets`() {
        // 80 braces = 160 septets = still one part, but 81 tips it over.
        assertEquals(1, estimateSmsParts("{".repeat(80)))
        assertEquals(2, estimateSmsParts("{".repeat(81)))
    }

    @Test
    fun `a single non-gsm7 character forces ucs2 for the whole message`() {
        val ascii = "a".repeat(100)
        assertEquals(1, estimateSmsParts(ascii))
        // One Kannada letter drops the budget from 160 to 70.
        assertTrue(estimateSmsParts(ascii + "ಔ") > 1)
    }

    @Test
    fun `ucs2 boundary is 70 characters`() {
        assertEquals(1, estimateSmsParts("ಔ".repeat(70)))
        assertEquals(2, estimateSmsParts("ಔ".repeat(71)))
    }

    @Test
    fun `the real availability template is expensive`() {
        val settings = AppSettings(
            shopName      = "Ainapur Medicals",
            shopAddress   = "Main Road, Ainapur",
            contact1Name  = "Yogesh",
            contact1Phone = "9876543210",
            contact2Name  = "Niranjan",
            contact2Phone = "9876543211"
        )

        val parts = estimateSmsParts(buildSmsMessage(settings))

        // Documents the cost rather than asserting a target. The template is
        // bilingual and emoji-heavy, so it encodes as UCS-2 and every "send
        // SMS" tap currently bills as 7 messages. If this test starts failing
        // because the count dropped, that is good news — update the bound.
        assertTrue(
            "Expected the bilingual template to cost several SMS parts, got $parts",
            parts >= 6
        )
    }
}
