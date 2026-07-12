package com.etudiantsdroitmaroc.app.ui.pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.etudiantsdroitmaroc.app.databinding.ActivityPageDetailBinding

class PageDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = intent.getStringExtra("title") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvContent.text = intent.getStringExtra("content") ?: ""
    }
}
