package com.etudiantsdroitmaroc.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etudiantsdroitmaroc.app.databinding.ActivityPromoteServiceBinding

/** طلب ترويج خدمة: نفس المنطق ديال النافذة القديمة (التحقق ثم إيميل لفريق التطبيق) فصفحة كاملة */
class PromoteServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPromoteServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSendPromoteRequest.setOnClickListener {
            val serviceName = binding.etServiceName.text?.toString()?.trim().orEmpty()
            val description = binding.etServiceDescription.text?.toString()?.trim().orEmpty()
            val contact = binding.etContactInfo.text?.toString()?.trim().orEmpty()

            if (serviceName.isEmpty() || description.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "عبي جميع الخانات", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailBody = """
                اسم الخدمة: $serviceName
                الوصف: $description
                التواصل: $contact
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("etudiantsendroitmorocco@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "طلب ترويج خدمة - $serviceName")
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }

            try {
                startActivity(emailIntent)
                finish()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(this, "ماكاينش تطبيق إيميل مثبت", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
