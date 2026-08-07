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

    private var currentPeopleItems: List<PersonItem> = emptyList()
    private var currentPeopleMode: PersonMode = PersonMode.SUGGESTION
    private var currentRequests: List<FriendRequest> = emptyList()
    private var isRequestsTab = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPeople.layoutManager = LinearLayoutManager(context)

        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(requireContext(), binding.bannerAdContainer)

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            binding.etSearch.setText("")
            when (checkedIds.firstOrNull()) {
                binding.chipAll.id -> loadSuggestions()
                binding.chipFriends.id -> loadFriends()
                binding.chipRequests.id -> loadRequests()
            }
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                applySearchFilter(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadSuggestions()
    }

    private fun applySearchFilter(query: String) {
        if (isRequestsTab) {
            val filtered = if (query.isBlank()) currentRequests
                else currentRequests.filter { it.fromName.contains(query, ignoreCase = true) }
            val adapter = FriendRequestAdapter(filtered, ::acceptRequest, ::declineRequest)
            binding.rvPeople.adapter = adapter
            binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        } else {
            val filtered = if (query.isBlank()) currentPeopleItems
                else currentPeopleItems.filter { it.profile.name.contains(query, ignoreCase = true) }
            bindPeopleAdapter(filtered, currentPeopleMode)
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val friends = repository.getMyFriends()
                showPeople(friends.map { PersonItem(it, "friend") }, PersonMode.FRIEND)
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPeople(items: List<PersonItem>, mode: PersonMode) {
        isRequestsTab = false
        currentPeopleItems = items
        currentPeopleMode = mode
        val query = binding.etSearch.text?.toString().orEmpty()
        val filtered = if (query.isBlank()) items
            else items.filter { it.profile.name.contains(query, ignoreCase = true) }
        bindPeopleAdapter(filtered, mode)
    }

    private fun bindPeopleAdapter(items: List<PersonItem>, mode: PersonMode) {
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
        viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val requests = repository.getIncomingRequests()
                isRequestsTab = true
                currentRequests = requests
                val query = binding.etSearch.text?.toString().orEmpty()
                val filtered = if (query.isBlank()) requests
                    else requests.filter { it.fromName.contains(query, ignoreCase = true) }
                val adapter = FriendRequestAdapter(
                    filtered,
                    onAccept = { acceptRequest(it) },
                    onDecline = { declineRequest(it) }
                )
                binding.rvPeople.adapter = adapter
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun acceptRequest(request: FriendRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
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
