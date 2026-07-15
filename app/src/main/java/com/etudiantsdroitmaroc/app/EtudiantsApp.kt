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

        // تسجيل أي انهيار غير متوقع + بعتو لـ Firestore باش نقدر نشوفوه بلا كمبيوتر
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val trace = sw.toString()
                File(filesDir, "last_crash.txt").writeText(trace)

                // Firestore عندو offline persistence، فالكتابة كتتخزن محليا وكتصيفط
                // للسيرفر أوتوماتيكيا ملي يرجع الانترنت، حتى لو التطبيق سدم مباشرة بعدها
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                Firebase.firestore.collection("debug_logs").document().set(
                    mapOf(
                        "uid" to uid,
                        "trace" to trace.take(4000),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
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

        // تتبع حالة "متصل الآن" بدقة (كيتحدث ملي التطبيق يبان/يخبى، مع نبضة كل 30 ثانية)
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
            .addObserver(com.etudiantsdroitmaroc.app.utils.PresenceManager)
    }
}
