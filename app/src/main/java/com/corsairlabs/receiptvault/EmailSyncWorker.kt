package com.corsairlabs.receiptvault

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic background sync of connected email accounts for paid users.
 *
 * Uses the same [EmailConnectorClient] / [EmailConnectorStore] / [ReceiptStore] logic as the
 * manual "Sync" action in the app, so receipts imported in the background show up the next time
 * the app loads its local store. The Cloudflare Worker enforces plan-based scan limits
 * (Plus: 100, Business: 500 with full Gmail pagination) server-side.
 */
class EmailSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val connectorStore = EmailConnectorStore(context)

        // Background sync is a paid feature. If the user downgraded, stop the periodic job.
        if (connectorStore.currentPlan() == ReceiptVaultPlan.Free) {
            EmailSyncScheduler.cancel(context)
            return Result.success()
        }

        val accounts = connectorStore.loadAccounts().filter {
            it.status == ConnectorStatus.Ready || it.status == ConnectorStatus.SyncReady
        }
        if (accounts.isEmpty()) {
            // Nothing left to sync (all disconnected); stop the periodic job.
            EmailSyncScheduler.cancel(context)
            return Result.success()
        }

        val connectorClient = EmailConnectorClient()
        val receiptStore = ReceiptStore(context)
        var failures = 0

        for (account in accounts) {
            try {
                val summary = connectorClient.syncProvider(account.provider)
                for (receiptJson in summary.receipts) {
                    runCatching { receiptStore.upsert(Receipt.fromJson(receiptJson)) }
                }
                connectorStore.markSyncReady(
                    id = account.id,
                    scanned = summary.scanned,
                    candidates = summary.candidates,
                    imported = summary.imported,
                    message = summary.message
                )
            } catch (_: Exception) {
                // Not signed in, offline, or backend error — retry on the next interval.
                failures++
            }
        }

        return if (failures > 0 && failures == accounts.size) Result.retry() else Result.success()
    }
}
