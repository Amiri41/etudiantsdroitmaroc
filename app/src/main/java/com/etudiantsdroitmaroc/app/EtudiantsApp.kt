package com.etudiantsdroitmaroc.app

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EtudiantsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // تسجيل أي انهيار غير متوقع فملف باش نقدر نشوفوه من بعد
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "last_crash.txt").writeText(sw.toString())
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Firebase
        FirebaseApp.initializeApp(this)

        // تفعيل offline persistence باش Firestore يخدم بلا انترنت (كاش تلقائي)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        Firebase.firestore.firestoreSettings = settings

        // AdMob
        MobileAds.initialize(this)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadInterstitial(this)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadRewarded(this)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadAppOpenAd(this)
    }
}
