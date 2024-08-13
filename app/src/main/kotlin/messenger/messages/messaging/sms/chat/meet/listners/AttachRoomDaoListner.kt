package messenger.messages.messaging.sms.chat.meet.listners

import androidx.room.Dao
import androidx.room.Query
import messenger.messages.messaging.sms.chat.meet.model.AttachmentSMSModel

@Dao
interface AttachRoomDaoListner {
    @Query("SELECT * FROM message_attachments")
    fun getAllList(): List<AttachmentSMSModel>
}
