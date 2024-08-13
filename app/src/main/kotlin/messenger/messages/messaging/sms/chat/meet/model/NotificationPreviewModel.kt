package messenger.messages.messaging.sms.chat.meet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class NotificationPreviewModel(
    val name: String,
    @PrimaryKey(autoGenerate = false)
    val number: String,
    val previewType: String,
    val isWakeup: Int
)
