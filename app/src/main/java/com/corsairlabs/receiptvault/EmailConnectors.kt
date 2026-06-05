package com.corsairlabs.receiptvault

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class EmailProvider(
    val label: String,
    val scopeLabel: String,
    val receiptQuery: String
) {
    Gmail(
        "Gmail",
        "gmail.readonly",
        "newer_than:90d (receipt OR order OR invoice OR \"purchase confirmation\" OR warranty)"
    ),
    Outlook(
        "Outlook",
        "Microsoft Graph Mail.Read delegated",
        "receipt OR order OR invoice OR purchase confirmation OR warranty"
    ),
    Yahoo(
        "Yahoo",
        "Yahoo OAuth + IMAP read",
        "receipt OR order OR invoice OR purchase confirmation OR warranty"
    ),
    Imap(
        "Other IMAP",
        "OAuth/IMAP read",
        "receipt OR order OR invoice OR purchase confirmation OR warranty"
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
    SyncReady("Receipt-only sync ready"),
    Disconnected("Disconnected")
}

data class EmailConnectorAccount(
    val id: String,
    val provider: EmailProvider,
    val emailAddress: String,
    val status: ConnectorStatus,
    val monthlyImportCount: Int,
    val monthlyImportLimit: Int,
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
        .put("lastSyncMillis", lastSyncMillis)
        .put("lastMessage", lastMessage)

    companion object {
        fun fromJson(json: JSONObject): EmailConnectorAccount = EmailConnectorAccount(
            id = json.optString("id", UUID.randomUUID().toString()),
            provider = runCatching {
                EmailProvider.valueOf(json.optString("provider", EmailProvider.Gmail.name))
            }.getOrDefault(EmailProvider.Gmail),
            emailAddress = json.optString("emailAddress", "Email account"),
            status = runCatching {
                ConnectorStatus.valueOf(json.optString("status", ConnectorStatus.OAuthPending.name))
            }.getOrDefault(ConnectorStatus.OAuthPending),
            monthlyImportCount = json.optInt("monthlyImportCount", 0),
            monthlyImportLimit = json.optInt("monthlyImportLimit", ReceiptVaultPlan.Free.monthlyEmailImports),
            lastSyncMillis = if (json.isNull("lastSyncMillis") || !json.has("lastSyncMillis")) null else json.optLong("lastSyncMillis"),
            lastMessage = json.optString("lastMessage", "Waiting for OAuth setup")
        )
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

class EmailConnectorStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("receiptvault_email_connectors", Context.MODE_PRIVATE)

    fun currentPlan(): ReceiptVaultPlan = ReceiptVaultPlan.Free

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
            lastSyncMillis = null,
            lastMessage = lastMessage
        )
        val updated = accounts + account
        saveAccounts(updated)
        return ConnectorStoreResult(updated, "${provider.label} connector added.")
    }

    fun markSyncReady(id: String): ConnectorStoreResult {
        val updated = loadAccounts().map { account ->
            if (account.id == id) {
                account.copy(
                    status = ConnectorStatus.SyncReady,
                    lastSyncMillis = System.currentTimeMillis(),
                    lastMessage = "Receipt/order-only query is ready. Live import starts after OAuth approval."
                )
            } else {
                account
            }
        }
        saveAccounts(updated)
        return ConnectorStoreResult(updated, "Receipt-only sync check completed.")
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

    private fun saveAccounts(accounts: List<EmailConnectorAccount>) {
        prefs.edit()
            .putString("accounts", JSONArray(accounts.map { it.toJson() }).toString())
            .apply()
    }
}
