package messenger.messages.messaging.sms.chat.meet.listners

import androidx.room.Dao
import androidx.room.Query
import messenger.messages.messaging.sms.chat.meet.model.AttachmentModel

@Dao
interface AttachRoomDao {
    @Query("SELECT * FROM attachments")
    fun getAllList(): List<AttachmentModel>
}
