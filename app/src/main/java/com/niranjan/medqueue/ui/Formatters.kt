package com.niranjan.medqueue.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Relative timestamps for the queue list, matching the mockup's ladder:
 * a clock time for today, "Yesterday", then "N days ago", then a date once
 * counting days stops being useful.
 */
fun formatRelative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val startOfToday = startOfDay(now)
    // Rounded, not truncated: a DST transition makes a calendar day 23 or 25
    // hours long, which would otherwise shift every label by one.
    val days = Math.round((startOfToday - startOfDay(timestamp)).toDouble() / DAY_MS).toInt()

    return when {
        // Future timestamps (clock skew, restored backup) read as "today".
        days <= 0  -> timeFormat().format(Date(timestamp))
        days == 1  -> "Yesterday"
        days < 7   -> "$days days ago"
        else       -> dateFormat().format(Date(timestamp))
    }
}

/** Long form for the detail screen — "25 Jul 2026, 10:12 AM". */
fun formatFull(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))

/** Midnight at the start of the day containing [timestamp]. */
fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun timeFormat() = SimpleDateFormat("hh:mm a", Locale.getDefault())
private fun dateFormat() = SimpleDateFormat("dd MMM", Locale.getDefault())
