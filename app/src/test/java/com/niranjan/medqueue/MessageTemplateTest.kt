package com.niranjan.medqueue

import com.niranjan.medqueue.contact.CUSTOMER_FALLBACK
import com.niranjan.medqueue.contact.DEFAULT_SMS_TEMPLATE
import com.niranjan.medqueue.contact.DEFAULT_WHATSAPP_TEMPLATE
import com.niranjan.medqueue.contact.ContactAction
import com.niranjan.medqueue.contact.MessageTemplates
import com.niranjan.medqueue.contact.buildMessage
import com.niranjan.medqueue.contact.renderTemplate
import com.niranjan.medqueue.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTemplateTest {

    private val fullShop = AppSettings(
        shopName      = "Ainapur Medical Store",
        shopAddress   = "Main Road, Ainapur",
        contact1Name  = "Yogesh",
        contact1Phone = "9876543210",
        contact2Name  = "Niranjan",
        contact2Phone = "9123456780"
    )

    // ── Substitution ────────────────────────────────────────────────────────

    @Test
    fun `shop tokens are substituted`() {
        val out = renderTemplate("{shop} — {address}", fullShop)
        assertEquals("Ainapur Medical Store — Main Road, Ainapur", out)
    }

    @Test
    fun `contact renders as name and number`() {
        assertEquals("Yogesh: 9876543210", renderTemplate("{contact}", fullShop))
    }

    @Test
    fun `contact falls back to the bare number when unnamed`() {
        val shop = fullShop.copy(contact1Name = "")
        assertEquals("9876543210", renderTemplate("{contact}", shop))
    }

    @Test
    fun `customer name is used when present`() {
        assertEquals("Dear Ramesh", renderTemplate("Dear {customer}", fullShop, "Ramesh"))
    }

    @Test
    fun `customer falls back when the request has no name`() {
        assertEquals(
            "Dear $CUSTOMER_FALLBACK",
            renderTemplate("Dear {customer}", fullShop, "   ")
        )
    }

    @Test
    fun `medicines join with commas and drop blanks`() {
        val out = renderTemplate(
            "{medicines}", fullShop,
            medicines = listOf("Paracetamol 650mg", "  ", "Azithromycin 500")
        )
        assertEquals("Paracetamol 650mg, Azithromycin 500", out)
    }

    // ── The line-drop rule ──────────────────────────────────────────────────

    @Test
    fun `a line whose only content was an empty token is dropped whole`() {
        val shop = fullShop.copy(contact2Name = "", contact2Phone = "")
        val out = renderTemplate("📞 {contact}\n📞 {contact2}\n", shop)
        assertEquals("📞 Yogesh: 9876543210\n", out)
    }

    @Test
    fun `a line keeps its own words even when a token is empty`() {
        val shop = fullShop.copy(shopAddress = "")
        val out = renderTemplate("Visit {shop} at {address}", shop)
        assertTrue(out.startsWith("Visit Ainapur Medical Store at"))
    }

    @Test
    fun `lines without tokens are never dropped`() {
        assertEquals("——————————————", renderTemplate("——————————————", fullShop))
    }

    // ── Defaults must not change what customers receive ─────────────────────

    @Test
    fun `default whatsapp template names the shop and both contacts`() {
        val out = renderTemplate(DEFAULT_WHATSAPP_TEMPLATE, fullShop)
        assertTrue(out.contains("🏥 *Ainapur Medical Store*"))
        assertTrue(out.contains("📞 Yogesh: 9876543210"))
        assertTrue(out.contains("📞 Niranjan: 9123456780"))
        assertTrue(out.endsWith("\n"))
    }

    @Test
    fun `default whatsapp template omits the second contact line when unset`() {
        val shop = fullShop.copy(contact2Name = "", contact2Phone = "")
        val out = renderTemplate(DEFAULT_WHATSAPP_TEMPLATE, shop)
        assertFalse(out.contains("Niranjan"))
        // Exactly one receiver line survives — no orphaned emoji.
        assertEquals(1, out.lines().count { it.startsWith("📞") })
    }

    @Test
    fun `default sms template carries no whatsapp markdown`() {
        val out = renderTemplate(DEFAULT_SMS_TEMPLATE, fullShop)
        assertFalse(out.contains("*"))
        assertFalse(out.contains("_"))
    }

    // ── Dispatch ────────────────────────────────────────────────────────────

    @Test
    fun `buildMessage picks the sms template for sms`() {
        val templates = MessageTemplates(whatsApp = "WA", sms = "SMS")
        assertEquals("SMS", buildMessage(fullShop, ContactAction.SMS, templates))
        assertEquals("WA", buildMessage(fullShop, ContactAction.WHATSAPP, templates))
    }

    @Test
    fun `buildMessage defaults to the stock wording`() {
        assertEquals(
            renderTemplate(DEFAULT_WHATSAPP_TEMPLATE, fullShop),
            buildMessage(fullShop)
        )
    }

    @Test
    fun `a shopkeeper template can use tokens the default does not`() {
        val templates = MessageTemplates(whatsApp = "{customer}, {medicines} at {shop}.")
        val out = buildMessage(
            settings = fullShop,
            action = ContactAction.WHATSAPP,
            templates = templates,
            customerName = "Ramesh",
            medicines = listOf("Paracetamol 650mg")
        )
        assertEquals("Ramesh, Paracetamol 650mg at Ainapur Medical Store.", out)
    }
}
