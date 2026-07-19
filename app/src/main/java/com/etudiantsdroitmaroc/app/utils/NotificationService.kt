package com.etudiantsdroitmaroc.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.ui.AnnouncementFullscreenActivity
import com.etudiantsdroitmaroc.app.ui.MainActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth

private const val CHANNEL_ID = "etudiants_general"

/**
 * كيستقبل الإشعارات (رسالة جديدة، منشور جديد...).
 * الإرسال كيتم عبر Cloudflare Worker (بديل مجاني لـ Cloud Functions) - شوف PushNotifier.kt.
 * هاد الملف كيدير الاستقبال والعرض فالهاتف.
 */
class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // نحفظو الـ FCM token فبروفايل المستخدم باش نقدر نبعتو ليه إشعارات مستهدفة
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: message.data["message"] ?: ""

        if (message.data["kind"] == "broadcast_announcement") {
            showAnnouncementNotification(
                title = title,
                body = body,
                type = message.data["type"] ?: "promo",
                imageUrl = message.data["imageUrl"] ?: "",
                linkUrl = message.data["linkUrl"] ?: ""
            )
        } else {
            showNotification(title, body)
        }
    }

    /** إشعار إعلان جماعي - ملي يضغط عليه المستخدم كيدخل مباشرة للصفحة الاحترافية بشاشة كاملة */
    private fun showAnnouncementNotification(title: String, body: String, type: String, imageUrl: String, linkUrl: String) {
        createChannelIfNeeded()

        val intent = Intent(this, AnnouncementFullscreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("type", type)
            putExtra("title", title)
            putExtra("message", body)
            putExtra("imageUrl", imageUrl)
            putExtra("linkUrl", linkUrl)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showNotification(title: String, body: String) {
        createChannelIfNeeded()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "إشعارات عامة", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
    }
}
