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
import com.etudiantsdroitmaroc.app.databinding.FragmentProfileBinding
import com.etudiantsdroitmaroc.app.ui.auth.LoginActivity
import com.etudiantsdroitmaroc.app.utils.getFast
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

        binding.ivCameraBadge.setOnClickListener { binding.btnEditProfile.performClick() }
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
            startActivity(Intent(requireContext(), PromoteServiceActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            AuthRepository(requireContext()).signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        binding.btnDarkMode.setOnClickListener {
            showDarkModeDialog()
        }

        updateIntroSoundButton()
        binding.btnIntroSound.setOnClickListener {
            val ctx = requireContext()
            com.etudiantsdroitmaroc.app.utils.IntroSound.setEnabled(ctx, !com.etudiantsdroitmaroc.app.utils.IntroSound.isEnabled(ctx))
            updateIntroSoundButton()
        }

        binding.btnDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

        loadProfile()
    }

    private fun updateIntroSoundButton() {
        val on = com.etudiantsdroitmaroc.app.utils.IntroSound.isEnabled(requireContext())
        binding.tvIntroSoundState.text = if (on) "مفعّل" else "موقوف"
    }

    private fun showDarkModeDialog() {
        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(com.etudiantsdroitmaroc.app.R.layout.dialog_theme_mode, null)
        val values = listOf("light", "dark", "system")
        val rows = listOf(
            com.etudiantsdroitmaroc.app.R.id.optLight,
            com.etudiantsdroitmaroc.app.R.id.optDark,
            com.etudiantsdroitmaroc.app.R.id.optSystem
        )
        val checks = listOf(
            com.etudiantsdroitmaroc.app.R.id.chkLight,
            com.etudiantsdroitmaroc.app.R.id.chkDark,
            com.etudiantsdroitmaroc.app.R.id.chkSystem
        )
        val current = com.etudiantsdroitmaroc.app.utils.DarkModeHelper.getSavedMode(ctx)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(ctx).setView(view).create()

        rows.forEachIndexed { i, id ->
            val row = view.findViewById<View>(id)
            val selected = values[i] == current
            row.setBackgroundResource(
                if (selected) com.etudiantsdroitmaroc.app.R.drawable.bg_option_selected
                else com.etudiantsdroitmaroc.app.R.drawable.bg_option
            )
            view.findViewById<View>(checks[i]).visibility = if (selected) View.VISIBLE else View.INVISIBLE
            row.setOnClickListener {
                com.etudiantsdroitmaroc.app.utils.DarkModeHelper.setMode(ctx, values[i])
                dialog.dismiss()
            }
        }
        view.findViewById<View>(com.etudiantsdroitmaroc.app.R.id.btnCancelTheme).setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun confirmDeleteAccount() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("حذف الحساب نهائيا")
            .setMessage("هاد العملية ماغاديش ترجع فيها. غادي يتحذف حسابك، البروفايل، الأصدقاء والمنشورات ديالك. التعليقات والرسائل الخاصة اللي بعتي كتبقى، وتقدر تطلب مسحها بالإيميل. متأكد بغيتي تكمل؟")
            .setPositiveButton("حذف نهائيا") { _, _ -> deleteAccount() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        // Firebase كيطلب دخول حديث لحذف الحساب: نتأكدو قبل ما نمسحو أي بيانات
        val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
        if (System.currentTimeMillis() - lastSignIn > 4 * 60 * 1000L) {
            android.widget.Toast.makeText(
                context,
                "خاصك تسجل الدخول مرة أخرى قبل الحذف (لأسباب أمان). سجل الخروج ثم الدخول وعاود جرب.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = Firebase.firestore
                // المنشورات ديال المستعمل
                db.collection("posts").whereEqualTo("authorUid", uid).get().await()
                    .documents.forEach { it.reference.delete().await() }
                // الأصدقاء والطلبات والحظر
                for (sub in listOf("friends", "incoming_requests", "blocked")) {
                    try {
                        db.collection("users").document(uid).collection(sub).get().await()
                            .documents.forEach { it.reference.delete().await() }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                    }
                }
                // البروفايل من Firestore قبل حذف الحساب
                db.collection("users").document(uid).delete().await()
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
            if (e is kotlinx.coroutines.CancellationException) throw e
                android.widget.Toast.makeText(context, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        binding.tvName.text = user.displayName ?: ""
        // الإيميل خاص، ما كنعرضوش فالواجهة
        user.photoUrl?.let { Glide.with(this).load(it).into(binding.ivProfilePhoto) }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val doc = Firebase.firestore.collection("users").document(user.uid).getFast()
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
