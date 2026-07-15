package com.etudiantsdroitmaroc.app.ui.friends

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.FriendRequest
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityFriendRequestsBinding
import kotlinx.coroutines.launch

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendRequestsBinding
    private val repository = ChatRepository()
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = FriendRequestAdapter(emptyList(), ::onAccept, ::onDecline)
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            try {
                val requests = repository.getIncomingRequests()
                adapter.updateData(requests)
                binding.tvEmpty.visibility = if (requests.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onAccept(request: FriendRequest) {
        lifecycleScope.launch {
            try {
                repository.acceptFriendRequest(request.fromUid)
                Toast.makeText(this@FriendRequestsActivity, "أصبحتوا أصدقاء ✅", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onDecline(request: FriendRequest) {
        lifecycleScope.launch {
            try {
                repository.declineFriendRequest(request.fromUid)
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
