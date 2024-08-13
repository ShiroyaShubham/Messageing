package messenger.messages.messaging.sms.chat.meet.listners

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import messenger.messages.messaging.sms.chat.meet.model.ArchivedModel
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.model.NotificationPreviewModel

@Dao
interface ArchivedMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertArchivedUser(archivedModel: ArchivedModel)

    @Delete
    fun deleteArchivedUser(archivedModel: ArchivedModel)

    @Query("DELETE FROM archivedmodel WHERE threadId = :threadId")
    fun deleteArchivedUser(threadId: Long )

    @Query("SELECT * FROM archivedmodel")
    fun getArchivedUser(): List<ArchivedModel>

}
