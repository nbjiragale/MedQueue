package com.niranjan.medqueue.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.niranjan.medqueue.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Relative timestamps for the queue list, matching the mockup's ladder:
 * a clock time for today, "Yesterday", then "N days ago", then a date once
 * counting days stops being useful.
 *
 * The two word-labels arrive as parameters so this stays a plain function that
 * a unit test can call; UI code goes through [relativeTime], which fills them
 * in from the shop's chosen language.
 */
fun formatRelative(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
    yesterday: String = "Yesterday",
    daysAgo: (days: Int) -> String = { "$it days ago" }
): String {
    val startOfToday = startOfDay(now)
    // Rounded, not truncated: a DST transition makes a calendar day 23 or 25
    // hours long, which would otherwise shift every label by one.
    val days = Math.round((startOfToday - startOfDay(timestamp)).toDouble() / DAY_MS).toInt()

    return when {
        // Future timestamps (clock skew, restored backup) read as "today".
        days <= 0  -> timeFormat().format(Date(timestamp))
        days == 1  -> yesterday
        days < 7   -> daysAgo(days)
        else       -> dateFormat().format(Date(timestamp))
    }
}

/** [formatRelative] with the labels translated into the current app language. */
@Composable
fun relativeTime(timestamp: Long): String {
    val resources = LocalContext.current.resources
    return formatRelative(
        timestamp = timestamp,
        yesterday = stringResource(R.string.relative_yesterday),
        daysAgo = { days -> resources.getQuantityString(R.plurals.relative_days_ago, days, days) }
    )
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
