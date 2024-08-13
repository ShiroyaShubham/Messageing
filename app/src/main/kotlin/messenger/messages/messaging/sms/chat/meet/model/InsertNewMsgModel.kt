package messenger.messages.messaging.sms.chat.meet.model

data class InsertNewMsgModel(
    var threadID: Long,
    var read: Boolean,
    var snipet: String,
    var dateTime: Long,
    var number: String,
    /*,
    var date: Int,
    var read: Boolean,
    var title: String,
    var photoUri: String,
    var isGroupConversation: Boolean,
    var isSelected: Boolean = false,
    var phoneNumber: String*/
)
