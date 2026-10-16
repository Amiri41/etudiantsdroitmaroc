package com.etudiantsdroitmaroc.app.utils

/** حسابات رسمية كتبان ليها علامة التوثيق ✔ (حساب الإدارة، نفس المعرّف لي فـ firestore.rules) */
object OfficialAccounts {
    private val UIDS = setOf("pndM3rNT9DRVCPxODaHFFnBO9jU2")
    fun isOfficial(uid: String) = uid in UIDS
}
