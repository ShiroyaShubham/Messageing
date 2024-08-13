package messenger.messages.messaging.sms.chat.meet.model

data class Contact (
    var _id:Long = 0,
    var lookupKey: String = "",
    var name:String = "",
    var photoUri:String? = "",
    var address:String = "",
)
