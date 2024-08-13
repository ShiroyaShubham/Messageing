package messenger.messages.messaging.sms.chat.meet.model

import android.net.Uri

data class SelectedAttachmentModel(
    val uri: Uri,
    val isPending: Boolean,
)
