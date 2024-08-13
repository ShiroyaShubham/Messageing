package messenger.messages.messaging.sms.chat.meet.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import messenger.messages.messaging.sms.chat.meet.send_message.Settings
import messenger.messages.messaging.sms.chat.meet.send_message.Transaction
import messenger.messages.messaging.sms.chat.meet.extensions.notificationManager
import messenger.messages.messaging.sms.chat.meet.extensions.showErrorToast
import messenger.messages.messaging.sms.chat.meet.send_message.Message
import messenger.messages.messaging.sms.chat.meet.utils.*

class ReplySMSService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mAddress = intent.getStringExtra(THREAD_NUMBER)
        val mThreadID = intent.getLongExtra(THREAD_ID, 0L)
        var mMessage = RemoteInput.getResultsFromIntent(intent)!!.getCharSequence(REPLY)?.toString() ?: return

        Log.d("TAG_MESSAGE", "onReceive: $mMessage")
        mMessage = context.removeDiacriticsIfNeeded(mMessage)

        val settings =
            Settings()
        settings.useSystemSending = true
        settings.deliveryReports = true

        val transaction =
            Transaction(
                context,
                settings
            )
        val message =
            Message(
                mMessage,
                mAddress
            )

        try {
            val smsSentIntent = Intent(context, SMS_Service_Sended_Status_Receiver::class.java)
            val deliveredIntent = Intent(context, SendedSMSStatusService::class.java)

            transaction.setExplicitBroadcastForSentSms(smsSentIntent)
            transaction.setExplicitBroadcastForDeliveredSms(deliveredIntent)

            transaction.sendNewMessage(message, mThreadID)
        } catch (e: Exception) {
            context.showErrorToast(e)
        }

        context.notificationManager.cancel(mThreadID.hashCode())

        ensureBackgroundThread {
            val messages = context.getMessages(mThreadID)
            context.messagesDB.insertAllInMSGTransaction(messages)
            context.markThreadMessagesReadNew(mThreadID)
            context.conversationsDB.markReadAsMessage(mThreadID)
        }
    }
}
