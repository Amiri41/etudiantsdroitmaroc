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
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.FragmentProfileBinding
import com.etudiantsdroitmaroc.app.ui.auth.LoginActivity
import com.etudiantsdroitmaroc.app.ui.friends.FriendRequestsActivity
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

        binding.btnFriendRequests.setOnClickListener {
            startActivity(Intent(requireContext(), FriendRequestsActivity::class.java))
        }

        loadFriendRequestsCount()

        binding.btnLogout.setOnClickListener {
            AuthRepository(requireContext()).signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
        loadFriendRequestsCount()
    }

    private fun loadFriendRequestsCount() {
        lifecycleScope.launch {
            try {
                val count = ChatRepository().getIncomingRequestsCount()
                binding.btnFriendRequests.text = if (count > 0) "طلبات الصداقة ($count)" else "طلبات الصداقة"
            } catch (_: Exception) {
            }
        }
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        binding.tvName.text = user.displayName ?: ""
        // الإيميل خاص، ما كنعرضوش فالواجهة
        user.photoUrl?.let { Glide.with(this).load(it).into(binding.ivProfilePhoto) }

        lifecycleScope.launch {
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
