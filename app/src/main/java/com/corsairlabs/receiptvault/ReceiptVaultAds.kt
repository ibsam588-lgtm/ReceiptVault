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
    private var launchRecorded = false
    private var pendingLaunchAd = false
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

        if (!launchRecorded) {
            launchRecorded = true
            val launchCount = prefs.getInt(LAUNCH_COUNT_KEY, 0) + 1
            prefs.edit().putInt(LAUNCH_COUNT_KEY, launchCount).apply()
            if (launchCount % 2 == 0) {
                pendingLaunchAd = true
                showInterstitialIfReady()
            }
        }
    }

    fun stopForPaidUser() {
        pendingLaunchAd = false
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
        val ad = interstitialAd ?: return
        if (!pendingLaunchAd) return
        pendingLaunchAd = false
        interstitialAd = null
        ad.show(activity)
    }

    companion object {
        private const val AD_PREFS = "receiptvault_ads"
        private const val LAUNCH_COUNT_KEY = "free_launch_count"
    }
}
