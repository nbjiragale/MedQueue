package com.niranjan.medqueue.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.niranjan.medqueue.data.local.AppDatabase
import com.niranjan.medqueue.data.settings.SettingsPrefs
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════════════════════════════════════════════
// Workers
// ══════════════════════════════════════════════════════════════════════════════

/** Fires when a request has sat unactioned for 24 hours or more. */
class PendingAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = SettingsPrefs(applicationContext)
        if (!prefs.pendingAlertsEnabled()) return Result.success()

        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(STALE_AFTER_HOURS)
        val stale = AppDatabase.getInstance(applicationContext)
            .requestDao()
            .getStale(cutoff = cutoff)

        if (stale.isNotEmpty()) {
            Notifications.showPendingAlert(
                applicationContext,
                staleCount = stale.size,
                oldestAgeMillis = System.currentTimeMillis() - stale.first().createdAt
            )
        }
        return Result.success()
    }

    companion object {
        const val STALE_AFTER_HOURS = 24L
    }
}

/** End-of-day count of everything still outstanding. */
class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = SettingsPrefs(applicationContext)
        if (!prefs.dailySummaryEnabled()) return Result.success()

        val pending = AppDatabase.getInstance(applicationContext)
            .requestDao()
            .countByStatus()

        // Nothing outstanding is not worth interrupting the shop for.
        if (pending > 0) {
            Notifications.showDailySummary(applicationContext, pending)
        }
        return Result.success()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Scheduling
// ══════════════════════════════════════════════════════════════════════════════

object Reminders {

    private const val WORK_PENDING = "medqueue_pending_alerts"
    private const val WORK_SUMMARY = "medqueue_daily_summary"

    /** Hour of day the summary fires. 20:00 — after a typical shop closes. */
    private const val SUMMARY_HOUR = 20

    /**
     * Brings scheduled work in line with the current toggles. Call on app start
     * and whenever a reminder switch changes; both paths are idempotent.
     */
    fun sync(context: Context) {
        val prefs = SettingsPrefs(context)
        Notifications.ensureChannels(context)

        val work = WorkManager.getInstance(context)

        if (prefs.pendingAlertsEnabled()) {
            work.enqueueUniquePeriodicWork(
                WORK_PENDING,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PendingAlertWorker>(6, TimeUnit.HOURS).build()
            )
        } else {
            work.cancelUniqueWork(WORK_PENDING)
        }

        if (prefs.dailySummaryEnabled()) {
            work.enqueueUniquePeriodicWork(
                WORK_SUMMARY,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(millisUntilNextSummary(), TimeUnit.MILLISECONDS)
                    .build()
            )
        } else {
            work.cancelUniqueWork(WORK_SUMMARY)
        }
    }

    /** Delay until the next [SUMMARY_HOUR], rolling to tomorrow if it has passed. */
    private fun millisUntilNextSummary(): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, SUMMARY_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
