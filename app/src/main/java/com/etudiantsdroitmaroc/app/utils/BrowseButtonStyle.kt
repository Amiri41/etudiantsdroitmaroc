package com.etudiantsdroitmaroc.app.utils

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.etudiantsdroitmaroc.app.R
import com.google.android.material.button.MaterialButton

/** الزر الأول فاللائحة ذهبي (بحال زر "تعديل البروفايل")، والباقي كحلي */
fun MaterialButton.applyGoldStyle() {
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gold))
    setTextColor(ContextCompat.getColor(context, R.color.navy_dark))
}
