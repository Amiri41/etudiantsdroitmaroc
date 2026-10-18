package com.etudiantsdroitmaroc.app.utils

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/** حالة الانترنت: الشاشات كتتحدث وحدها ملي يرجع النت */
object NetworkStatus {
    private val _online = MutableLiveData(true)
    val online: LiveData<Boolean> get() = _online

    fun init(app: Application) {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        try {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            _online.postValue(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _online.postValue(true) }
                override fun onLost(network: Network) { _online.postValue(false) }
            })
        } catch (_: Exception) {
        }
    }
}
