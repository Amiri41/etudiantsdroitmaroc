package com.etudiantsdroitmaroc.app.ui.pages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.AppPage
import com.etudiantsdroitmaroc.app.data.remote.PagesRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityPagesListBinding
import kotlinx.coroutines.launch

class PagesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagesListBinding
    private val repository = PagesRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = PageAdapter(emptyList()) { page -> openPage(page) }
        binding.rvPages.layoutManager = LinearLayoutManager(this)
        binding.rvPages.adapter = adapter

        lifecycleScope.launch {
            adapter.updateData(repository.getPages())
        }
    }

    private fun openPage(page: AppPage) {
        if (page.type == "link" && page.url.isNotEmpty()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page.url)))
        } else {
            val intent = Intent(this, PageDetailActivity::class.java)
            intent.putExtra("title", page.title)
            intent.putExtra("content", page.content)
            startActivity(intent)
        }
    }
}
