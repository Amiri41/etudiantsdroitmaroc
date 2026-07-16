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
import com.etudiantsdroitmaroc.app.data.model.Post
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.databinding.DialogNewPostBinding
import com.etudiantsdroitmaroc.app.databinding.FragmentForumBinding
import kotlinx.coroutines.launch

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val repository = ForumRepository()
    private lateinit var adapter: ForumFeedAdapter
    private var allPosts: List<Post> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ForumFeedAdapter(
            requireContext(), emptyList(),
            onLikeClick = { post ->
                lifecycleScope.launch {
                    try {
                        repository.toggleLike(post.id)
                        loadPosts()
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> confirmDeletePost(post) }
        )
        binding.rvPosts.layoutManager = LinearLayoutManager(context)
        binding.rvPosts.adapter = adapter

        binding.fabNewPost.setOnClickListener { showNewPostDialog() }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                filterPosts(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadPosts()
    }

    private fun filterPosts(query: String) {
        val filtered = if (query.isBlank()) {
            allPosts
        } else {
            allPosts.filter {
                it.content.contains(query, ignoreCase = true) ||
                    it.authorName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filtered)
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            try {
                allPosts = repository.getPosts()
                filterPosts(binding.etSearch.text?.toString().orEmpty())
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

    private fun showEditPostDialog(post: Post) {
        val dialogBinding = DialogNewPostBinding.inflate(LayoutInflater.from(context))
        dialogBinding.etPostContent.setText(post.content)
        dialogBinding.btnPublish.text = "حفظ التعديل"
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnPublish.setOnClickListener {
            val content = dialogBinding.etPostContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) return@setOnClickListener
            lifecycleScope.launch {
                val result = repository.updatePost(post.id, content)
                if (result.isSuccess) {
                    dialog.dismiss()
                    loadPosts()
                } else {
                    Toast.makeText(context, "فشل التعديل", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeletePost(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف المنشور")
            .setMessage("متأكد بغيتي تحذف هاد المنشور؟")
            .setPositiveButton("حذف") { _, _ ->
                lifecycleScope.launch {
                    repository.deletePost(post.id)
                    loadPosts()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
