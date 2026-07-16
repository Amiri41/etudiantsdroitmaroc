package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

/**
 * كيدير تحميل وعرض كل أنواع الإعلانات (Interstitial, Rewarded, Rewarded Interstitial, App Open)
 * فمكان واحد باش ما نعاودوش نكتبو نفس الكود فكل صفحة.
 *
 * الاستعمال:
 *   AdManager.loadInterstitial(context)
 *   AdManager.showInterstitialIfReady(activity) { /* كمل العملية */ }
 */
object AdManager {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null

    private var interstitialShownCount = 0

    // ---------- Interstitial ----------

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return
        InterstitialAd.load(
            context, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /** كتبان الإعلان غير كل 3 انتقالات باش ما نزعجوش الطالب بزاف */
    fun showInterstitialIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        interstitialShownCount++
        val shouldShow = interstitialAd != null && interstitialShownCount % 3 == 0

        if (!shouldShow) {
            onDismissed()
            return
        }

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismissed()
            }
        }
        interstitialAd?.show(activity) ?: onDismissed()
    }

    /** كتبان بلا شرط عدد الانتقالات (كتستعمل من PeriodicAdManager كل 8 دقايق) */
    fun showInterstitialForced(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }

    // ---------- Rewarded ----------

    fun loadRewarded(context: Context) {
        if (rewardedAd != null) return
        RewardedAd.load(
            context, AdIds.REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
            }
        }
        ad.show(activity) { onReward() }
    }

    // ---------- Rewarded Interstitial ----------

    fun loadRewardedInterstitial(context: Context) {
        if (rewardedInterstitialAd != null) return
        RewardedInterstitialAd.load(
            context, AdIds.REWARDED_INTERSTITIAL, AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                }
            }
        )
    }

    // ---------- App Open ----------

    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null) return
        AppOpenAd.load(
            context, AdIds.APP_OPEN, AdRequest.Builder().build(),
            object : com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpenAdIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = appOpenAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
