package com.corsairlabs.receiptvault

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic background email sync for paid (Plus/Business) users.
 * Free users sync manually from the Email tab; the Worker enforces plan limits server-side.
 */
object EmailSyncScheduler {
    private const val WORK_NAME = "receiptvault_email_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<EmailSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
