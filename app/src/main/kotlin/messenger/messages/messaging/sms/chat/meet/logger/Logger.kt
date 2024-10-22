package messenger.messages.messaging.sms.chat.meet.logger

interface Logger {

    fun v(classInstance: Class<out Any?>?, msg: String?)
    fun i(classInstance: Class<out Any?>?, msg: String?)
    fun d(classInstance: Class<out Any?>?, msg: String?)
    fun w(classInstance: Class<out Any?>?, msg: String?)
    fun e(classInstance: Class<out Any?>?, msg: String?)

}
