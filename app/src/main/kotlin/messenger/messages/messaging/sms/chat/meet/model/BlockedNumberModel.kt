package messenger.messages.messaging.sms.chat.meet.model

data class BlockedNumberModel(val id: Long, val number: String, val normalizedNumber: String, val numberToCompare: String)
