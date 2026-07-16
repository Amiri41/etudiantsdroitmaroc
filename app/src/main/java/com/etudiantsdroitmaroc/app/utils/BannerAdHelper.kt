package com.etudiantsdroitmaroc.app.utils

import android.content.Context
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** كيزيد بانر إعلان (Banner) فأي container، كيستعمل فكل الصفحات الرئيسية */
object BannerAdHelper {
    fun attach(context: Context, container: FrameLayout) {
        container.removeAllViews()
        val adView = AdView(context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = AdIds.BANNER
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }
}
