package messenger.messages.messaging.sms.chat.meet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ArchivedModel(
    @PrimaryKey(autoGenerate = false)
    val number: String,
    val threadId: Long,
    val name: String
)
