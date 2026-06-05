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

    private suspend fun firebaseToken(): String {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        val token = (user ?: throw IOException("Firebase anonymous auth unavailable"))
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
        if (connection.responseCode !in 200..299) throw IOException(response)
        JSONObject(response).optString("authorizationUrl", "").takeIf { it.isNotBlank() }
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
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
