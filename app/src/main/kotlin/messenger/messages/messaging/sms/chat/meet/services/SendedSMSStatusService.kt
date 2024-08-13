package messenger.messages.messaging.sms.chat.meet.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.MainScope
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.send_message.DeliveredReceiver
import messenger.messages.messaging.sms.chat.meet.utils.ensureBackgroundThread
import messenger.messages.messaging.sms.chat.meet.utils.messagesDB
import messenger.messages.messaging.sms.chat.meet.utils.updateMessageStatusNew
import messenger.messages.messaging.sms.chat.meet.utils.refreshMessages

class SendedSMSStatusService : DeliveredReceiver() {

    override fun onMessageStatusUpdated(context: Context, intent: Intent, receiverResultCode: Int) {
        if (intent.extras?.containsKey("message_uri") == true) {
            val uri = Uri.parse(intent.getStringExtra("message_uri"))
            val mMessageID = uri?.lastPathSegment?.toLong() ?: 0L
            ensureBackgroundThread {
                val status = Telephony.Sms.STATUS_COMPLETE
                context.updateMessageStatusNew(mMessageID, status)
                val updated = context.messagesDB.updateStatusMessages(mMessageID, status)
                if (updated == 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        ensureBackgroundThread {
                            context.messagesDB.updateStatusMessages(mMessageID, status)
                        }
                    }, 2000)
                }
                Log.e("TAG_REFRESH", " SMS_Service_Sended_Status DeliveredReceiver")
                MainAppClass.getAllMessagesFromDb {
                    refreshMessages()
                }
            }
        }
    }
}
