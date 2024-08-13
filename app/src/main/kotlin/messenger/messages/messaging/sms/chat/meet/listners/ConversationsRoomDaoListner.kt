package messenger.messages.messaging.sms.chat.meet.listners

import androidx.room.*
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.model.MessagesModel

@Dao
interface ConversationsRoomDaoListner {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMessage(conversation: ConversationSmsModel): Long

    @Query("SELECT * FROM conversations")
    fun getAllList(): List<ConversationSmsModel>

    @Query("SELECT * FROM conversations WHERE read = 0")
    fun getUnreadConversationsMessage(): List<ConversationSmsModel>

    @Query("SELECT * FROM conversations WHERE title LIKE :text")
    fun getConversationsWithTextMessage(text: String): List<ConversationSmsModel>

    @Query("UPDATE conversations SET read = 1 WHERE thread_id = :threadId")
    fun markReadAsMessage(threadId: Long)

    @Query("UPDATE conversations SET read = 0 WHERE thread_id = :threadId")
    fun markUnreadAsMessage(threadId: Long)

    @Query("DELETE FROM conversations WHERE thread_id = :threadId")
    fun deleteThreadIdMessage(threadId: Long)

    @Query("DELETE FROM conversations")
    fun deleteAllConversation()

    @Transaction
    fun insertAllInConversationTransaction(items: List<ConversationSmsModel>) {
        insertAll(items)
    }

    @Query("SELECT * FROM conversations WHERE REPLACE(phone_number,'+','') LIKE :number")
    fun getDataFromNumber(number: String): ConversationSmsModel

    @Query("SELECT * FROM conversations WHERE thread_id = :threadId")
    fun getDataFromThreadId(threadId: Long): ConversationSmsModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ConversationSmsModel>)

    @Query("SELECT * FROM conversations WHERE snippet LIKE :text")
    fun getConversationWithText(text: String): List<ConversationSmsModel>
}
