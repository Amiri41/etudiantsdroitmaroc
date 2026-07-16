package com.etudiantsdroitmaroc.app.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.ChatThread
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.FragmentChatListBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val repository = ChatRepository()
    private lateinit var threadAdapter: ThreadAdapter
    private lateinit var onlineAdapter: OnlineUserAdapter
    private var allThreads: List<ChatThread> = emptyList()
    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onlineAdapter = OnlineUserAdapter(emptyList()) { user -> openUserProfile(user) }
        binding.rvOnlineUsers.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvOnlineUsers.adapter = onlineAdapter

        threadAdapter = ThreadAdapter(emptyList()) { thread -> openThread(thread) }
        binding.rvThreads.layoutManager = LinearLayoutManager(context)
        binding.rvThreads.adapter = threadAdapter

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                filterThreads(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadData()
    }

    private fun filterThreads(query: String) {
        val filtered = if (query.isBlank()) {
            allThreads
        } else {
            allThreads.filter { thread ->
                val otherUid = thread.participantUids.firstOrNull { it != myUid } ?: ""
                val otherName = thread.participantNames[otherUid] ?: ""
                otherName.contains(query, ignoreCase = true)
            }
        }
        threadAdapter.updateData(filtered)
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                onlineAdapter.updateData(repository.getOnlineUsers())
                allThreads = repository.getMyThreads()
                filterThreads(binding.etSearch.text?.toString().orEmpty())
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ Firestore: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openUserProfile(user: UserProfile) {
        val intent = Intent(requireContext(), com.etudiantsdroitmaroc.app.ui.userprofile.UserProfileActivity::class.java)
        intent.putExtra("uid", user.uid)
        startActivity(intent)
    }

    private fun openThreadWith(user: UserProfile) {
        lifecycleScope.launch {
            val threadId = repository.getOrCreateThread(user.uid, user.name)
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("threadId", threadId)
            intent.putExtra("otherName", user.name)
            intent.putExtra("otherUid", user.uid)
            startActivity(intent)
        }
    }

    private fun openThread(thread: ChatThread) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val otherUid = thread.participantUids.firstOrNull { it != myUid } ?: ""
        val otherName = thread.participantNames[otherUid] ?: "طالب"

        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("threadId", thread.id)
        intent.putExtra("otherName", otherName)
        intent.putExtra("otherUid", otherUid)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
