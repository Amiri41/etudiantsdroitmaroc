package com.etudiantsdroitmaroc.app.ui.forum

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityCommentsBinding
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private val repository = ForumRepository()
    private lateinit var adapter: CommentAdapter
    private lateinit var postId: String

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) sendImageComment(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra("postId") ?: return
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = CommentAdapter(emptyList())
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = adapter

        binding.fabSendComment.setOnClickListener { sendComment() }
        binding.btnAttachImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            adapter.updateData(repository.getComments(postId))
            binding.rvComments.scrollToPosition(maxOf(0, adapter.itemCount - 1))
        }
    }

    private fun sendComment() {
        val text = binding.etComment.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.etComment.setText("")
        lifecycleScope.launch {
            repository.addComment(postId, text)
            loadComments()
        }
    }

    private fun sendImageComment(uri: Uri) {
        lifecycleScope.launch {
            Toast.makeText(this@CommentsActivity, "كنرفعو الصورة...", Toast.LENGTH_SHORT).show()
            val result = ImageUploader.uploadImage(this@CommentsActivity, uri)
            val imageUrl = result.getOrNull()
            if (imageUrl.isNullOrEmpty()) {
                Toast.makeText(this@CommentsActivity, "فشل رفع الصورة", Toast.LENGTH_SHORT).show()
                return@launch
            }
            repository.addComment(postId, "", imageUrl)
            loadComments()
        }
    }
}
