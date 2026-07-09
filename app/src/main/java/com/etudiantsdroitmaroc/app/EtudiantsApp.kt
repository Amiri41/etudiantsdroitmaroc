package com.etudiantsdroitmaroc.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EtudiantsApp : Application() {

    override fun onCreate() {
        super.onCreate()

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
