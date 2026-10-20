package com.etudiantsdroitmaroc.app.ui.chat

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.DialogChatBackgroundBinding
import com.etudiantsdroitmaroc.app.ui.common.AppDialog
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityChatBinding
import com.etudiantsdroitmaroc.app.ui.moderation.ReportDialog
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val repository = ChatRepository()
    private lateinit var adapter: MessageAdapter
    private lateinit var threadId: String
    private var otherUid: String = ""
    private var messagesListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null
    private var lastMessages: List<ChatMessage> = emptyList()
    private var inForeground = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) sendImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        threadId = intent.getStringExtra("threadId") ?: return
        otherUid = intent.getStringExtra("otherUid") ?: ""
        binding.tvOtherName.text = intent.getStringExtra("otherName") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnChatMenu.setOnClickListener { showChatMenu() }

        lifecycleScope.launch { repository.markThreadAsRead(threadId) }

        if (otherUid.isNotEmpty()) {
            Firebase.firestore.collection("users").document(otherUid).get()
                .addOnSuccessListener { doc ->
                    val photoUrl = doc.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this).load(photoUrl).into(binding.ivOtherAvatar)
                    }
                }
        }

        adapter = MessageAdapter(
            emptyList(),
            onReportClick = { message ->
                ReportDialog.show(
                    this, lifecycleScope,
                    targetType = "message", targetId = message.id,
                    targetOwnerUid = otherUid, targetOwnerName = binding.tvOtherName.text.toString(),
                    targetPath = "chat_threads/$threadId/messages/${message.id}"
                )
            },
            onInfoClick = { message -> showMessageInfo(message) }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.setHasFixedSize(true)
        binding.rvMessages.adapter = adapter

        binding.fabSend.setOnClickListener { sendMessage() }
        binding.btnAttachImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        applySavedBackground()
        listenOtherStatus()
        listenForMessages()
    }

    /** Real-time listener - كل رسالة جديدة كتبان مباشرة بلا ما نعاود نطلب من جديد */
    private fun listenForMessages() {
        messagesListener = Firebase.firestore.collection("chat_threads").document(threadId)
            .collection("messages")
            .orderBy("sentAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // قبل، الخطأ كان كيتجاهل وكتبان محادثة فارغة (مثلا رفض صلاحية)
                    android.util.Log.e("EDM", "messages listener failed: ${error.code} - ${error.message}", error)
                    Toast.makeText(this, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(error), Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val messages: List<ChatMessage> = snapshot?.toObjects() ?: emptyList()
                lastMessages = messages
                adapter.updateData(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
                markSeenIfVisible()
            }
    }

    /** ملي المستخدم كيشوف المحادثة: كنعلمو الرسائل الواردة بـ "تمت قراءتها" (✓✓ زرقاء عند المرسل) */
    private fun markSeenIfVisible() {
        if (!inForeground) return
        if (lastMessages.none { it.senderUid != currentUid() && !it.seen }) return
        val snapshot = lastMessages
        lifecycleScope.launch { repository.markIncomingMessagesSeen(threadId, snapshot) }
    }

    private fun currentUid() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    /** حالة الطرف الآخر فالهيدر: "متصل الآن" ولا "آخر ظهور ..." */
    private fun listenOtherStatus() {
        if (otherUid.isEmpty()) return
        statusListener = Firebase.firestore.collection("users").document(otherUid)
            .addSnapshotListener { doc, _ ->
                if (doc == null || !doc.exists()) return@addSnapshotListener
                val online = doc.getBoolean("isOnline") == true
                val lastSeen = doc.getLong("lastSeen") ?: 0L
                val text = statusText(online, lastSeen)
                binding.tvOtherStatus.visibility = if (text == null) View.GONE else View.VISIBLE
                binding.tvOtherStatus.text = text
            }
    }

    private fun statusText(online: Boolean, lastSeen: Long): String? {
        val now = System.currentTimeMillis()
        if (online && now - lastSeen < 2 * 60 * 1000L) return "متصل الآن"
        if (lastSeen <= 0L) return null
        return "آخر ظهور " + whenText(lastSeen)
    }

    /** "اليوم الساعة 09:11" / "أمس الساعة 21:03" / "17 أكتوبر 2026 الساعة 14:50" */
    private fun whenText(ts: Long): String {
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(ts))
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        fun same(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when {
            same(c, today) -> "اليوم الساعة $time"
            same(c, yesterday) -> "أمس الساعة $time"
            else -> SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date(ts)) + " الساعة $time"
        }
    }

    /** معلومات الرسالة (ضغطة مطولة على رسالة ديالي): وقت التسليم والقراءة */
    private fun showMessageInfo(message: ChatMessage) {
        val sent = whenText(message.sentAt)
        val delivered = if (message.deliveredAt > 0L) whenText(message.deliveredAt) else "—"
        val seen = if (message.seen && message.seenAt > 0L) whenText(message.seenAt) else if (message.seen) "تمت" else "—"
        AppDialog.Builder(this)
            .setTitle("معلومات الرسالة")
            .setMessage("✓ تبعتات: $sent\n\n✓✓ وصلات: $delivered\n\n✓✓ تقراات: $seen")
            .setPositiveButton("تمام")
            .show()
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        lifecycleScope.launch {
            repository.sendMessage(threadId, text)
        }
    }

    private fun sendImage(uri: android.net.Uri) {
        Toast.makeText(this, "جاري رفع الصورة…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = ImageUploader.uploadImage(this@ChatActivity, uri)
            if (result.isSuccess) {
                repository.sendImageMessage(threadId, result.getOrNull() ?: "")
            } else {
                Toast.makeText(this@ChatActivity, "فشل رفع الصورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChatMenu() {
        val popup = android.widget.PopupMenu(this, binding.btnChatMenu)
        popup.menu.add("حظر المستخدم")
        popup.menu.add("مسح المحادثة")
        popup.menu.add("تغيير الخلفية")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "حظر المستخدم" -> confirmBlockUser()
                "مسح المحادثة" -> confirmClearChat()
                "تغيير الخلفية" -> showBackgroundPicker()
            }
            true
        }
        popup.show()
    }

    private fun confirmBlockUser() {
        AppDialog.Builder(this)
            .setTitle("حظر المستخدم")
            .setIcon(R.drawable.ic_block)
            .setMessage("ملي تحظرو، ما غاديش يبان ليك فلوائح المتصلين ولا المحادثات. متأكد؟")
            .setPositiveButton("حظر") { _, _ ->
                lifecycleScope.launch {
                    repository.blockUser(otherUid)
                    Toast.makeText(this@ChatActivity, "تم حظر المستخدم", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmClearChat() {
        AppDialog.Builder(this)
            .setTitle("مسح المحادثة")
            .setIcon(R.drawable.ic_delete_forever)
            .setMessage("غادي يتمسح كل التاريخ ديال هاد المحادثة. متأكد؟")
            .setPositiveButton("مسح") { _, _ ->
                lifecycleScope.launch {
                    repository.clearChatMessages(threadId)
                    Toast.makeText(this@ChatActivity, "تم مسح المحادثة", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showBackgroundPicker() {
        val pb = DialogChatBackgroundBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).setView(pb.root).create()
        val saved = getSharedPreferences("chat_prefs", MODE_PRIVATE).getString(prefsKey(), null) ?: "default"
        val density = resources.displayMetrics.density
        val night = isNightMode()

        ChatWallpapers.all.forEach { wp ->
            val card = FrameLayout(this).apply {
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = (96 * density).toInt()
                    height = (132 * density).toInt()
                    setMargins((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
                }
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 14 * density)
                    }
                }
                clipToOutline = true
            }
            val preview = View(this)
            if (wp.drawableRes != 0) {
                preview.background = ContextCompat.getDrawable(this, wp.drawableRes)
            } else {
                preview.setBackgroundColor(ContextCompat.getColor(this, R.color.off_white))
            }
            card.addView(preview, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            val label = TextView(this).apply {
                text = wp.label
                textSize = 12f
                setTextColor(0xFF0A1F3D.toInt())
                setBackgroundResource(R.drawable.bg_date_chip)
                setPadding((10 * density).toInt(), (3 * density).toInt(), (10 * density).toInt(), (3 * density).toInt())
            }
            card.addView(label, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = (8 * density).toInt() })

            if (wp.key == saved || (wp.key == "default" && saved.startsWith("#"))) {
                val ring = View(this).apply { setBackgroundResource(R.drawable.bg_wallpaper_selected) }
                card.addView(ring, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            card.setOnClickListener {
                saveBackground(wp.key)
                dialog.dismiss()
            }
            pb.wallpaperGrid.addView(card)
        }
        AppDialog.styleWindow(this, dialog)
    }

    private fun prefsKey() = "chat_bg_$threadId"

    private fun saveBackground(key: String) {
        getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
            .putString(prefsKey(), key).apply()
        applySavedBackground()
    }

    private fun isNightMode() =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    /** كنطبقو الخلفية المحفوظة (نقش أو لون قديم) وكنعلمو الـ adapter واش الخلفية داكنة باش تتبدل ألوان الفقاعات */
    private fun applySavedBackground() {
        val saved = getSharedPreferences("chat_prefs", MODE_PRIVATE).getString(prefsKey(), null)
        val wp = ChatWallpapers.find(saved)
        var dark = false
        when {
            wp != null && wp.drawableRes != 0 -> {
                binding.rvMessages.background = ContextCompat.getDrawable(this, wp.drawableRes)
                dark = wp.dark
            }
            saved != null && saved.startsWith("#") -> {
                // خلفية بلون قديم (من النسخ السابقة)
                try {
                    val color = android.graphics.Color.parseColor(saved)
                    binding.rvMessages.setBackgroundColor(color)
                    dark = androidx.core.graphics.ColorUtils.calculateLuminance(color) < 0.4
                } catch (_: Exception) {
                    binding.rvMessages.setBackgroundColor(ContextCompat.getColor(this, R.color.off_white))
                    dark = isNightMode()
                }
            }
            else -> {
                binding.rvMessages.setBackgroundColor(ContextCompat.getColor(this, R.color.off_white))
                dark = isNightMode()
            }
        }
        if (::adapter.isInitialized) adapter.setDarkBackground(dark)
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        if (::threadId.isInitialized) com.etudiantsdroitmaroc.app.utils.ActiveChat.threadId = threadId
        markSeenIfVisible()
    }

    override fun onPause() {
        super.onPause()
        inForeground = false
        com.etudiantsdroitmaroc.app.utils.ActiveChat.threadId = null
    }

    override fun onDestroy() {
        messagesListener?.remove()
        statusListener?.remove()
        super.onDestroy()
    }
}
