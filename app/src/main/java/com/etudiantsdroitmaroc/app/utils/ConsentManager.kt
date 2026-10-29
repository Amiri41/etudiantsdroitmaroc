package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * كيدبر موافقة المستعمل على الإعلانات (Google UMP).
 * الإعلانات ما كتبداش (initialize + load) حتى كتولي canRequestAds() = true.
 * خارج المناطق اللي كتطلب الموافقة (بحال المغرب) كتبدا مباشرة بلا نافذة.
 */
object ConsentManager {

    private val adsStarted = AtomicBoolean(false)

    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context.applicationContext).canRequestAds()

    /** كيتنادى من SplashActivity فكل فتح. onDone كيتنادى مرة وحدة مهما كانت النتيجة. */
    fun gather(activity: Activity, onDone: () -> Unit) {
        val app = activity.applicationContext
        val info = UserMessagingPlatform.getConsentInformation(app)
        val done = AtomicBoolean(false)
        val finish = {
            if (info.canRequestAds()) startAds(app)
            if (done.compareAndSet(false, true)) onDone()
        }

        // موافقة محفوظة من قبل: نبداو الإعلانات دغيا بلا ما نتسناو الشبكة
        if (info.canRequestAds()) startAds(app)

        try {
            val params = ConsentRequestParameters.Builder().build()
            info.requestConsentInfoUpdate(
                activity, params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> finish() }
                },
                { _ -> finish() }
            )
        } catch (_: Exception) {
            finish()
        }
    }

    /** زر "خيارات الخصوصية" (مطلوب فأوروبا): واش خاصو يبان */
    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)
            .privacyOptionsRequirementStatus ==
            com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onDone() }
    }

    private fun startAds(app: Context) {
        if (!adsStarted.compareAndSet(false, true)) return
        MobileAds.initialize(app)
        AdManager.loadInterstitial(app)
        AdManager.loadRewarded(app)
        AdManager.loadAppOpenAd(app)
    }
}
