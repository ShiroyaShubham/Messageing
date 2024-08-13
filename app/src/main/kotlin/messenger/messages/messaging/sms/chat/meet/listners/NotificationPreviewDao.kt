package messenger.messages.messaging.sms.chat.meet.listners

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.model.NotificationPreviewModel

@Dao
interface NotificationPreviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserPreview(previewModel: NotificationPreviewModel)


    @Query("SELECT * FROM NotificationPreviewModel")
    fun getUserNotificationPreview(): List<NotificationPreviewModel>

}
