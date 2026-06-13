package com.corsairlabs.receiptvault

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic background sync of connected email connectors for paid users.
 *
 * Uses the same [EmailConnectorClient] / [EmailConnectorStore] / [ReceiptStore] logic as the
 * manual "Sync" action in the app, so receipts imported in the background show up the next time
 * the app loads its local store. The Cloudflare Worker enforces plan-based monthly import
 * limits server-side.
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
                // Skip emails imported on a previous sync (same dedup as the manual
                // sync path) so background syncs don't resurrect deleted receipts.
                var importedNow = 0
                for (receiptJson in summary.receipts) {
                    runCatching {
                        val receipt = Receipt.fromJson(receiptJson)
                        val emailKey = receipt.emailMessageId ?: receipt.id
                        if (!receiptStore.isEmailImported(emailKey)) {
                            receiptStore.upsert(receipt)
                            receiptStore.markEmailImported(emailKey)
                            importedNow++
                        }
                    }
                }
                val syncMessage = when {
                    summary.status == "import_limit_reached" -> summary.message
                    summary.imported > 0 && importedNow == 0 -> "No new purchase documents to import."
                    else -> summary.message
                }
                connectorStore.markSyncReady(
                    id = account.id,
                    scanned = summary.scanned,
                    candidates = summary.candidates,
                    imported = importedNow,
                    monthlyImportUsed = summary.monthlyImportUsed,
                    monthlyImportLimit = summary.monthlyImportLimit,
                    message = syncMessage
                )
            } catch (error: Exception) {
                // Transient sync failures retry on the next interval; product limitations do not.
                val detail = error.message?.takeIf { it.isNotBlank() } ?: "unknown backend error"
                connectorStore.markSyncFailed(
                    id = account.id,
                    message = "Could not reach connector sync: $detail."
                )
                if (!detail.contains("not available in this build", ignoreCase = true)) {
                    failures++
                }
            }
        }

        return if (failures > 0 && failures == accounts.size) Result.retry() else Result.success()
    }
}
