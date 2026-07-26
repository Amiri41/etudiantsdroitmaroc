package com.etudiantsdroitmaroc.app.ui.userprofile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.Post
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityUserProfileBinding
import com.etudiantsdroitmaroc.app.ui.chat.ChatActivity
import com.etudiantsdroitmaroc.app.ui.forum.ForumFeedAdapter
import com.etudiantsdroitmaroc.app.ui.moderation.ReportDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val repository = ChatRepository()
    private val forumRepository = ForumRepository()
    private lateinit var targetUid: String
    private var targetName: String = ""
    private lateinit var postsAdapter: ForumFeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetUid = intent.getStringExtra("uid") ?: return
        binding.toolbar.setNavigationOnClickListener { finish() }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (targetUid == myUid) {
            // ماشي منطقي تشوف "إضافة صديق" لراسك
            binding.btnAddFriend.visibility = android.view.View.GONE
            binding.btnMessage.visibility = android.view.View.GONE
        } else {
            binding.toolbar.menu.add("الإبلاغ عن المستخدم")
            binding.toolbar.menu.add("حظر المستخدم")
            binding.toolbar.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "الإبلاغ عن المستخدم" -> {
                        ReportDialog.show(
                            this, lifecycleScope,
                            targetType = "user", targetId = targetUid,
                            targetOwnerUid = targetUid, targetOwnerName = targetName
                        )
                    }
                    "حظر المستخدم" -> {
                        ReportDialog.confirmBlock(this, lifecycleScope, targetUid, targetName) { finish() }
                    }
                }
                true
            }
        }

        postsAdapter = ForumFeedAdapter(
            this,
            emptyList(),
            onLikeClick = { post -> toggleLike(post) },
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> confirmDeletePost(post) },
            onReportClick = { post ->
                ReportDialog.show(
                    this, lifecycleScope,
                    targetType = "post", targetId = post.id,
                    targetOwnerUid = post.authorUid, targetOwnerName = post.authorName
                )
            },
            onBlockClick = { post ->
                ReportDialog.confirmBlock(this, lifecycleScope, post.authorUid, post.authorName) { loadUserPosts() }
            }
        )
        binding.rvUserPosts.layoutManager = LinearLayoutManager(this)
        binding.rvUserPosts.adapter = postsAdapter

        loadProfile()
        loadUserPosts()

        binding.btnAddFriend.setOnClickListener { addFriend() }
        binding.btnMessage.setOnClickListener { openChat() }
    }

    private fun loadUserPosts() {
        lifecycleScope.launch {
            try {
                val posts = forumRepository.getPostsByUser(targetUid)
                postsAdapter.updateData(posts)
                binding.tvNoPosts.visibility = if (posts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@UserProfileActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleLike(post: Post) {
        lifecycleScope.launch {
            forumRepository.toggleLike(post.id)
            loadUserPosts()
        }
    }

    private fun showEditPostDialog(post: Post) {
        val editText = android.widget.EditText(this)
        editText.setText(post.content)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("تعديل المنشور")
            .setView(editText)
            .setPositiveButton("حفظ") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    lifecycleScope.launch {
                        forumRepository.updatePost(post.id, newContent)
                        loadUserPosts()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeletePost(post: Post) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("حذف المنشور")
            .setMessage("متأكد بغيتي تحذف هاد المنشور؟")
            .setPositiveButton("حذف") { _, _ ->
                lifecycleScope.launch {
                    forumRepository.deletePost(post.id)
                    loadUserPosts()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val profile = repository.getUserProfile(targetUid)
                if (profile != null) {
                    targetName = profile.name
                    binding.tvName.text = profile.name
                    val parts = listOfNotNull(
                        profile.university.takeIf { it.isNotEmpty() },
                        profile.level.takeIf { it.isNotEmpty() }
                    )
                    binding.tvUniversityLevel.text = parts.joinToString(" · ")
                    if (profile.photoUrl.isNotEmpty()) {
                        Glide.with(this@UserProfileActivity).load(profile.photoUrl).into(binding.ivPhoto)
                    }
                }
                updateFriendButtonState()
            } catch (e: Exception) {
                Toast.makeText(this@UserProfileActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var requestPending = false

    private fun updateFriendButtonState() {
        lifecycleScope.launch {
            val isFriend = repository.isFriend(targetUid)
            if (isFriend) {
                binding.btnAddFriend.text = "أصدقاء ✓"
                binding.btnAddFriend.isEnabled = false
                return@launch
            }
            requestPending = repository.hasPendingRequestTo(targetUid)
            binding.btnAddFriend.isEnabled = true
            binding.btnAddFriend.text = if (requestPending) "إلغاء طلب الصداقة ✕" else "إضافة صديق ➕"
        }
    }

    private fun addFriend() {
        lifecycleScope.launch {
            try {
                if (requestPending) {
                    repository.cancelFriendRequest(targetUid)
                    Toast.makeText(this@UserProfileActivity, "تم إلغاء طلب الصداقة", Toast.LENGTH_SHORT).show()
                } else {
                    repository.sendFriendRequest(targetUid)
                    Toast.makeText(this@UserProfileActivity, "تم بعث طلب الصداقة ✅", Toast.LENGTH_SHORT).show()
                }
                updateFriendButtonState()
            } catch (e: Exception) {
                Toast.makeText(this@UserProfileActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // الدردشة ديما متاحة بحال واتساب، سواء كانو صحاب أو لا أو كاين طلب معلق

    private fun openChat() {
        lifecycleScope.launch {
            val threadId = repository.getOrCreateThread(targetUid, targetName)
            val intent = Intent(this@UserProfileActivity, ChatActivity::class.java)
            intent.putExtra("threadId", threadId)
            intent.putExtra("otherName", targetName)
            intent.putExtra("otherUid", targetUid)
            startActivity(intent)
        }
    }
}
