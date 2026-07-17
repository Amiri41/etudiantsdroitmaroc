package com.etudiantsdroitmaroc.app.ui.forum

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.ForumGroup
import com.etudiantsdroitmaroc.app.data.model.Post
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.data.remote.GroupRepository
import com.etudiantsdroitmaroc.app.databinding.DialogCreateGroupBinding
import com.etudiantsdroitmaroc.app.databinding.DialogNewPostBinding
import com.etudiantsdroitmaroc.app.databinding.FragmentForumBinding
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val repository = ForumRepository()
    private val groupRepository = GroupRepository()
    private lateinit var adapter: ForumFeedAdapter
    private lateinit var groupAdapter: GroupAdapter
    private var allPosts: List<Post> = emptyList()
    private var pickedGroupIconUri: Uri? = null
    private var groupIconPreviewCallback: ((Uri) -> Unit)? = null
    private var pickedPostImageUri: Uri? = null
    private var postImagePreviewCallback: ((Uri) -> Unit)? = null

    private val pickPostImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedPostImageUri = uri
            postImagePreviewCallback?.invoke(uri)
        }
    }

    private val pickGroupIconLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedGroupIconUri = uri
            groupIconPreviewCallback?.invoke(uri)
        }
    }

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

        groupAdapter = GroupAdapter(
            emptyList(),
            isMember = { it.memberUids.contains(FirebaseAuth.getInstance().currentUser?.uid) },
            onOpen = { openGroupChat(it) },
            onJoin = { joinGroup(it) }
        )
        binding.rvGroups.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvGroups.adapter = groupAdapter
        binding.btnCreateGroup.setOnClickListener { showCreateGroupDialog() }
        loadGroups()

        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(requireContext(), binding.bannerAdContainer)

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
        pickedPostImageUri = null
        val dialogBinding = DialogNewPostBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        postImagePreviewCallback = { uri ->
            dialogBinding.ivPostImagePreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(dialogBinding.ivPostImagePreview)
        }
        dialogBinding.btnPickPostImage.setOnClickListener { pickPostImageLauncher.launch("image/*") }

        dialogBinding.btnPublish.setOnClickListener {
            val content = dialogBinding.etPostContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty() && pickedPostImageUri == null) {
                Toast.makeText(context, "اكتب شي حاجة أو زيد صورة قبل النشر", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                var imageUrl = ""
                val imageUri = pickedPostImageUri
                if (imageUri != null) {
                    val uploadResult = ImageUploader.uploadImage(requireContext(), imageUri)
                    imageUrl = uploadResult.getOrNull() ?: ""
                }
                val result = repository.createPost(content, imageUrl)
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

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                groupAdapter.updateData(groupRepository.getGroups())
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun joinGroup(group: ForumGroup) {
        lifecycleScope.launch {
            try {
                groupRepository.joinGroup(group.id)
                Toast.makeText(context, "انضميتي لمجموعة ${group.name} ✅", Toast.LENGTH_SHORT).show()
                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGroupChat(group: ForumGroup) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        val intent = Intent(requireContext(), GroupChatActivity::class.java)
        intent.putExtra("groupId", group.id)
        intent.putExtra("groupName", group.name)
        intent.putExtra("groupIcon", group.iconUrl)
        intent.putExtra("isAdmin", group.creatorUid == myUid)
        startActivity(intent)
    }

    private fun showCreateGroupDialog() {
        pickedGroupIconUri = null
        val dialogBinding = DialogCreateGroupBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        groupIconPreviewCallback = { uri ->
            Glide.with(this).load(uri).into(dialogBinding.ivGroupIconPreview)
        }

        dialogBinding.btnPickIcon.setOnClickListener { pickGroupIconLauncher.launch("image/*") }

        dialogBinding.btnCreateGroup.setOnClickListener {
            val name = dialogBinding.etGroupName.text?.toString()?.trim().orEmpty()
            val description = dialogBinding.etGroupDescription.text?.toString()?.trim().orEmpty()
            if (name.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "دخل اسم ووصف المجموعة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                var iconUrl = ""
                val iconUri = pickedGroupIconUri
                if (iconUri != null) {
                    val result = ImageUploader.uploadImage(requireContext(), iconUri)
                    iconUrl = result.getOrNull() ?: ""
                }
                try {
                    groupRepository.createGroup(name, description, iconUrl)
                    dialog.dismiss()
                    Toast.makeText(context, "تم إنشاء المجموعة ✅", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } catch (e: Exception) {
                    Toast.makeText(context, "فشل إنشاء المجموعة: ${e.message}", Toast.LENGTH_SHORT).show()
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
