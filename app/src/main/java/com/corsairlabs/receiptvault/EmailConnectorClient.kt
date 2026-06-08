package com.corsairlabs.receiptvault

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject

class EmailConnectorClient {
    private val auth = FirebaseAuth.getInstance()
    private val apiBase = BuildConfig.R2_BACKUP_API_URL

    suspend fun startOAuth(provider: EmailProvider): String? {
        return runCatching {
            val token = firebaseToken()
            postStart(token, provider)
        }.getOrNull()
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

    suspend fun syncProvider(provider: EmailProvider): ConnectorSyncSummary {
        val token = firebaseToken()
        return postSync(token, provider)
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
            readTimeout = 25000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put("provider", provider.providerId)
            .put("maxCandidates", 10)
            .toString()
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) throw IOException(httpErrorMessage(connection.responseCode, response))

        val reports = JSONObject(response).optJSONArray("reports")
        val report = reports?.optJSONObject(0)
        ConnectorSyncSummary(
            scanned = report?.optInt("scanned", 0) ?: 0,
            candidates = report?.optInt("candidates", 0) ?: 0,
            imported = report?.optInt("imported", 0) ?: 0,
            message = report?.optString("message", "Receipt-only sync check completed.") ?: "Receipt-only sync check completed."
        )
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
            else -> apiError ?: "HTTP $statusCode"
        }
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
