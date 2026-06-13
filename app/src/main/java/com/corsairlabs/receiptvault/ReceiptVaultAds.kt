package com.corsairlabs.receiptvault

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class ReceiptVaultAdController(private val activity: Activity) {
    private val prefs = activity.getSharedPreferences(AD_PREFS, Context.MODE_PRIVATE)
    private var initialized = false
    private var pendingVisitAd = false
    private var introDialogShowing = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null

    fun startForFreeUser() {
        if (!initialized) {
            initialized = true
            MobileAds.initialize(activity.applicationContext) {
                loadInterstitial()
                loadRewardedInterstitial()
            }
        } else if (interstitialAd == null) {
            loadInterstitial()
        }
        if (initialized && rewardedInterstitialAd == null) {
            loadRewardedInterstitial()
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
        if (visitCount % FREE_VIDEO_INTERVAL == 0) {
            pendingVisitAd = true
            showAdBreakIfReady()
        }
    }

    fun stopForPaidUser() {
        pendingVisitAd = false
        introDialogShowing = false
        interstitialAd = null
        rewardedInterstitialAd = null
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
                    showAdBreakIfReady()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun loadRewardedInterstitial() {
        RewardedInterstitialAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedInterstitialAd = null
                            loadRewardedInterstitial()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            rewardedInterstitialAd = null
                            loadRewardedInterstitial()
                        }
                    }
                    showAdBreakIfReady()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                }
            }
        )
    }

    private fun showAdBreakIfReady() {
        if (!pendingVisitAd) return
        val rewarded = rewardedInterstitialAd
        if (rewarded != null) {
            showRewardedIntro(rewarded)
            return
        }

        val interstitial = interstitialAd ?: return
        pendingVisitAd = false
        interstitialAd = null
        interstitial.show(activity)
    }

    private fun showRewardedIntro(ad: RewardedInterstitialAd) {
        if (introDialogShowing || activity.isFinishing || activity.isDestroyed) return
        introDialogShowing = true
        AlertDialog.Builder(activity)
            .setTitle("Ad break")
            .setMessage("Watch a short ad to keep using ReceiptVault Free with ads. You can skip this ad break.")
            .setNegativeButton("Skip") { dialog, _ ->
                pendingVisitAd = false
                introDialogShowing = false
                dialog.dismiss()
            }
            .setPositiveButton("Watch ad") { dialog, _ ->
                pendingVisitAd = false
                introDialogShowing = false
                rewardedInterstitialAd = null
                dialog.dismiss()
                ad.show(activity) {
                    // The rewarded interstitial callback is required by the ad format.
                }
            }
            .setOnCancelListener {
                pendingVisitAd = false
                introDialogShowing = false
            }
            .show()
    }

    companion object {
        private const val AD_PREFS = "receiptvault_ads"
        private const val VISIT_COUNT_KEY = "free_visit_count"
        private const val LAST_VISIT_AT_KEY = "free_last_visit_at"
        private const val VISIT_DEDUPE_MS = 2_000L
        private const val FREE_VIDEO_INTERVAL = 2
    }
}
