package messenger.messages.messaging.sms.chat.meet.model

data class SendedSmsModel(val messageID: Long, val delivered: Boolean) : ItemModel()
