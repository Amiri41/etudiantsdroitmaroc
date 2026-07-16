package com.etudiantsdroitmaroc.app.ui.friends

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.FriendRequest
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.FragmentFriendsBinding
import com.etudiantsdroitmaroc.app.ui.userprofile.UserProfileActivity
import kotlinx.coroutines.launch

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val repository = ChatRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPeople.layoutManager = LinearLayoutManager(context)

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipAll.id -> loadSuggestions()
                binding.chipFriends.id -> loadFriends()
                binding.chipRequests.id -> loadRequests()
            }
        }

        loadSuggestions()
    }

    override fun onResume() {
        super.onResume()
        // نعاود نحدث الحالة الحالية ملي المستخدم يرجع لهاد الصفحة
        when (binding.chipGroupFilter.checkedChipId) {
            binding.chipAll.id -> loadSuggestions()
            binding.chipFriends.id -> loadFriends()
            binding.chipRequests.id -> loadRequests()
        }
    }

    private fun openProfile(profile: UserProfile) {
        val intent = Intent(requireContext(), UserProfileActivity::class.java)
        intent.putExtra("uid", profile.uid)
        startActivity(intent)
    }

    private fun loadSuggestions() {
        lifecycleScope.launch {
            try {
                val allUsers = repository.getAllUsers()
                val friendUids = repository.getMyFriendsUids()

                val items = allUsers.map { profile ->
                    val status = when {
                        friendUids.contains(profile.uid) -> "friend"
                        else -> "none"
                    }
                    PersonItem(profile, status)
                }

                // نفحصو الطلبات المعلقة (لكل شخص ماشي صديق بعد) بصفة موازية بسيطة
                val finalItems = items.map { item ->
                    if (item.status == "none") {
                        val pending = repository.hasPendingRequestTo(item.profile.uid)
                        if (pending) item.copy(status = "pending") else item
                    } else item
                }

                showPeople(finalItems, PersonMode.SUGGESTION)
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFriends() {
        lifecycleScope.launch {
            try {
                val friends = repository.getMyFriends()
                showPeople(friends.map { PersonItem(it, "friend") }, PersonMode.FRIEND)
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPeople(items: List<PersonItem>, mode: PersonMode) {
        val adapter = PeopleAdapter(
            items,
            mode,
            onClickProfile = { openProfile(it) },
            onPrimaryAction = { item ->
                if (mode == PersonMode.SUGGESTION) sendRequest(item)
                else removeFriend(item)
            }
        )
        binding.rvPeople.adapter = adapter
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sendRequest(item: PersonItem) {
        lifecycleScope.launch {
            try {
                repository.sendFriendRequest(item.profile.uid)
                Toast.makeText(context, "تم بعث طلب الصداقة ✅", Toast.LENGTH_SHORT).show()
                loadSuggestions()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeFriend(item: PersonItem) {
        lifecycleScope.launch {
            try {
                repository.removeFriend(item.profile.uid)
                Toast.makeText(context, "تمت إزالة الصديق", Toast.LENGTH_SHORT).show()
                loadFriends()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            try {
                val requests = repository.getIncomingRequests()
                val adapter = FriendRequestAdapter(
                    requests,
                    onAccept = { acceptRequest(it) },
                    onDecline = { declineRequest(it) }
                )
                binding.rvPeople.adapter = adapter
                binding.tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun acceptRequest(request: FriendRequest) {
        lifecycleScope.launch {
            try {
                repository.acceptFriendRequest(request.fromUid)
                Toast.makeText(context, "أصبحتوا أصدقاء ✅", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun declineRequest(request: FriendRequest) {
        lifecycleScope.launch {
            try {
                repository.declineFriendRequest(request.fromUid)
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
