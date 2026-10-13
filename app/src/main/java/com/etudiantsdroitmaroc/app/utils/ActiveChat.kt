package com.etudiantsdroitmaroc.app.utils

/** المحادثة المفتوحة دابا (باش ما نبينوش إشعار لرسالة فمحادثة نتا فيها) */
object ActiveChat {
    @Volatile var threadId: String? = null
}
