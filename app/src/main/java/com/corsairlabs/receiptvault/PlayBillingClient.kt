package com.corsairlabs.receiptvault

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class ReceiptVaultBillingProduct(
    val productId: String,
    val title: String,
    val fallbackPrice: String,
    val cadence: String,
    val plan: ReceiptVaultPlan,
    val description: String
) {
    PlusMonthly(
        productId = "receiptvault_plus_monthly",
        title = "ReceiptVault Plus",
        fallbackPrice = "\$4.99",
        cadence = "Monthly",
        plan = ReceiptVaultPlan.Plus,
        description = "No ads, cloud backup, 3 email connectors, unlimited manual uploads, and unlimited warranties."
    ),
    PlusYearly(
        productId = "receiptvault_plus_yearly",
        title = "ReceiptVault Plus",
        fallbackPrice = "\$47.99",
        cadence = "Yearly",
        plan = ReceiptVaultPlan.Plus,
        description = "No ads and all Plus features with the yearly 20% discount."
    ),
    BusinessMonthly(
        productId = "receiptvault_business_monthly",
        title = "ReceiptVault Business",
        fallbackPrice = "\$12.99",
        cadence = "Monthly",
        plan = ReceiptVaultPlan.Business,
        description = "No ads, 10 email connectors, business folders, team-ready backup, and tax-ready exports."
    ),
    BusinessYearly(
        productId = "receiptvault_business_yearly",
        title = "ReceiptVault Business",
        fallbackPrice = "\$124.99",
        cadence = "Yearly",
        plan = ReceiptVaultPlan.Business,
        description = "No ads and all Business features with the yearly 20% discount."
    )
}

data class BillingPlanOffer(
    val product: ReceiptVaultBillingProduct,
    val displayPrice: String,
    val available: Boolean,
    val active: Boolean,
    val offerToken: String?,
    val productDetails: ProductDetails?
)

data class ReceiptVaultBillingState(
    val connected: Boolean = false,
    val loading: Boolean = true,
    val message: String = "Connecting to Google Play Billing",
    val activeProductIds: Set<String> = emptySet(),
    val offers: List<BillingPlanOffer> = fallbackBillingOffers(emptySet())
)

