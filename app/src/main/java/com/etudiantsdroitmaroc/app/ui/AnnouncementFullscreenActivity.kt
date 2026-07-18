package com.etudiantsdroitmaroc.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.databinding.ActivityAnnouncementFullscreenBinding

class AnnouncementFullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnnouncementFullscreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("type") ?: "promo"
        val title = intent.getStringExtra("title") ?: ""
        val message = intent.getStringExtra("message") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val linkUrl = intent.getStringExtra("linkUrl") ?: ""

        binding.tvBadge.text = if (type == "update") "🚀 تحديث جديد" else "🎁 عرض خاص"
        binding.tvTitle.text = title
        binding.tvMessage.text = message

        if (imageUrl.isNotEmpty()) {
            binding.ivAnnouncementImage.visibility = View.VISIBLE
            Glide.with(this).load(imageUrl).into(binding.ivAnnouncementImage)
        }

        if (linkUrl.isNotEmpty()) {
            binding.btnAction.visibility = View.VISIBLE
            binding.btnAction.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)))
            }
        }

        binding.btnClose.setOnClickListener { finish() }
    }
}
