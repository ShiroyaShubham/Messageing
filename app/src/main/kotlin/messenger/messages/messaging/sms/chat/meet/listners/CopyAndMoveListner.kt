package messenger.messages.messaging.sms.chat.meet.listners

interface CopyAndMoveListner {
    fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean, destinationPath: String, wasCopyingOneFileOnly: Boolean)

    fun copyFailed()
}
