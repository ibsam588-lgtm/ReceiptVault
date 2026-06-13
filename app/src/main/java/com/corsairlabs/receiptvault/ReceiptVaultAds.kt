package com.corsairlabs.receiptvault

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class ReceiptVaultAdController(private val activity: Activity) {
    private val prefs = activity.getSharedPreferences(AD_PREFS, Context.MODE_PRIVATE)
    private var initialized = false
    private var pendingVisitAd = false
    private var interstitialAd: InterstitialAd? = null

    fun startForFreeUser() {
        if (!initialized) {
            initialized = true
            MobileAds.initialize(activity.applicationContext) {
                loadInterstitial()
            }
        } else if (interstitialAd == null) {
            loadInterstitial()
        }
    }

    fun recordFreeVisit() {
        startForFreeUser()
        val now = System.currentTimeMillis()
        val lastVisitAt = prefs.getLong(LAST_VISIT_AT_KEY, 0L)
        if (now - lastVisitAt < VISIT_DEDUPE_MS) return
        val visitCount = prefs.getInt(VISIT_COUNT_KEY, 0) + 1
        prefs.edit()
            .putLong(LAST_VISIT_AT_KEY, now)
            .putInt(VISIT_COUNT_KEY, visitCount)
            .apply()
        if (visitCount % FREE_INTERSTITIAL_INTERVAL == 0) {
            pendingVisitAd = true
            showInterstitialIfReady()
        }
    }

    fun stopForPaidUser() {
        pendingVisitAd = false
        interstitialAd = null
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            activity,
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            loadInterstitial()
                        }
                    }
                    showInterstitialIfReady()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialIfReady() {
        if (!pendingVisitAd) return
        val interstitial = interstitialAd ?: return
        pendingVisitAd = false
        interstitialAd = null
        interstitial.show(activity)
    }

    companion object {
        private const val AD_PREFS = "receiptvault_ads"
        private const val VISIT_COUNT_KEY = "free_visit_count"
        private const val LAST_VISIT_AT_KEY = "free_last_visit_at"
        private const val VISIT_DEDUPE_MS = 2_000L
        private const val FREE_INTERSTITIAL_INTERVAL = 2
    }
}
