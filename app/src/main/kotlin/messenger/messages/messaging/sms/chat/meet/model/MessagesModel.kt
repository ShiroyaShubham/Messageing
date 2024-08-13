package messenger.messages.messaging.sms.chat.meet.model

import android.provider.Telephony
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessagesModel(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "participants") val participants: ArrayList<ContactsModel>,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "is_mms") val isMMS: Boolean,
    @ColumnInfo(name = "attachment") val attachment: AttachmentSMSModel?,
    @ColumnInfo(name = "sender_name") var senderName: String,
    @ColumnInfo(name = "sender_photo_uri") val senderPhotoUri: String,
    @ColumnInfo(name = "subscription_id") var subscriptionId: Int,
) : ItemModel() {

    fun isReceivedMessage() = type == Telephony.Sms.MESSAGE_TYPE_INBOX
}
