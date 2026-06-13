package com.corsairlabs.receiptvault

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RemoteConnectorAccount(
    val provider: EmailProvider,
    val emailAddress: String?,
    val connected: Boolean
)

data class DownloadedEmailAttachment(
    val bytes: ByteArray,
    val contentType: String
)

class EmailConnectorClient {
    private val auth = FirebaseAuth.getInstance()
    private val apiBase = BuildConfig.R2_BACKUP_API_URL

    suspend fun startOAuth(provider: EmailProvider): String? {
        val token = firebaseToken()
        return postStart(token, provider)
    }

    suspend fun deleteAccount(provider: EmailProvider): Boolean {
        return runCatching {
            val token = firebaseToken()
            deleteConnectorToken(token, provider)
        }.getOrDefault(false)
    }

    suspend fun registerManualImap(config: ImapManualConfig): Boolean {
        return runCatching {
            val token = firebaseToken()
            postManualImap(token, config)
        }.getOrDefault(false)
    }

    suspend fun fetchRemoteAccounts(): List<RemoteConnectorAccount> {
        return runCatching {
            val token = firebaseToken()
            getRemoteAccounts(token)
        }.getOrDefault(emptyList())
    }

    suspend fun syncProvider(provider: EmailProvider): ConnectorSyncSummary {
        val token = firebaseToken()
        return postSync(token, provider)
    }

    suspend fun downloadAttachment(storageKey: String): DownloadedEmailAttachment {
        val token = firebaseToken()
        return getAttachment(token, storageKey)
    }

    private suspend fun firebaseToken(): String {
        val user = auth.currentUser ?: throw IOException("Please sign in to connect email accounts.")
        val token = user
            .getIdToken(false)
            .await()
            .token
        return token ?: throw IOException("Firebase token unavailable")
    }

    private suspend fun postStart(token: String, provider: EmailProvider): String? = withContext(Dispatchers.IO) {
        val connection = (URL("$apiBase/v1/connectors/oauth/start").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put("provider", provider.providerId)
            .put("returnUrl", "receiptvault://connectors")
            .toString()
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) throw IOException(httpErrorMessage(connection.responseCode, response))
        JSONObject(response).optString("authorizationUrl", "").takeIf { it.isNotBlank() }
    }

    private suspend fun postManualImap(token: String, config: ImapManualConfig): Boolean = withContext(Dispatchers.IO) {
        val connection = (URL("$apiBase/v1/connectors/imap/manual").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put("emailAddress", config.emailAddress)
            .put("host", config.host)
            .put("port", config.port)
            .put("username", config.username)
            .put("password", config.password)
            .put("useTls", config.useTls)
            .toString()
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) throw IOException(httpErrorMessage(connection.responseCode, response))
        true
    }

    private suspend fun postSync(token: String, provider: EmailProvider): ConnectorSyncSummary = withContext(Dispatchers.IO) {
        val connection = (URL("$apiBase/v1/connectors/sync").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            // Paid plans can scan deeper mailbox history with pagination, which takes longer.
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }

        // No maxCandidates: the Worker applies the plan-based monthly import limit
        // (Free: 10, Plus: 250, Business: 1000).
        val body = JSONObject()
            .put("provider", provider.providerId)
            .toString()
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) throw IOException(httpErrorMessage(connection.responseCode, response))

