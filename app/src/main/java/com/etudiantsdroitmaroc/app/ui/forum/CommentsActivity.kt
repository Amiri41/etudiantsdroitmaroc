package com.etudiantsdroitmaroc.app.ui.forum

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityCommentsBinding
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private val repository = ForumRepository()
    private lateinit var adapter: CommentAdapter
    private lateinit var postId: String

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

        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            adapter.updateData(repository.getComments(postId))
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
}
