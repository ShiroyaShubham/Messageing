package messenger.messages.messaging.sms.chat.meet.model

data class InsertNewChatModel(
    var threadID: Long,
    var read: Boolean,
    var snipet: String,
    var title: String,
    var number: String,
    var dateTime: Long
)
