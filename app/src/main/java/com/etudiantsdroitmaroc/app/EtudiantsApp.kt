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

        // Firebase خاصو يتبدا أول حاجة ديال كولشي - باش الـ exception handler تحت
        // يقدر يكتب لـ Firestore حتى لو الكراش وقع فبداية بداية التطبيق (بحال DarkModeHelper)
        FirebaseApp.initializeApp(this)

        // تسجيل أي انهيار غير متوقع - خاصو يكون قبل أي كود آخر يقدر يكريش
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val trace = sw.toString()
                File(filesDir, "last_crash.txt").writeText(trace)

                // Firestore عندو offline persistence، فالكتابة كتتخزن محليا وكتصيفط
                // للسيرفر أوتوماتيكيا ملي يرجع الانترنت، حتى لو التطبيق سدم مباشرة بعدها
                val uid = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                } catch (_: Exception) { "unknown" }
                Firebase.firestore.collection("debug_logs").document().set(
                    mapOf(
                        "uid" to uid,
                        "trace" to trace.take(4000),
                        "timestamp" to System.currentTimeMillis(),
                        "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (SDK ${android.os.Build.VERSION.SDK_INT})"
                    )
                )
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // إلا كان عندنا كراش محلي قديم ماتبعتش (مثلا وقع الكراش وما كانش انترنت
        // أو Firebase ماكانش مهيأ بعد فنسخة قديمة)، نحاولو نبعتوه دابا
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
