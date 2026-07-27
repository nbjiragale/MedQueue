package com.niranjan.medqueue.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.niranjan.medqueue.MainActivity
import com.niranjan.medqueue.R
import com.niranjan.medqueue.data.settings.AppLocale
import java.util.concurrent.TimeUnit

/**
 * Notification plumbing for the two reminders in the redesign:
 * stale-pending alerts and the end-of-day summary.
 *
 * Every string here is resolved against [AppLocale], not the caller's context:
 * these fire from workers holding the application context, which follows the
 * *phone's* locale and would push English reminders to a shop running the app
 * in Kannada.
 */
object Notifications {

    const val CHANNEL_PENDING = "medqueue_pending_alerts"
    const val CHANNEL_SUMMARY = "medqueue_daily_summary"

    private const val ID_PENDING = 2001
    private const val ID_SUMMARY = 2002

    /**
     * Idempotent — safe to call on every app start and from workers.
     *
     * Re-running it also re-labels the channels in the current language, which
     * is what carries a language change through to the system settings screen.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val localized = AppLocale.wrap(context)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PENDING,
                localized.getString(R.string.channel_pending_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = localized.getString(R.string.channel_pending_desc) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                localized.getString(R.string.channel_summary_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = localized.getString(R.string.channel_summary_desc) }
        )
    }

    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /** [oldestAgeMillis] is how long the longest-waiting request has sat. */
    fun showPendingAlert(context: Context, staleCount: Int, oldestAgeMillis: Long) {
        val localized = AppLocale.wrap(context)
        val text = localized.resources.getQuantityString(
            R.plurals.reminder_pending_body, staleCount, staleCount, describeAge(localized, oldestAgeMillis)
        )
        post(
            context,
            id = ID_PENDING,
            channel = CHANNEL_PENDING,
            title = localized.getString(R.string.reminder_pending_title),
            text = text
        )
    }

    fun showDailySummary(context: Context, pendingCount: Int) {
        val localized = AppLocale.wrap(context)
        val text = localized.resources.getQuantityString(
            R.plurals.reminder_summary_body, pendingCount, pendingCount
        )
        post(
            context,
            id = ID_SUMMARY,
            channel = CHANNEL_SUMMARY,
            title = localized.getString(R.string.reminder_summary_title),
            text = text
        )
    }

    /**
     * "a day" / "3 days", for the sentence the pending alert wraps it in. It
     * lives here rather than in the worker so it is translated alongside the
     * body it is interpolated into.
     */
    private fun describeAge(localized: Context, ageMillis: Long): String {
        val days = (ageMillis / TimeUnit.DAYS.toMillis(1)).toInt()
        return if (days < 2) {
            localized.getString(R.string.age_a_day)
        } else {
            localized.resources.getQuantityString(R.plurals.age_days, days, days)
        }
    }

    private fun post(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!canPost(context)) return
        ensureChannels(context)

        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        // canPost() already gated this, but the platform still wants the check
        // co-located with the notify call.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
