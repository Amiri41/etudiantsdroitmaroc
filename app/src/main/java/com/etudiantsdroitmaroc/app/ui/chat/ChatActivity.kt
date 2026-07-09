package com.etudiantsdroitmaroc.app.ui.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityChatBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val repository = ChatRepository()
    private lateinit var adapter: MessageAdapter
    private lateinit var threadId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        threadId = intent.getStringExtra("threadId") ?: return
        binding.toolbar.title = intent.getStringExtra("otherName") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MessageAdapter(emptyList())
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.fabSend.setOnClickListener { sendMessage() }

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
}
