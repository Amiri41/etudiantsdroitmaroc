package com.etudiantsdroitmaroc.app.ui.chat

import android.os.Bundle
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

        applySavedBackground()

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

        adapter = MessageAdapter(emptyList()) { message ->
            ReportDialog.show(
                this, lifecycleScope,
                targetType = "message", targetId = message.id,
                targetOwnerUid = otherUid, targetOwnerName = binding.tvOtherName.text.toString()
            )
        }
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.fabSend.setOnClickListener { sendMessage() }
        binding.btnAttachImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        listenForMessages()
    }

    /** Real-time listener - كل رسالة جديدة كتبان مباشرة بلا ما نعاود نطلب من جديد */
    private fun listenForMessages() {
        Firebase.firestore.collection("chat_threads").document(threadId)
            .collection("messages")
            .orderBy("sentAt")
            .addSnapshotListener { snapshot, _ ->
                val messages: List<ChatMessage> = snapshot?.toObjects() ?: emptyList()
                adapter.updateData(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("حظر المستخدم")
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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("مسح المحادثة")
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
        val dialogView = layoutInflater.inflate(com.etudiantsdroitmaroc.app.R.layout.dialog_chat_background, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView).create()

        val colorViews = listOf(
            com.etudiantsdroitmaroc.app.R.id.colorDefault,
            com.etudiantsdroitmaroc.app.R.id.color1,
            com.etudiantsdroitmaroc.app.R.id.color2,
            com.etudiantsdroitmaroc.app.R.id.color3,
            com.etudiantsdroitmaroc.app.R.id.color4,
            com.etudiantsdroitmaroc.app.R.id.color5,
            com.etudiantsdroitmaroc.app.R.id.color6,
            com.etudiantsdroitmaroc.app.R.id.color7
        )
        for (id in colorViews) {
            val view = dialogView.findViewById<android.view.View>(id)
            view.setOnClickListener {
                val colorHex = it.tag as String
                saveBackground(colorHex)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun prefsKey() = "chat_bg_$threadId"

    private fun saveBackground(colorHex: String) {
        getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
            .putString(prefsKey(), colorHex).apply()
        applySavedBackground()
    }

    private fun applySavedBackground() {
        val colorHex = getSharedPreferences("chat_prefs", MODE_PRIVATE)
            .getString(prefsKey(), null) ?: return
        try {
            binding.rvMessages.setBackgroundColor(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) { }
    }
}