class PlayBillingClient(private val context: Context) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences(BILLING_PREFS, Context.MODE_PRIVATE)
    private val productDetailsById = mutableMapOf<String, ProductDetails>()
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(
        ReceiptVaultBillingState(activeProductIds = loadActiveProductIds(), offers = fallbackBillingOffers(loadActiveProductIds()))
    )
    val state: kotlinx.coroutines.flow.StateFlow<ReceiptVaultBillingState> = _state

    fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryActivePurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    updateState(connected = true, loading = false, message = "Google Play Billing connected.")
                    queryProductDetails()
                    queryActivePurchases()
                } else {
                    updateState(
                        connected = false,
                        loading = false,
                        message = "Google Play Billing unavailable: ${billingResult.debugMessage}"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                updateState(connected = false, loading = false, message = "Google Play Billing disconnected. It will reconnect when needed.")
            }
        })
    }

    fun launchPurchase(activity: Activity, product: ReceiptVaultBillingProduct) {
        val offer = _state.value.offers.firstOrNull { it.product == product }
        val details = offer?.productDetails
        val offerToken = offer?.offerToken
        if (!billingClient.isReady || details == null || offerToken.isNullOrBlank()) {
            updateState(message = "Create and activate ${product.productId} in Play Console before purchases can launch.")
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            updateState(message = "Could not start purchase: ${result.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty(), clearWhenEmpty = false)
            BillingClient.BillingResponseCode.USER_CANCELED -> updateState(message = "Purchase canceled.")
            else -> updateState(message = "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    fun stop() {
        if (billingClient.isReady) billingClient.endConnection()
        scope.cancel()
    }

    private fun queryProductDetails() {
        val products = ReceiptVaultBillingProduct.entries.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                updateState(loading = false, message = "Could not load subscription products: ${billingResult.debugMessage}")
                return@queryProductDetailsAsync
            }

            productDetailsById.clear()
            result.productDetailsList.forEach { details ->
                productDetailsById[details.productId] = details
            }
            updateState(
                loading = false,
                message = if (productDetailsById.isEmpty()) {
                    "Subscription products are not active in Play Console yet."
                } else {
                    "Subscription products loaded from Google Play."
                }
            )
        }
    }

    private fun queryActivePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases, clearWhenEmpty = true)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>, clearWhenEmpty: Boolean) {
        if (purchases.isEmpty() && clearWhenEmpty) {
            saveActiveProductIds(emptySet())
            updateState(message = "No active Google Play subscription found.")
            return
        }

        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                val productId = purchase.products.firstOrNull { it in billingProductIds() } ?: return@forEach
                scope.launch {
                    // B15: acknowledge BEFORE backend verification — Google auto-refunds after
                    // 3 days if the purchase is not acknowledged, and the network call might not
                    // complete if the app is closed immediately after purchase
                    acknowledgePurchaseIfNeeded(purchase)

                    val verified = runCatching {
                        verifyPurchaseWithBackend(productId, purchase.purchaseToken)
                    }.getOrDefault(false)
                    if (verified || BuildConfig.DEBUG) {
                        val updated = loadActiveProductIds() + productId
                        saveActiveProductIds(updated)
                        updateState(
                            message = if (verified) {
                                "Subscription verified. ${productForId(productId)?.plan?.label ?: "Paid plan"} is active."
                            } else {
                                "Purchase acknowledged locally for testing. Add the Worker Google Play service account secret for server verification."
                            }
                        )
                    } else {
                        updateState(message = "Purchase completed, but server verification is not configured yet.")
                    }
                }
            }

        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PENDING }
            .takeIf { it.isNotEmpty() }
            ?.let { updateState(message = "Purchase is pending. Google Play will update ReceiptVault after payment completes.") }
    }

    private suspend fun verifyPurchaseWithBackend(productId: String, purchaseToken: String): Boolean = withContext(Dispatchers.IO) {
        val token = firebaseToken()
        val connection = (URL("${BuildConfig.R2_BACKUP_API_URL}/v1/billing/google-play/purchase").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        val body = JSONObject()
            .put("productId", productId)
            .put("purchaseToken", purchaseToken)
            .toString()
        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) throw IOException(response)
        JSONObject(response).optBoolean("active", false)
    }

    private suspend fun firebaseToken(): String {
        val user = auth.currentUser ?: throw IOException("Please sign in before verifying subscriptions.")
        val token = user
            .getIdToken(false)
            .billingAwait()
            .token
        return token ?: throw IOException("Firebase token unavailable")
    }

    private fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                updateState(message = "Purchase active, but acknowledgement failed: ${result.debugMessage}")
            }
        }
    }

    private fun updateState(
        connected: Boolean = _state.value.connected,
        loading: Boolean = _state.value.loading,
        message: String = _state.value.message
    ) {
        val activeProductIds = loadActiveProductIds()
        _state.value = ReceiptVaultBillingState(
            connected = connected,
            loading = loading,
            message = message,
            activeProductIds = activeProductIds,
            offers = buildBillingOffers(activeProductIds)
        )
    }

    private fun buildBillingOffers(activeProductIds: Set<String>): List<BillingPlanOffer> {
        return ReceiptVaultBillingProduct.entries.map { product ->
            val details = productDetailsById[product.productId]
            val subscriptionOffer = details?.subscriptionOfferDetails?.firstOrNull()
            val price = subscriptionOffer
                ?.pricingPhases
                ?.pricingPhaseList
                ?.lastOrNull()
                ?.formattedPrice
                ?: product.fallbackPrice
            BillingPlanOffer(
                product = product,
                displayPrice = price,
                available = details != null && !subscriptionOffer?.offerToken.isNullOrBlank(),
                active = product.productId in activeProductIds,
                offerToken = subscriptionOffer?.offerToken,
                productDetails = details
            )
        }
    }

    private fun loadActiveProductIds(): Set<String> {
        return prefs.getStringSet(ACTIVE_PRODUCTS_KEY, emptySet()).orEmpty()
    }

    private fun saveActiveProductIds(productIds: Set<String>) {
        prefs.edit().putStringSet(ACTIVE_PRODUCTS_KEY, productIds).apply()
    }

    companion object {
        private const val BILLING_PREFS = "receiptvault_billing"
        private const val ACTIVE_PRODUCTS_KEY = "activeProductIds"
    }
}

fun receiptVaultPlanFromActiveProducts(context: Context): ReceiptVaultPlan {
    val productIds = context
        .getSharedPreferences("receiptvault_billing", Context.MODE_PRIVATE)
        .getStringSet("activeProductIds", emptySet())
        .orEmpty()
    return when {
        productIds.any { productForId(it)?.plan == ReceiptVaultPlan.Business } -> ReceiptVaultPlan.Business
        productIds.any { productForId(it)?.plan == ReceiptVaultPlan.Plus } -> ReceiptVaultPlan.Plus
        else -> ReceiptVaultPlan.Free
    }
}

fun fallbackBillingOffers(activeProductIds: Set<String>): List<BillingPlanOffer> {
    return ReceiptVaultBillingProduct.entries.map { product ->
        BillingPlanOffer(
            product = product,
            displayPrice = product.fallbackPrice,
            available = false,
            active = product.productId in activeProductIds,
            offerToken = null,
            productDetails = null
        )
    }
}

fun billingProductIds(): Set<String> = ReceiptVaultBillingProduct.entries.map { it.productId }.toSet()

fun productForId(productId: String): ReceiptVaultBillingProduct? {
    return ReceiptVaultBillingProduct.entries.firstOrNull { it.productId == productId }
}

private suspend fun <T> Task<T>.billingAwait(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
