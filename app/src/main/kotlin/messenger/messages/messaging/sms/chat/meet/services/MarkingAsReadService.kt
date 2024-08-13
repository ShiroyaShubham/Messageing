package messenger.messages.messaging.sms.chat.meet.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.extensions.copyToClipboard
import messenger.messages.messaging.sms.chat.meet.extensions.notificationManager
import messenger.messages.messaging.sms.chat.meet.model.MarkAsReadModel
import messenger.messages.messaging.sms.chat.meet.utils.conversationsDB
import messenger.messages.messaging.sms.chat.meet.utils.markThreadMessagesReadNew
import messenger.messages.messaging.sms.chat.meet.utils.updateUnreadCountBadge
import messenger.messages.messaging.sms.chat.meet.utils.*
import org.greenrobot.eventbus.EventBus

class MarkingAsReadService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MARK_AS_READ -> {
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.markThreadMessagesReadNew(threadId)
                    context.conversationsDB.markReadAsMessage(threadId)
                    EventBus.getDefault().post(MarkAsReadModel(threadId, true))
                    context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversationsMessage())
                    Log.e("TAG_REFRESH", " SMS_Service_Marking_As_Read MARK_AS_READ")
                    MainAppClass.getAllMessagesFromDb {
                        refreshMessages()
                    }
                }
                return
            }

            COPY_OTP -> {
                val otpCopy = intent.getStringExtra(OTP)
                Log.e("COPY_OTP: ", "" + otpCopy)
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {

                    if (otpCopy != null) {
                        context.copyToClipboard(otpCopy)
                    }
                    /*context.markThreadMessagesReadNew(threadId)
                    context.conversationsDB.markReadAsMessage(threadId)
                    context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversationsMessage())
                    refreshMessages()*/
                }
                return
            }
        }
    }
}
