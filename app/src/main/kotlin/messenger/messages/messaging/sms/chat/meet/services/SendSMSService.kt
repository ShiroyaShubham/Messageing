package messenger.messages.messaging.sms.chat.meet.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import messenger.messages.messaging.sms.chat.meet.send_message.Settings
import messenger.messages.messaging.sms.chat.meet.send_message.Transaction
import messenger.messages.messaging.sms.chat.meet.utils.getThreadId
import messenger.messages.messaging.sms.chat.meet.send_message.Message

class SendSMSService : Service() {
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent == null) {
                return START_NOT_STICKY
            }

            val mNumber = Uri.decode(intent.dataString!!.removePrefix("sms:").removePrefix("smsto:").removePrefix("mms").removePrefix("mmsto:").trim())
            val mText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val mSettings =
                Settings()
            mSettings.useSystemSending = true
            mSettings.deliveryReports = true

            val mTransaction =
                Transaction(
                    this,
                    mSettings
                )
            val mMessage =
                Message(
                    mText,
                    mNumber
                )

            val smsSentIntent = Intent(this, SMS_Service_Sended_Status_Receiver::class.java)
            val deliveredIntent = Intent(this, SendedSMSStatusService::class.java)

            mTransaction.setExplicitBroadcastForSentSms(smsSentIntent)
            mTransaction.setExplicitBroadcastForDeliveredSms(deliveredIntent)

            mTransaction.sendNewMessage(mMessage, getThreadId(mNumber))
        } catch (ignored: Exception) {
        }

        return super.onStartCommand(intent, flags, startId)
    }
}