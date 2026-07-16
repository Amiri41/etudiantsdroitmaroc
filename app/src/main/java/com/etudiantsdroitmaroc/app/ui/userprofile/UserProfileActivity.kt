package com.etudiantsdroitmaroc.app.ui.userprofile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityUserProfileBinding
import com.etudiantsdroitmaroc.app.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val repository = ChatRepository()
    private lateinit var targetUid: String
    private var targetName: String = ""

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
        }

        loadProfile()

        binding.btnAddFriend.setOnClickListener { addFriend() }
        binding.btnMessage.setOnClickListener { openChat() }
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

    private fun updateFriendButtonState() {
        lifecycleScope.launch {
            val isFriend = repository.isFriend(targetUid)
            if (isFriend) {
                binding.btnAddFriend.text = "أصدقاء ✓"
                binding.btnAddFriend.isEnabled = false
                return@launch
            }
            val pending = repository.hasPendingRequestTo(targetUid)
            if (pending) {
                binding.btnAddFriend.text = "طلب الصداقة مرسل ⏳"
                binding.btnAddFriend.isEnabled = false
            }
        }
    }

    private fun addFriend() {
        lifecycleScope.launch {
            try {
                repository.sendFriendRequest(targetUid)
                binding.btnAddFriend.text = "طلب الصداقة مرسل ⏳"
                binding.btnAddFriend.isEnabled = false
                Toast.makeText(this@UserProfileActivity, "تم بعث طلب الصداقة ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@UserProfileActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
