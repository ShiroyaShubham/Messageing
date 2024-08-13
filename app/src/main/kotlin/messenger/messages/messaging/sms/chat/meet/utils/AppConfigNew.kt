package messenger.messages.messaging.sms.chat.meet.utils

import android.content.Context
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import java.util.HashSet

class AppConfigNew(context: Context) : messenger.messages.messaging.sms.chat.meet.utils.BasePref(context) {
    companion object {
        fun newInstance(context: Context) = messenger.messages.messaging.sms.chat.meet.utils.AppConfigNew(context)
    }

    fun saveUseSIMIdAtNumber(number: String, SIMId: Int) {
        prefs.edit().putInt(messenger.messages.messaging.sms.chat.meet.utils.USE_SIM_ID_PREFIX + number, SIMId).apply()
    }

    fun getUseSIMIdAtNumber(number: String) = prefs.getInt(messenger.messages.messaging.sms.chat.meet.utils.USE_SIM_ID_PREFIX + number, 0)

    var showCharacterCounter: Boolean
        get() = prefs.getBoolean(messenger.messages.messaging.sms.chat.meet.utils.SHOW_CHARACTER_COUNTER, false)
        set(showCharacterCounter) = prefs.edit().putBoolean(messenger.messages.messaging.sms.chat.meet.utils.SHOW_CHARACTER_COUNTER, showCharacterCounter).apply()

    var useSimpleCharacters: Boolean
        get() = prefs.getBoolean(messenger.messages.messaging.sms.chat.meet.utils.USE_SIMPLE_CHARACTERS, false)
        set(useSimpleCharacters) = prefs.edit().putBoolean(messenger.messages.messaging.sms.chat.meet.utils.USE_SIMPLE_CHARACTERS, useSimpleCharacters).apply()

    var enableDeliveryReports: Boolean
        get() = prefs.getBoolean(messenger.messages.messaging.sms.chat.meet.utils.ENABLE_DELIVERY_REPORTS, true)
        set(enableDeliveryReports) = prefs.edit().putBoolean(messenger.messages.messaging.sms.chat.meet.utils.ENABLE_DELIVERY_REPORTS, enableDeliveryReports).apply()

    var lockScreenVisibilitySetting: Int
        get() = prefs.getInt(
            messenger.messages.messaging.sms.chat.meet.utils.LOCK_SCREEN_VISIBILITY,
            messenger.messages.messaging.sms.chat.meet.utils.LOCK_SCREEN_SENDER_MESSAGE
        )
        set(lockScreenVisibilitySetting) = prefs.edit().putInt(messenger.messages.messaging.sms.chat.meet.utils.LOCK_SCREEN_VISIBILITY, lockScreenVisibilitySetting).apply()

    var mmsFileSizeLimit: Long
        get() = prefs.getLong(
            messenger.messages.messaging.sms.chat.meet.utils.MMS_FILE_SIZE_LIMIT,
            messenger.messages.messaging.sms.chat.meet.utils.FILE_SIZE_1_MB
        )
        set(mmsFileSizeLimit) = prefs.edit().putLong(messenger.messages.messaging.sms.chat.meet.utils.MMS_FILE_SIZE_LIMIT, mmsFileSizeLimit).apply()

    var pinnedConversations: Set<String>
        get() = prefs.getStringSet(messenger.messages.messaging.sms.chat.meet.utils.PINNED_CONVERSATIONS, HashSet<String>())!!
        set(pinnedConversations) = prefs.edit().putStringSet(messenger.messages.messaging.sms.chat.meet.utils.PINNED_CONVERSATIONS, pinnedConversations).apply()

    fun addPinnedConversations(conversations: List<ConversationSmsModel>) {
        pinnedConversations = pinnedConversations.plus(conversations.map { it.threadId.toString() })
    }

    fun removePinnedConversations(conversations: List<ConversationSmsModel>) {
        pinnedConversations = pinnedConversations.minus(conversations.map { it.threadId.toString() })
    }

    var lastExportDate: String
        get() = prefs.getString(messenger.messages.messaging.sms.chat.meet.utils.LAST_EXPORT_DATE, "")!!
        set(lastExportPath) = prefs.edit().putString(messenger.messages.messaging.sms.chat.meet.utils.LAST_EXPORT_DATE, lastExportPath).apply()

    var exportSms: Boolean
        get() = prefs.getBoolean(messenger.messages.messaging.sms.chat.meet.utils.EXPORT_SMS, true)
        set(exportSms) = prefs.edit().putBoolean(messenger.messages.messaging.sms.chat.meet.utils.EXPORT_SMS, exportSms).apply()

//    var exportMms: Boolean
//        get() = prefs.getBoolean(EXPORT_MMS, true)
//        set(exportMms) = prefs.edit().putBoolean(EXPORT_MMS, exportMms).apply()

    var importSms: Boolean
        get() = prefs.getBoolean(messenger.messages.messaging.sms.chat.meet.utils.IMPORT_SMS, true)
        set(importSms) = prefs.edit().putBoolean(messenger.messages.messaging.sms.chat.meet.utils.IMPORT_SMS, importSms).apply()

//    var importMms: Boolean
//        get() = prefs.getBoolean(IMPORT_MMS, true)
//        set(importMms) = prefs.edit().putBoolean(IMPORT_MMS, importMms).apply()
}
