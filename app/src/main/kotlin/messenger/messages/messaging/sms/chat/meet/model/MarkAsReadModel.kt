package messenger.messages.messaging.sms.chat.meet.model

data class MarkAsReadModel(
    var threadID: Long,
    var isRead: Boolean
)
