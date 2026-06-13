package com.corsairlabs.receiptvault

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.YearMonth
import java.util.UUID

enum class EmailProvider(
    val label: String,
    val scopeLabel: String,
    val receiptQuery: String,
    val liveSyncAvailable: Boolean,
    val unavailableMessage: String = ""
) {
    Gmail(
        "Gmail",
        "gmail.readonly",
        "newer_than:90d (receipt OR order OR invoice OR bill OR statement OR warranty)",
        true
    ),
    Outlook(
        "Outlook",
        "Microsoft Graph Mail.Read delegated",
        "receipt OR order OR invoice OR bill OR statement OR warranty",
        true
    ),
    Yahoo(
        "Yahoo",
        "Yahoo OAuth + IMAP read",
        "receipt OR order OR invoice OR bill OR statement OR warranty",
        false,
        "Yahoo live sync is not available in this build."
    ),
    Imap(
        "Other IMAP",
        "OAuth/IMAP read",
        "receipt OR order OR invoice OR bill OR statement OR warranty",
        false,
        "Manual IMAP live sync is not available in this build."
    )
}

enum class ReceiptVaultPlan(
    val label: String,
    val maxEmailAccounts: Int,
    val monthlyEmailImports: Int
) {
    Free("Free", 1, 10),
    Plus("Plus", 3, 250),
    Business("Business", 10, 1000)
}

enum class ConnectorStatus(val label: String) {
    OAuthPending("OAuth setup pending"),
    Ready("Ready"),
    SyncReady("Purchase-document sync ready"),
    Disconnected("Disconnected")
}

data class EmailConnectorAccount(
    val id: String,
    val provider: EmailProvider,
    val emailAddress: String,
    val status: ConnectorStatus,
    val monthlyImportCount: Int,
    val monthlyImportLimit: Int,
    val monthlyImportPeriod: String,
    val lastSyncMillis: Long?,
    val lastMessage: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("provider", provider.name)
        .put("emailAddress", emailAddress)
        .put("status", status.name)
        .put("monthlyImportCount", monthlyImportCount)
        .put("monthlyImportLimit", monthlyImportLimit)
        .put("monthlyImportPeriod", monthlyImportPeriod)
        .put("lastSyncMillis", lastSyncMillis)
        .put("lastMessage", lastMessage)

    companion object {
        fun fromJson(json: JSONObject): EmailConnectorAccount {
            val currentPeriod = currentImportPeriod()
            val storedPeriod = json.optString("monthlyImportPeriod", currentPeriod).ifBlank { currentPeriod }
            return EmailConnectorAccount(
                id = json.optString("id", UUID.randomUUID().toString()),
                provider = runCatching {
                    EmailProvider.valueOf(json.optString("provider", EmailProvider.Gmail.name))
                }.getOrDefault(EmailProvider.Gmail),
                emailAddress = json.optString("emailAddress", "Email account"),
                status = runCatching {
                    ConnectorStatus.valueOf(json.optString("status", ConnectorStatus.OAuthPending.name))
                }.getOrDefault(ConnectorStatus.OAuthPending),
                monthlyImportCount = if (storedPeriod == currentPeriod) json.optInt("monthlyImportCount", 0) else 0,
                monthlyImportLimit = json.optInt("monthlyImportLimit", ReceiptVaultPlan.Free.monthlyEmailImports),
                monthlyImportPeriod = currentPeriod,
                lastSyncMillis = if (json.isNull("lastSyncMillis") || !json.has("lastSyncMillis")) null else json.optLong("lastSyncMillis"),
                lastMessage = json.optString("lastMessage", "Waiting for OAuth setup")
            )
        }
    }
}

data class ConnectorStoreResult(
    val accounts: List<EmailConnectorAccount>,
    val message: String
)

data class ImapManualConfig(
    val emailAddress: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val useTls: Boolean
)

data class ConnectorSyncSummary(
    val scanned: Int,
    val candidates: Int,
    val imported: Int,
    val message: String,
    val ok: Boolean = true,
    val status: String = "",
    val error: String = "",
    val monthlyImportLimit: Int? = null,
    val monthlyImportUsed: Int? = null,
    val monthlyImportRemaining: Int? = null,
    val receipts: List<org.json.JSONObject> = emptyList()
)

class EmailConnectorStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("receiptvault_email_connectors", Context.MODE_PRIVATE)

    fun currentPlan(): ReceiptVaultPlan = receiptVaultPlanFromActiveProducts(context)

    fun loadAccounts(): List<EmailConnectorAccount> {
        val raw = prefs.getString("accounts", "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(EmailConnectorAccount.fromJson(array.getJSONObject(index)))
            }
        }
    }

    fun canAddAccount(): Boolean {
        val plan = currentPlan()
        val accounts = loadAccounts()
        val activeCount = accounts.count { it.status != ConnectorStatus.Disconnected }
        return activeCount < plan.maxEmailAccounts
    }

    fun connect(
        provider: EmailProvider,
        emailAddress: String = "${provider.label} account",
        status: ConnectorStatus = ConnectorStatus.OAuthPending,
        lastMessage: String = "Add ${provider.scopeLabel} OAuth credentials to enable live mailbox sync."
    ): ConnectorStoreResult {
        val plan = currentPlan()
        val accounts = loadAccounts()
        if (!canAddAccount()) {
            return ConnectorStoreResult(
                accounts,
                "${plan.label} allows ${plan.maxEmailAccounts} connected email account."
            )
        }

        val account = EmailConnectorAccount(
            id = UUID.randomUUID().toString(),
            provider = provider,
            emailAddress = emailAddress,
            status = status,
            monthlyImportCount = 0,
            monthlyImportLimit = plan.monthlyEmailImports,
            monthlyImportPeriod = currentImportPeriod(),
            lastSyncMillis = null,
            lastMessage = lastMessage
        )
        val updated = accounts + account
        saveAccounts(updated)
        return ConnectorStoreResult(updated, "${provider.label} connector added.")
    }

    fun markSyncReady(
        id: String,
        scanned: Int = 0,
        candidates: Int = 0,
        imported: Int = 0,
        monthlyImportUsed: Int? = null,
        monthlyImportLimit: Int? = null,
        message: String = "Purchase-document sync check completed."
    ): ConnectorStoreResult {
        val period = currentImportPeriod()
        val planLimit = currentPlan().monthlyEmailImports
        val updated = loadAccounts().map { account ->
            if (account.id == id) {
                account.copy(
                    status = ConnectorStatus.SyncReady,
                    lastSyncMillis = System.currentTimeMillis(),
                    monthlyImportCount = monthlyImportUsed ?: (account.monthlyImportCount + imported),
                    monthlyImportLimit = monthlyImportLimit ?: planLimit,
                    monthlyImportPeriod = period,
                    lastMessage = message.ifBlank {
                        "Scanned $scanned messages, found $candidates purchase-document candidates, imported $imported."
                    }
                )
            } else {
                account
            }
        }
        saveAccounts(updated)
        return ConnectorStoreResult(updated, message)
    }

    fun markSyncFailed(
        id: String,
        message: String
    ): ConnectorStoreResult {
        val updated = loadAccounts().map { account ->
            if (account.id == id) {
                account.copy(
                    status = if (account.status == ConnectorStatus.Disconnected) account.status else ConnectorStatus.Ready,
                    lastSyncMillis = System.currentTimeMillis(),
                    lastMessage = message
                )
            } else {
                account
            }
        }
        saveAccounts(updated)
        return ConnectorStoreResult(updated, message)
    }

    fun disconnect(id: String): ConnectorStoreResult {
        val updated = loadAccounts().map { account ->
            if (account.id == id) {
                account.copy(
                    status = ConnectorStatus.Disconnected,
                    lastMessage = "Account disconnected. Imported receipts remain until deleted."
                )
            } else {
                account
            }
        }
        saveAccounts(updated)
        return ConnectorStoreResult(updated, "Email account disconnected.")
    }

    fun deleteAccountData(id: String): ConnectorStoreResult {
        val updated = loadAccounts().filterNot { it.id == id }
        saveAccounts(updated)
        return ConnectorStoreResult(updated, "Connector and imported email metadata deleted.")
    }

    /**
     * Merges remote account data (from the Worker's /v1/connectors/accounts) into local
     * SharedPreferences. Accounts that are connected on the server get their email address
     * and status updated. Local accounts whose server token was deleted get marked Disconnected.
     */
    fun syncFromRemote(remoteAccounts: List<RemoteConnectorAccount>): List<EmailConnectorAccount> {
        val local = loadAccounts().toMutableList()
        val remoteByProvider = remoteAccounts.associateBy { it.provider }

        // Update existing local entries with real email + Ready status
        val updated = local.map { account ->
            val remote = remoteByProvider[account.provider]
            if (remote != null && remote.connected && account.status != ConnectorStatus.Disconnected) {
                account.copy(
                    emailAddress = remote.emailAddress ?: account.emailAddress,
                    status = ConnectorStatus.Ready,
                    lastMessage = "Connected. Tap Sync to import receipts."
                )
            } else {
                account
            }
        }.toMutableList()

        // Add any remote accounts that don't have a local entry yet
        val localProviders = updated.map { it.provider }.toSet()
        val plan = currentPlan()
        for (remote in remoteAccounts) {
            if (remote.connected && remote.provider !in localProviders) {
                updated.add(EmailConnectorAccount(
                    id = java.util.UUID.randomUUID().toString(),
                    provider = remote.provider,
                    emailAddress = remote.emailAddress ?: "${remote.provider.label} account",
                    status = ConnectorStatus.Ready,
                    monthlyImportCount = 0,
                    monthlyImportLimit = plan.monthlyEmailImports,
                    monthlyImportPeriod = currentImportPeriod(),
                    lastSyncMillis = null,
                    lastMessage = "Connected. Tap Sync to import receipts."
                ))
            }
        }

        saveAccounts(updated)
        return updated
    }

    private fun saveAccounts(accounts: List<EmailConnectorAccount>) {
        prefs.edit()
            .putString("accounts", JSONArray(accounts.map { it.toJson() }).toString())
            .apply()
    }
}

fun currentImportPeriod(): String = YearMonth.now().toString()
