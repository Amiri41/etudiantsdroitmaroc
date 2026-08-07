package com.etudiantsdroitmaroc.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.data.remote.AuthRepository
import com.etudiantsdroitmaroc.app.databinding.DialogPromoteServiceBinding
import com.etudiantsdroitmaroc.app.databinding.FragmentProfileBinding
import com.etudiantsdroitmaroc.app.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(requireContext(), binding.bannerAdContainer)
        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(requireContext(), binding.bannerAdContainerBottom)

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), com.etudiantsdroitmaroc.app.ui.profile.EditProfileActivity::class.java))
        }

        binding.btnPages.setOnClickListener {
            startActivity(Intent(requireContext(), com.etudiantsdroitmaroc.app.ui.pages.PagesListActivity::class.java))
        }

        binding.btnMyPosts.setOnClickListener {
            val myUid = FirebaseAuth.getInstance().currentUser?.uid
            if (myUid != null) {
                val intent = Intent(requireContext(), com.etudiantsdroitmaroc.app.ui.userprofile.UserProfileActivity::class.java)
                intent.putExtra("uid", myUid)
                startActivity(intent)
            }
        }

        binding.btnPromoteService.setOnClickListener {
            showPromoteServiceDialog()
        }

        binding.btnLogout.setOnClickListener {
            AuthRepository(requireContext()).signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        binding.btnDarkMode.setOnClickListener {
            showDarkModeDialog()
        }

        binding.btnDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

        loadProfile()
    }

    private fun showDarkModeDialog() {
        val options = arrayOf("فاتح ☀️", "داكن 🌙", "حسب النظام (افتراضي)")
        val values = arrayOf("light", "dark", "system")
        val current = com.etudiantsdroitmaroc.app.utils.DarkModeHelper.getSavedMode(requireContext())
        val checkedIndex = values.indexOf(current).coerceAtLeast(0)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("مظهر التطبيق")
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                com.etudiantsdroitmaroc.app.utils.DarkModeHelper.setMode(requireContext(), values[which])
                dialog.dismiss()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeleteAccount() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("حذف الحساب نهائيا")
            .setMessage("هاد العملية ماغاديش ترجع فيها. غادي يتحذف حسابك، البروفايل، والمنشورات ديالك. متأكد بغيتي تكمل؟")
            .setPositiveButton("حذف نهائيا") { _, _ -> deleteAccount() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // نحذفو البروفايل من Firestore قبل حذف الحساب
                Firebase.firestore.collection("users").document(uid).delete().await()
                user.delete().await()
                android.widget.Toast.makeText(context, "تم حذف الحساب", android.widget.Toast.LENGTH_SHORT).show()
                AuthRepository(requireContext()).signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                android.widget.Toast.makeText(
                    context,
                    "خاصك تسجل الدخول مرة أخرى قبل الحذف (لأسباب أمان). سجل الخروج ثم الدخول وعاود جرب.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun showPromoteServiceDialog() {
        val dialogBinding = DialogPromoteServiceBinding.inflate(LayoutInflater.from(context))
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSendPromoteRequest.setOnClickListener {
            val serviceName = dialogBinding.etServiceName.text?.toString()?.trim().orEmpty()
            val description = dialogBinding.etServiceDescription.text?.toString()?.trim().orEmpty()
            val contact = dialogBinding.etContactInfo.text?.toString()?.trim().orEmpty()

            if (serviceName.isEmpty() || description.isEmpty() || contact.isEmpty()) {
                android.widget.Toast.makeText(context, "عبي جميع الخانات", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailBody = """
                اسم الخدمة: $serviceName
                الوصف: $description
                التواصل: $contact
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("etudiantsendroitmorocco@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "طلب ترويج خدمة - $serviceName")
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }

            try {
                startActivity(emailIntent)
                dialog.dismiss()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "ماكاينش تطبيق إيميل مثبت", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        binding.tvName.text = user.displayName ?: ""
        // الإيميل خاص، ما كنعرضوش فالواجهة
        user.photoUrl?.let { Glide.with(this).load(it).into(binding.ivProfilePhoto) }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val doc = Firebase.firestore.collection("users").document(user.uid).get().await()
                val profile = doc.toObject<UserProfile>()
                if (profile != null) {
                    val parts = listOfNotNull(
                        profile.university.takeIf { it.isNotEmpty() },
                        profile.level.takeIf { it.isNotEmpty() }
                    )
                    binding.tvUniversityLevel.text = parts.joinToString(" · ")
                    if (profile.photoUrl.isNotEmpty()) {
                        Glide.with(this@ProfileFragment).load(profile.photoUrl).into(binding.ivProfilePhoto)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
