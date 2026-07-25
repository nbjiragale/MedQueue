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

/**
 * Notification plumbing for the two reminders in the redesign:
 * stale-pending alerts and the end-of-day summary.
 */
object Notifications {

    const val CHANNEL_PENDING = "medqueue_pending_alerts"
    const val CHANNEL_SUMMARY = "medqueue_daily_summary"

    private const val ID_PENDING = 2001
    private const val ID_SUMMARY = 2002

    /** Idempotent — safe to call on every app start and from workers. */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PENDING,
                context.getString(R.string.channel_pending_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.channel_pending_desc) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                context.getString(R.string.channel_summary_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.channel_summary_desc) }
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

    fun showPendingAlert(context: Context, staleCount: Int, oldestLabel: String) {
        val text = context.resources.getQuantityString(
            R.plurals.reminder_pending_body, staleCount, staleCount, oldestLabel
        )
        post(
            context,
            id = ID_PENDING,
            channel = CHANNEL_PENDING,
            title = context.getString(R.string.reminder_pending_title),
            text = text
        )
    }

    fun showDailySummary(context: Context, pendingCount: Int) {
        val text = context.resources.getQuantityString(
            R.plurals.reminder_summary_body, pendingCount, pendingCount
        )
        post(
            context,
            id = ID_SUMMARY,
            channel = CHANNEL_SUMMARY,
            title = context.getString(R.string.reminder_summary_title),
            text = text
        )
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
