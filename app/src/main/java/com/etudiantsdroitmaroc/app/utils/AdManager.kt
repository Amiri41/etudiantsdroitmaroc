package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

    private val handler = Handler(Looper.getMainLooper())
    private var interstitialLoading = false
    private var interstitialRetries = 0
    private var rewardedLoading = false
    private var rewardedRetries = 0
    private var lastInterstitialShownAt = 0L

    /** أقل مدة بين إعلانين Interstitial (دقيقتين) مهما كان السبب */
    private const val MIN_GAP_MS = 2 * 60 * 1000L
    private const val MAX_RETRIES = 3

    private fun cooldownPassed(): Boolean {
        val last = lastInterstitialShownAt
        return last == 0L || SystemClock.elapsedRealtime() - last >= MIN_GAP_MS
    }

    fun loadInterstitial(context: Context) {
        if (!ConsentManager.canRequestAds(context)) return
        if (interstitialAd != null || interstitialLoading) return
        val app = context.applicationContext
        interstitialLoading = true
        InterstitialAd.load(
            app, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialRetries = 0
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitialAd = null
                    // نعاودو نجربو (ماكسيمم 3 مرات) باش ما نبقاوش بلا إعلان جاهز
                    if (interstitialRetries < MAX_RETRIES) {
                        interstitialRetries++
                        handler.postDelayed({ loadInterstitial(app) }, 30_000L * interstitialRetries)
                    }
                }
            }
        )
    }

    private fun presentInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial(activity) // ماكانش جاهز: نحملو للمرة الجاية
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
                loadInterstitial(activity)
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    /** كتبان غير كل 3 فتحات محتوى، وبشرط دقيقتين على الأقل من آخر إعلان */
    fun showInterstitialIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        interstitialShownCount++
        if (interstitialShownCount % 3 != 0 || !cooldownPassed()) {
            if (interstitialAd == null) loadInterstitial(activity)
            onDismissed()
            return
        }
        presentInterstitial(activity, onDismissed)
    }

    /** كتستعمل من PeriodicAdManager: بلا شرط العدد، ولكن بنفس شرط الدقيقتين */
    fun showInterstitialForced(activity: Activity, onDismissed: () -> Unit = {}) {
        if (!cooldownPassed()) {
            onDismissed()
            return
        }
        presentInterstitial(activity, onDismissed)
    }

    // ---------- Rewarded ----------

    fun loadRewarded(context: Context) {
        if (!ConsentManager.canRequestAds(context)) return
        if (rewardedAd != null || rewardedLoading) return
        val app = context.applicationContext
        rewardedLoading = true
        RewardedAd.load(
            app, AdIds.REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedRetries = 0
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewardedAd = null
                    if (rewardedRetries < MAX_RETRIES) {
                        rewardedRetries++
                        handler.postDelayed({ loadRewarded(app) }, 30_000L * rewardedRetries)
                    }
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity)
            onUnavailable()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onUnavailable()
            }
        }
        ad.show(activity) { onReward() }
    }

    // ---------- Rewarded Interstitial ----------

    fun loadRewardedInterstitial(context: Context) {
        if (!ConsentManager.canRequestAds(context)) return
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
        if (!ConsentManager.canRequestAds(context)) return
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
