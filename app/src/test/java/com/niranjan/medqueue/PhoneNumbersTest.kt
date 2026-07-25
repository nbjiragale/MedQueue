package com.niranjan.medqueue

import com.niranjan.medqueue.contact.isDialable
import com.niranjan.medqueue.contact.normalizeIndianPhone
import com.niranjan.medqueue.contact.toDialString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `local ten digit number gains country code`() {
        assertEquals("919876543210", normalizeIndianPhone("9876543210"))
    }

    @Test
    fun `formatting characters are stripped`() {
        assertEquals("919876543210", normalizeIndianPhone("+91 98765 43210"))
        assertEquals("919876543210", normalizeIndianPhone("(+91)-98765-43210"))
    }

    @Test
    fun `leading trunk zero is replaced by country code`() {
        assertEquals("919876543210", normalizeIndianPhone("09876543210"))
    }

    @Test
    fun `international access prefix is stripped`() {
        assertEquals("919876543210", normalizeIndianPhone("0091 9876543210"))
    }

    @Test
    fun `already normalised number is left alone`() {
        assertEquals("919876543210", normalizeIndianPhone("919876543210"))
    }

    @Test
    fun `country code is never applied twice`() {
        val once = normalizeIndianPhone("9876543210")
        assertEquals(once, normalizeIndianPhone(once))
    }

    @Test
    fun `unrecognisable input is returned as bare digits rather than mangled`() {
        assertEquals("12345", normalizeIndianPhone("12345"))
        assertEquals("", normalizeIndianPhone(""))
        assertEquals("", normalizeIndianPhone("not a phone number"))
    }

    @Test
    fun `dial string is E164 for recognisable numbers`() {
        assertEquals("+919876543210", toDialString("9876543210"))
        assertEquals("+919876543210", toDialString("+91 98765 43210"))
    }

    @Test
    fun `dial string omits plus when the number is not country coded`() {
        assertEquals("12345", toDialString("12345"))
    }

    @Test
    fun `dialable requires at least a full local number`() {
        assertTrue(isDialable("9876543210"))
        assertTrue(isDialable("+91 98765 43210"))
        assertFalse(isDialable(""))
        assertFalse(isDialable("   "))
        assertFalse(isDialable("98765"))
    }
}