        val reports = JSONObject(response).optJSONArray("reports")
        val report = reports?.optJSONObject(0)
            ?: throw IOException("connector sync returned no report")
        val ok = report.optBoolean("ok", false)
        val status = report.optString("status", "")
        val apiError = report.optString("error", "")
        val message = report.optString("message", "Purchase-document sync check completed.")
        if (!ok) {
            throw IOException(connectorReportErrorMessage(apiError, message))
        }
        val receiptsArray = report?.optJSONArray("receipts")
        val receiptsList = buildList {
            if (receiptsArray != null) {
                for (i in 0 until receiptsArray.length()) {
                    runCatching { add(receiptsArray.getJSONObject(i)) }
                }
            }
        }
        ConnectorSyncSummary(
            scanned = report.optInt("scanned", 0),
            candidates = report.optInt("candidates", 0),
            imported = report.optInt("imported", 0),
            message = message,
            ok = ok,
            status = status,
            error = apiError,
            monthlyImportLimit = report.optionalInt("monthlyImportLimit"),
            monthlyImportUsed = report.optionalInt("monthlyImportUsed"),
            monthlyImportRemaining = report.optionalInt("monthlyImportRemaining"),
            receipts = receiptsList
        )
    }

    private suspend fun getRemoteAccounts(token: String): List<RemoteConnectorAccount> = withContext(Dispatchers.IO) {
        val connection = (URL("$apiBase/v1/connectors/accounts").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) return@withContext emptyList()

        val accounts = JSONObject(response).optJSONArray("accounts") ?: JSONArray()
        buildList {
            for (i in 0 until accounts.length()) {
                val obj = accounts.getJSONObject(i)
                val providerStr = obj.optString("provider", "")
                val provider = providerIdToEnum(providerStr) ?: continue
                add(RemoteConnectorAccount(
                    provider = provider,
                    emailAddress = obj.optString("emailAddress", "").takeIf { it.isNotBlank() },
                    connected = obj.optBoolean("connected", false)
                ))
            }
        }
    }

    private suspend fun getAttachment(token: String, storageKey: String): DownloadedEmailAttachment = withContext(Dispatchers.IO) {
        val encodedKey = URLEncoder.encode(storageKey, Charsets.UTF_8.name())
        val connection = (URL("$apiBase/v1/receipts/attachments?key=$encodedKey").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 60000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        if (connection.responseCode !in 200..299) {
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IOException(httpErrorMessage(connection.responseCode, response))
        }
        val bytes = stream.use { input -> input?.readBytes() ?: ByteArray(0) }
        if (bytes.isEmpty()) throw IOException("Attachment was empty")
        DownloadedEmailAttachment(
            bytes = bytes,
            contentType = connection.contentType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        )
    }

    private fun providerIdToEnum(id: String): EmailProvider? = when (id) {
        "gmail" -> EmailProvider.Gmail
        "outlook" -> EmailProvider.Outlook
        "yahoo" -> EmailProvider.Yahoo
        "imap" -> EmailProvider.Imap
        else -> null
    }

    private suspend fun deleteConnectorToken(token: String, provider: EmailProvider): Boolean = withContext(Dispatchers.IO) {
        val connection = (URL("$apiBase/v1/connectors/accounts/${provider.providerId}").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        stream?.close()
        connection.responseCode in 200..299
    }

    private val EmailProvider.providerId: String
        get() = when (this) {
            EmailProvider.Gmail -> "gmail"
            EmailProvider.Outlook -> "outlook"
            EmailProvider.Yahoo -> "yahoo"
            EmailProvider.Imap -> "imap"
        }

    private fun httpErrorMessage(statusCode: Int, response: String): String {
        val apiError = runCatching {
            JSONObject(response).optString("error", "")
        }.getOrDefault("").takeIf { it.isNotBlank() }

        return when (apiError) {
            "not_found" -> "backend endpoint not found"
            "provider_not_configured" -> "OAuth provider is not configured on the Worker"
            "connector_encryption_not_configured" -> "connector encryption secret is missing on the Worker"
            "unauthorized" -> "Firebase sign-in was not accepted"
            "plus_required" -> "Plus subscription is required for cloud sync"
            "missing_oauth_token" -> "OAuth token is missing. Disconnect and reconnect this email account."
            "missing_access_token" -> "OAuth access token is missing. Disconnect and reconnect this email account."
            "provider_sync_not_implemented" -> "Live sync for this email provider is not available in this build."
            "bad_attachment_key" -> "Attachment is not available for this account."
            "attachment_not_found" -> "Attachment file was not found in backup storage."
            else -> apiError ?: "HTTP $statusCode"
        }
    }

    private fun connectorReportErrorMessage(apiError: String, message: String): String {
        return when (apiError) {
            "provider_not_configured" -> "OAuth provider is not configured on the Worker"
            "connector_encryption_not_configured" -> "connector encryption secret is missing on the Worker"
            "missing_oauth_token" -> "OAuth token is missing. Disconnect and reconnect this email account."
            "missing_access_token" -> "OAuth access token is missing. Disconnect and reconnect this email account."
            "provider_sync_not_implemented" -> "Live sync for this email provider is not available in this build."
            else -> apiError.ifBlank { message.ifBlank { "connector sync failed" } }
        }
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

private fun JSONObject.optionalInt(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null
