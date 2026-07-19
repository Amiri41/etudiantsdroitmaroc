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
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.data.remote.GroupRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityGroupSettingsBinding
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import kotlinx.coroutines.launch

class GroupSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupSettingsBinding
    private val repository = GroupRepository()
    private lateinit var groupId: String
    private lateinit var memberAdapter: GroupMemberAdapter
    private var pickedIconUri: Uri? = null

    private val pickIconLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedIconUri = uri
            Glide.with(this).load(uri).into(binding.ivGroupIconPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("groupId") ?: return
        binding.toolbar.setNavigationOnClickListener { finish() }

        memberAdapter = GroupMemberAdapter(emptyList()) { member -> confirmRemoveMember(member) }
        binding.rvMembers.layoutManager = LinearLayoutManager(this)
        binding.rvMembers.adapter = memberAdapter

        binding.btnChangeIcon.setOnClickListener { pickIconLauncher.launch("image/*") }
        binding.btnSaveGroupInfo.setOnClickListener { saveGroupInfo() }
        binding.btnDeleteGroup.setOnClickListener { confirmDeleteGroup() }

        loadGroupInfo()
        loadMembers()
    }

    private fun loadGroupInfo() {
        lifecycleScope.launch {
            val group = repository.getGroup(groupId) ?: return@launch
            binding.etGroupName.setText(group.name)
            binding.etGroupDescription.setText(group.description)
            if (group.iconUrl.isNotEmpty()) {
                Glide.with(this@GroupSettingsActivity).load(group.iconUrl).into(binding.ivGroupIconPreview)
            }
        }
    }

    private fun loadMembers() {
        lifecycleScope.launch {
            try {
                memberAdapter.updateData(repository.getGroupMembers(groupId))
            } catch (e: Exception) {
                Toast.makeText(this@GroupSettingsActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveGroupInfo() {
        val name = binding.etGroupName.text?.toString()?.trim().orEmpty()
        val description = binding.etGroupDescription.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            Toast.makeText(this, "دخل اسم المجموعة", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            var iconUrl = ""
            val iconUri = pickedIconUri
            if (iconUri != null) {
                val result = ImageUploader.uploadImage(this@GroupSettingsActivity, iconUri)
                iconUrl = result.getOrNull() ?: ""
            }
            try {
                repository.updateGroupInfo(groupId, name, description, iconUrl)
                Toast.makeText(this@GroupSettingsActivity, "تم الحفظ ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@GroupSettingsActivity, "فشل الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmRemoveMember(member: UserProfile) {
        AlertDialog.Builder(this)
            .setTitle("حظر العضو")
            .setMessage("متأكد بغيتي تحيد ${member.name} من المجموعة؟")
            .setPositiveButton("حظر") { _, _ ->
                lifecycleScope.launch {
                    repository.removeMember(groupId, member.uid)
                    loadMembers()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeleteGroup() {
        AlertDialog.Builder(this)
            .setTitle("حذف المجموعة")
            .setMessage("غادي يتمسح كل شيء (الرسائل والمجموعة) بلا رجعة. متأكد؟")
            .setPositiveButton("حذف نهائيا") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteGroup(groupId)
                        Toast.makeText(this@GroupSettingsActivity, "تم حذف المجموعة", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@GroupSettingsActivity, "فشل الحذف: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
