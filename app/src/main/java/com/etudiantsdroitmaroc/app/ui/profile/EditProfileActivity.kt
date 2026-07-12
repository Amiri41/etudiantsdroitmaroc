package com.etudiantsdroitmaroc.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.databinding.ActivityEditProfileBinding
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var pickedImageUri: Uri? = null
    private var uploadedPhotoUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            Glide.with(this).load(uri).into(binding.ivProfilePhoto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.ivEditPhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.ivProfilePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }

        loadCurrentProfile()

        binding.btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadCurrentProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val doc = Firebase.firestore.collection("users").document(uid).get().await()
                val profile = doc?.toObject<UserProfile>()
                binding.etName.setText(profile?.name ?: FirebaseAuth.getInstance().currentUser?.displayName)
                binding.etUniversity.setText(profile?.university)
                binding.etLevel.setText(profile?.level)
                val photoUrl = profile?.photoUrl ?: FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this@EditProfileActivity).load(photoUrl).into(binding.ivProfilePhoto)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val university = binding.etUniversity.text?.toString()?.trim().orEmpty()
        val level = binding.etLevel.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            Toast.makeText(this, "دخل الاسم", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                var photoUrl: String? = null

                if (pickedImageUri != null) {
                    val result = ImageUploader.uploadImage(this@EditProfileActivity, pickedImageUri!!)
                    if (result.isSuccess) {
                        photoUrl = result.getOrNull()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "فشل رفع الصورة، تحقق من مفتاح imgbb", Toast.LENGTH_LONG).show()
                    }
                }

                val user = FirebaseAuth.getInstance().currentUser
                val profileUpdateBuilder = UserProfileChangeRequest.Builder().setDisplayName(name)
                if (photoUrl != null) profileUpdateBuilder.setPhotoUri(Uri.parse(photoUrl))
                user?.updateProfile(profileUpdateBuilder.build())?.await()

                val data = mutableMapOf<String, Any>(
                    "uid" to (user?.uid ?: ""),
                    "name" to name,
                    "email" to (user?.email ?: ""),
                    "university" to university,
                    "level" to level
                )
                if (photoUrl != null) data["photoUrl"] = photoUrl

                Firebase.firestore.collection("users").document(user?.uid ?: "")
                    .set(data, com.google.firebase.firestore.SetOptions.merge()).await()

                Toast.makeText(this@EditProfileActivity, "تم الحفظ ✅", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }
}

