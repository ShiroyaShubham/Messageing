package messenger.messages.messaging.sms.chat.meet.services

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.extensions.isNumberBlocked
import messenger.messages.messaging.sms.chat.meet.extensions.isNumberBlockedNew
import messenger.messages.messaging.sms.chat.meet.model.ContactsModel
import messenger.messages.messaging.sms.chat.meet.model.InsertNewMsgModel
import messenger.messages.messaging.sms.chat.meet.model.MessagesModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import org.greenrobot.eventbus.EventBus

class ReceiverService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        var address = ""
        var body = ""
        var subject = ""
        var date = 0L
        var threadId = 0L
        var status = Telephony.Sms.STATUS_NONE
        val type = Telephony.Sms.MESSAGE_TYPE_INBOX
        val read = 0
        val subscriptionId = intent.getIntExtra("subscription", -1)

        ensureBackgroundThread {
            messages.forEach {
                address = it.originatingAddress ?: ""
                subject = it.pseudoSubject
                status = it.status
                body += it.messageBody
                date = Math.min(it.timestampMillis, System.currentTimeMillis())
                threadId = context.getThreadId(address)
            }

            Handler(Looper.getMainLooper()).post {
                ensureBackgroundThread {
                    Log.e("TAG", "onReceive:>>>>>>>>>>> "+type)
                    if (!context.isNumberBlockedNew(address)) {
                        val newMessageId = context.insertNewSMS(address, subject, body, date, read, threadId, type, subscriptionId)

                        val conversation = context.getConversations1(threadId).firstOrNull() ?: return@ensureBackgroundThread
                        try {
                            context.conversationsDB.insertOrUpdateMessage(conversation)
                        } catch (ignored: Exception) {
                        }

                        try {
                            context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversationsMessage())
                        } catch (ignored: Exception) {
                        }

                        val mParticipant = ContactsModel(0, 0, address, "", arrayListOf(address), ArrayList(), ArrayList())
                        val mParticipants = arrayListOf(mParticipant)
//                        val mMessageDate = (date / 1000).toInt()
                        val mMessage =
                            MessagesModel(newMessageId, body, type, status, mParticipants, date, false, threadId, false, null, address, "", subscriptionId)
                        context.messagesDB.insertOrUpdateMessage(mMessage)
                        Log.e("TAG_REFRESH", " SMS_Service_Receiver BroadcastReceiver")
                        MainAppClass.getAllMessagesFromDb {
                            refreshMessages()
                        }
                        EventBus.getDefault().post(InsertNewMsgModel(threadId, read = false, snipet = body, dateTime = date, address))
                        context?.let {
                            val topActivity = getTopActivity(it)
                            Log.e("MyReceiver", "Top Activity: $topActivity")
                            if (topActivity != null && topActivity!!.contains("MsgActivity") && CURRENTLY_OPEN_THREADID == threadId) {

                            } else {
                                context.showReceivedMessageNotification(address, body, threadId, null)
                            }
                        }
                    }

                }
            }
        }
    }

    private fun getTopActivity(context: Context): String? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val appTasks = activityManager.appTasks
            if (appTasks.isNotEmpty()) {
                appTasks[0].taskInfo.topActivity?.className
            } else null
        } else {
            val runningTasks = activityManager.getRunningTasks(1)
            if (runningTasks.isNotEmpty()) {
                runningTasks[0].topActivity?.className
            } else null
        }
    }
}
