package com.etudiantsdroitmaroc.app.ui.forum

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.GroupMessage
import com.etudiantsdroitmaroc.app.data.remote.GroupRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityGroupChatBinding
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import kotlinx.coroutines.launch

class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private val repository = GroupRepository()
    private lateinit var adapter: GroupMessageAdapter
    private lateinit var groupId: String
    private var isAdmin = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) sendImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("groupId") ?: return
        val groupName = intent.getStringExtra("groupName") ?: ""
        val groupIcon = intent.getStringExtra("groupIcon") ?: ""
        isAdmin = intent.getBooleanExtra("isAdmin", false)

        binding.tvGroupName.text = groupName
        if (groupIcon.isNotEmpty()) {
            Glide.with(this).load(groupIcon).into(binding.ivGroupIcon)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = GroupMessageAdapter(
            emptyList(),
            isAdmin,
            onLongPressDelete = { message -> confirmDelete(message) },
            onClickAvatar = { message -> openSenderProfile(message) }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.fabSend.setOnClickListener { sendText() }
        binding.btnAttachImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        loadMessages()
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val messages = repository.getGroupMessages(groupId)
                adapter.updateData(messages)
                binding.rvMessages.scrollToPosition(maxOf(0, messages.size - 1))
            } catch (e: Exception) {
                Toast.makeText(this@GroupChatActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendText() {
        val text = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        lifecycleScope.launch {
            repository.sendGroupTextMessage(groupId, text)
            loadMessages()
        }
    }

    private fun sendImage(uri: Uri) {
        lifecycleScope.launch {
            Toast.makeText(this@GroupChatActivity, "كنرفعو الصورة...", Toast.LENGTH_SHORT).show()
            val result = ImageUploader.uploadImage(this@GroupChatActivity, uri)
            val url = result.getOrNull()
            if (url.isNullOrEmpty()) {
                Toast.makeText(this@GroupChatActivity, "فشل رفع الصورة", Toast.LENGTH_SHORT).show()
                return@launch
            }
            repository.sendGroupImageMessage(groupId, url)
            loadMessages()
        }
    }

    private fun openSenderProfile(message: com.etudiantsdroitmaroc.app.data.model.GroupMessage) {
        val intent = android.content.Intent(this, com.etudiantsdroitmaroc.app.ui.userprofile.UserProfileActivity::class.java)
        intent.putExtra("uid", message.senderUid)
        startActivity(intent)
    }

    private fun confirmDelete(message: GroupMessage) {
        AlertDialog.Builder(this)
            .setTitle("حذف الرسالة")
            .setMessage("واش متأكد بغيتي تحيد هاد الرسالة؟")
            .setPositiveButton("حذف") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteGroupMessage(groupId, message.id)
                    loadMessages()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
