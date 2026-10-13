package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.content.Intent
import com.etudiantsdroitmaroc.app.ui.MainActivity

/** رجوع مباشر للصفحة الرئيسية: كيسد كل الصفحات الداخلية وكيخلي MainActivity الموجودة */
fun Activity.goHome() {
    val intent = Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    startActivity(intent)
    finish()
}
