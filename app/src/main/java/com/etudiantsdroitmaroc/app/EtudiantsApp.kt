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

    // attachBaseContext كيخدم قبل حتى onCreate() وقبل ما تتبدا أي ContentProvider
    // (بحال ديال Firebase أو WorkManager) - هو أبكر نقطة ممكنة نحطو فيها الـ handler
    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val trace = sw.toString()
                val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (SDK ${android.os.Build.VERSION.SDK_INT})"
                val fullText = "DEVICE: $deviceInfo\nTIME: ${System.currentTimeMillis()}\n\n$trace"

                // 1) كتابة محلية (filesDir) - دايما كتخدم بلا شروط
                try { File(filesDir, "last_crash.txt").writeText(fullText) } catch (_: Exception) {}

                // 2) كتابة فمجلد خارجي مرتبط بالتطبيق - بلا ما نحتاجو أي إذن (permission)
                // وقابل للقراءة عبر File Manager: Android/data/com.etudiantsdroitmaroc.app/files/
                try {
                    getExternalFilesDir(null)?.let { dir ->
                        File(dir, "last_crash.txt").writeText(fullText)
                    }
                } catch (_: Exception) {}

                // 3) محاولة نبعتو لـ Firestore - إلا Firebase ماشي مهيأ، نهيؤوه أولا
                try {
                    if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                        FirebaseApp.initializeApp(this)
                    }
                    val uid = try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                    } catch (_: Exception) { "unknown" }
                    Firebase.firestore.collection("debug_logs").document().set(
                        mapOf(
                            "uid" to uid,
                            "trace" to trace.take(4000),
                            "timestamp" to System.currentTimeMillis(),
                            "device" to deviceInfo
                        )
                    )
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Firebase (محمي بـ try/catch باش إلا كريش هو نفسه، الـ handler لي حطينا
        // فـ attachBaseContext يقدر يمسكو ويسجلو محليا حتى لو Firebase ماخدمش)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (_: Exception) {
        }

        // إلا كان عندنا كراش محلي قديم ماتبعتش، نحاولو نبعتوه دابا
        try {
            val crashFile = File(filesDir, "last_crash.txt")
            if (crashFile.exists()) {
                val trace = crashFile.readText()
                val uid = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                } catch (_: Exception) { "unknown" }
                Firebase.firestore.collection("debug_logs").document().set(
                    mapOf(
                        "uid" to uid,
                        "trace" to trace.take(4000),
                        "timestamp" to System.currentTimeMillis(),
                        "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (SDK ${android.os.Build.VERSION.SDK_INT})",
                        "recovered" to true
                    )
                )
                crashFile.delete()
            }
        } catch (_: Exception) {
        }

        try {
            com.etudiantsdroitmaroc.app.utils.DarkModeHelper.applySavedMode(this)
        } catch (_: Exception) {
            // ما تكسرش بداية التطبيق إلا فشل تطبيق الوضع الليلي المحفوظ
        }

        // تفعيل offline persistence باش Firestore يخدم بلا انترنت (كاش تلقائي)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            Firebase.firestore.firestoreSettings = settings
        } catch (_: Exception) {
        }

        // AdMob
        MobileAds.initialize(this)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadInterstitial(this)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadRewarded(this)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadAppOpenAd(this)

        // تتبع حالة "متصل الآن" بدقة (كيتحدث ملي التطبيق يبان/يخبى، مع نبضة كل 30 ثانية)
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
            .addObserver(com.etudiantsdroitmaroc.app.utils.PresenceManager)

        // إعلان كل 8 دقايق مادام المستخدم نشيط فالتطبيق (بلا ما يبان مباشرة عند الفتح)
        com.etudiantsdroitmaroc.app.utils.PeriodicAdManager.init(this)
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
            .addObserver(com.etudiantsdroitmaroc.app.utils.PeriodicAdManager)

        // بانر الإعلان بشاشة كاملة كيتحكم فيه الأدمين (توقيت التكرار من لوحة التحكم)
        com.etudiantsdroitmaroc.app.utils.AnnouncementManager.init(this)
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
            .addObserver(com.etudiantsdroitmaroc.app.utils.AnnouncementManager)

        // كنشتركو فـ topic عام باش الأدمين يقدر يبعت إشعار لجميع المستخدمين مرة وحدة
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("all_users")
    }
}
