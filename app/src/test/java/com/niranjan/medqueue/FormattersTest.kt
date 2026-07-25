package com.niranjan.medqueue

import com.niranjan.medqueue.contact.formatForDisplay
import com.niranjan.medqueue.ui.formatRelative
import com.niranjan.medqueue.ui.startOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneDisplayTest {

    @Test
    fun `local number is grouped for display`() {
        assertEquals("+91 98765 43210", formatForDisplay("9876543210"))
    }

    @Test
    fun `already normalised number groups the same way`() {
        assertEquals("+91 98765 43210", formatForDisplay("919876543210"))
        assertEquals("+91 98765 43210", formatForDisplay("+91 98765 43210"))
    }

    @Test
    fun `unrecognisable input is shown as typed`() {
        assertEquals("12345", formatForDisplay("12345"))
        assertEquals("", formatForDisplay(""))
    }
}

class RelativeTimeTest {

    private val now = System.currentTimeMillis()
    private val todayStart = startOfDay(now)
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `today shows a clock time`() {
        // Not asserting the exact format — it is locale-dependent — only that
        // it is a time rather than one of the relative labels.
        val label = formatRelative(now, now)
        assertTrue("Expected a clock time, got '$label'", label.contains(":"))
    }

    @Test
    fun `the instant before midnight is yesterday`() {
        assertEquals("Yesterday", formatRelative(todayStart - 1, now))
    }

    @Test
    fun `two and three days back count days`() {
        assertEquals("2 days ago", formatRelative(todayStart - day - 1, now))
        assertEquals("3 days ago", formatRelative(todayStart - 2 * day - 1, now))
    }

    @Test
    fun `a week back falls through to a date`() {
        val label = formatRelative(todayStart - 8 * day, now)
        assertTrue("Expected a date, got '$label'", !label.contains("ago"))
        assertTrue("Expected a date, got '$label'", label != "Yesterday")
    }

    @Test
    fun `a future timestamp does not render as negative days`() {
        val label = formatRelative(now + day, now)
        assertTrue("Expected a clock time, got '$label'", label.contains(":"))
    }
}
