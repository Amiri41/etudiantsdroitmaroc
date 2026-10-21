package com.etudiantsdroitmaroc.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * تخزين محلي للرسائل (بحال واتساب): الرسالة ملي كتوصل كتتسجل فالهاتف وكتبقى حتى إلا تبدل شي فالسيرفر
 * (فشل قراءة، كاش تمسح، رفض صلاحية...). الواجهة كتقرا من هنا، والسيرفر كيزيد/كيحدث فقط.
 */
@Entity(tableName = "chat_messages", indices = [Index(value = ["threadId", "sentAt"])])
data class LocalMessage(
    @PrimaryKey val id: String,
    val threadId: String,
    val senderUid: String,
    val text: String,
    val type: String,
    val imageUrl: String,
    val sentAt: Long,
    val seen: Boolean,
    val deliveredAt: Long,
    val seenAt: Long,
    val pending: Boolean,          // مكتوبة محليا وما تأكدتش من السيرفر بعد
    val participantUids: String    // UID مفصولة بفاصلة
)

@Dao
interface LocalMessageDao {

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY sentAt ASC")
    fun observeThread(threadId: String): Flow<List<LocalMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<LocalMessage>)

    /** رسائل مازال "قيد الإرسال" وما ظهراتش فنتيجة مؤكدة من السيرفر: الإرسال فشل، كنحيدوها */
    @Query("DELETE FROM chat_messages WHERE threadId = :threadId AND pending = 1 AND id NOT IN (:confirmedIds)")
    suspend fun deleteFailedPending(threadId: String, confirmedIds: List<String>)

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun clearThread(threadId: String)
}

@Database(entities = [LocalMessage::class], version = 1, exportSchema = false)
abstract class ChatLocalDatabase : RoomDatabase() {

    abstract fun messageDao(): LocalMessageDao

    companion object {
        @Volatile private var INSTANCE: ChatLocalDatabase? = null

        fun getInstance(context: Context): ChatLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatLocalDatabase::class.java,
                    "chat_local.db"
                ).build().also { INSTANCE = it }
            }
        }

        /** عند تسجيل الخروج/حذف الحساب: كنمسحو الرسائل المحلية باش ما تبقاش لمستخدم آخر فنفس الهاتف */
        fun wipeAsync(context: Context) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try { getInstance(app).clearAllTables() } catch (_: Exception) { }
            }
        }
    }
}

fun ChatMessage.toLocal(threadId: String, pending: Boolean) = LocalMessage(
    id = id,
    threadId = threadId,
    senderUid = senderUid,
    text = text,
    type = type,
    imageUrl = imageUrl,
    sentAt = sentAt,
    seen = seen,
    deliveredAt = deliveredAt,
    seenAt = seenAt,
    pending = pending,
    participantUids = participantUids.joinToString(",")
)

fun LocalMessage.toChatMessage() = ChatMessage(
    id = id,
    senderUid = senderUid,
    text = text,
    type = type,
    imageUrl = imageUrl,
    sentAt = sentAt,
    seen = seen,
    deliveredAt = deliveredAt,
    seenAt = seenAt,
    participantUids = if (participantUids.isEmpty()) emptyList() else participantUids.split(","),
    pending = pending
)
