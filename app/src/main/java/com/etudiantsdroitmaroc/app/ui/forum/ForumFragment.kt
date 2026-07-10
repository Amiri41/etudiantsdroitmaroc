package com.etudiantsdroitmaroc.app.ui.forum

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.databinding.DialogNewPostBinding
import com.etudiantsdroitmaroc.app.databinding.FragmentForumBinding
import kotlinx.coroutines.launch

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val repository = ForumRepository()
    private lateinit var adapter: ForumFeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ForumFeedAdapter(requireContext(), emptyList())
        binding.rvPosts.layoutManager = LinearLayoutManager(context)
        binding.rvPosts.adapter = adapter

        binding.fabNewPost.setOnClickListener { showNewPostDialog() }

        loadPosts()
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            try {
                val posts = repository.getPosts()
                adapter.updateData(posts)
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showNewPostDialog() {
        val dialogBinding = DialogNewPostBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnPublish.setOnClickListener {
            val content = dialogBinding.etPostContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) {
                Toast.makeText(context, "اكتب شي حاجة قبل النشر", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val result = repository.createPost(content)
                if (result.isSuccess) {
                    dialog.dismiss()
                    loadPosts()
                } else {
                    Toast.makeText(context, "فشل النشر، تأكد من الانترنت", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
