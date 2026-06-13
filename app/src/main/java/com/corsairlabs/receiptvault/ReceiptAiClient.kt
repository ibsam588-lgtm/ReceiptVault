package com.corsairlabs.receiptvault

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ReceiptAiClient(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val endpoint = "${BuildConfig.R2_BACKUP_API_URL}/v1/ai/categorize"

    suspend fun categorize(rawText: String, source: ImportSource, preferredCurrency: String): ReceiptAiSuggestion? {
        if (rawText.isBlank()) return null
        return runCatching {
            val token = firebaseToken()
            postCategorize(token, rawText, source, preferredCurrency)
        }.getOrNull()
    }

    private suspend fun firebaseToken(): String {
        val user = auth.currentUser ?: throw IOException("Please sign in to use AI categorization.")
        val token = user
            .getIdToken(false)
            .await()
            .token
        return token ?: throw IOException("Firebase token unavailable")
    }

    private suspend fun postCategorize(
        token: String,
        rawText: String,
        source: ImportSource,
        preferredCurrency: String
    ): ReceiptAiSuggestion? =
        withContext(Dispatchers.IO) {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject()
                .put("ocrText", rawText.take(12000))
                .put("emailSubject", if (source == ImportSource.EmailShare) "Shared email receipt" else "")
                .put("preferredCurrency", preferredCurrency)
                .toString()

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IOException("AI categorization failed with ${connection.responseCode}: $response")
            }

            val result = JSONObject(response).optJSONObject("result") ?: return@withContext null
            ReceiptAiSuggestion(
                isReceipt = result.optBoolean("isReceipt", false),
                merchant = result.nullableString("merchant"),
                total = result.nullableDouble("total"),
                currencyCode = result.nullableString("currencyCode"),
                purchaseDate = result.nullableString("purchaseDate"),
                category = result.nullableString("category"),
                warrantyCandidate = result.optBoolean("warrantyCandidate", false),
                returnWindowDays = result.nullableInt("returnWindowDays"),
                confidence = result.optDouble("confidence", 0.0),
                notes = result.nullableString("notes")
            )
        }
}

data class ReceiptAiSuggestion(
    val isReceipt: Boolean,
    val merchant: String?,
    val total: Double?,
    val currencyCode: String?,
    val purchaseDate: String?,
    val category: String?,
    val warrantyCandidate: Boolean,
    val returnWindowDays: Int?,
    val confidence: Double,
    val notes: String?
)

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

private fun JSONObject.nullableString(key: String): String? {
    val value = optString(key, "").trim()
    return value.takeIf { it.isNotBlank() && it.lowercase() != "null" }
}

private fun JSONObject.nullableDouble(key: String): Double? {
    return if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
}

private fun JSONObject.nullableInt(key: String): Int? {
    return if (has(key) && !isNull(key)) optInt(key) else null
}
