package messenger.messages.messaging.sms.chat.meet.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.provider.ContactsContract.PhoneLookup
import android.provider.OpenableColumns
import android.provider.Telephony
import android.provider.Telephony.*
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.gson.Gson
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.MessagesActivity
import messenger.messages.messaging.sms.chat.meet.services.MarkingAsReadService
import messenger.messages.messaging.sms.chat.meet.services.ReplySMSService
import messenger.messages.messaging.sms.chat.meet.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leolin.shortcutbadger.ShortcutBadger
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.listners.*
import org.greenrobot.eventbus.EventBus
import java.io.FileNotFoundException
import java.util.regex.Pattern


val Context.config: AppConfigNew
    get() = AppConfigNew.newInstance(
        applicationContext
    )

fun Context.getMessagessDB() = DatabaseMessages.getInstance(this)

val Context.conversationsDB: ConversationsRoomDaoListner get() = getMessagessDB().ConversationsDao()

val Context.messagesDB: DaoListner get() = getMessagessDB().MessagesDao()
val Context.notificationPreViewDao: NotificationPreviewDao get() = getMessagessDB().NotificationPreviewDao()
val Context.archivedMessageDao: ArchivedMessageDao get() = getMessagessDB().ArchivedMessageDao()
val Context.blockContactDao: BlockNoDao get() = getMessagessDB().BlockNoDao()

fun Context.getMessages(threadId: Long): ArrayList<MessagesModel> {
    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms._ID,
        Sms.BODY,
        Sms.TYPE,
        Sms.ADDRESS,
        Sms.DATE,
        Sms.READ,
        Sms.THREAD_ID,
        Sms.SUBSCRIPTION_ID,
        Sms.STATUS
    )

    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    val sortOrder = "${Sms._ID} DESC LIMIT 100"

    val blockStatus = HashMap<String, Boolean>()
    val blockedNumbers = getBlockedNumbers()
    var messages = ArrayList<MessagesModel>()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, showErrors = true) { cursor ->
        val senderNumber = cursor.getStringValue(Sms.ADDRESS) ?: return@queryCursor

        val isNumberBlocked = if (blockStatus.containsKey(senderNumber)) {
            blockStatus[senderNumber]!!
        } else {
            val isBlocked = isNumberBlockedNew(senderNumber)
            blockStatus[senderNumber] = isBlocked
            isBlocked
        }

        if (isNumberBlocked) {
            return@queryCursor
        }

        val id = cursor.getLongValue(Sms._ID)
        val body = cursor.getStringValue(Sms.BODY)
        val type = cursor.getIntValue(Sms.TYPE)
        val namePhoto = getNameAndPhotoFromPhoneNumber(senderNumber)
        val senderName = namePhoto.name
        val photoUri = namePhoto.photoUri ?: ""
        val date = cursor.getLongValue(Sms.DATE)
        val read = cursor.getIntValue(Sms.READ) == 1
        val thread = cursor.getLongValue(Sms.THREAD_ID)
        val subscriptionId = cursor.getIntValue(Sms.SUBSCRIPTION_ID)
        val status = cursor.getIntValue(Sms.STATUS)
        val participant = ContactsModel(0, 0, senderName, photoUri, arrayListOf(senderNumber), ArrayList(), ArrayList())
        val isMMS = false
        val message = MessagesModel(id, body, type, status, arrayListOf(participant), date, read, thread, isMMS, null, senderName, photoUri, subscriptionId)
        messages.add(message)
    }

    messages.addAll(getMMS(threadId, sortOrder))
    messages = messages.filter { it.participants.isNotEmpty() }
        .sortedWith(compareBy<MessagesModel> { it.date }.thenBy { it.id }).toMutableList() as ArrayList<MessagesModel>

    return messages
}

// as soon as a message contains multiple recipients it counts as an MMS instead of SMS
fun Context.getMMS(threadId: Long? = null, sortOrder: String? = null): ArrayList<MessagesModel> {
    val uri = Mms.CONTENT_URI
    val projection = arrayOf(
        Mms._ID,
        Mms.DATE,
        Mms.READ,
        Mms.MESSAGE_BOX,
        Mms.THREAD_ID,
        Mms.SUBSCRIPTION_ID,
        Mms.STATUS
    )

    val selection = if (threadId == null) {
        "1 == 1) GROUP BY (${Mms.THREAD_ID}"
    } else {
        "${Mms.THREAD_ID} = ?"
    }

    val selectionArgs = if (threadId == null) {
        null
    } else {
        arrayOf(threadId.toString())
    }

    val messages = ArrayList<MessagesModel>()
    val contactsMap = HashMap<Int, ContactsModel>()
    val threadParticipants = HashMap<Long, ArrayList<ContactsModel>>()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, showErrors = true) { cursor ->
        val mmsId = cursor.getLongValue(Mms._ID)
        val type = cursor.getIntValue(Mms.MESSAGE_BOX)
        val date = cursor.getLongValue(Mms.DATE)
        val read = cursor.getIntValue(Mms.READ) == 1
        val threadId = cursor.getLongValue(Mms.THREAD_ID)
        val subscriptionId = cursor.getIntValue(Mms.SUBSCRIPTION_ID)
        val status = cursor.getIntValue(Mms.STATUS)
        val participants = if (threadParticipants.containsKey(threadId)) {
            threadParticipants[threadId]!!
        } else {
            val parts = getThreadParticipants(threadId, contactsMap)
            threadParticipants[threadId] = parts
            parts
        }

        val isMMS = true
        val attachment = getMmsAttachment(mmsId)
        val body = attachment.text
        var senderName = ""
        var senderPhotoUri = ""

        if (type != Mms.MESSAGE_BOX_SENT && type != Mms.MESSAGE_BOX_FAILED) {
            val number = getMMSSender(mmsId)
            val namePhoto = getNameAndPhotoFromPhoneNumber(number)
            senderName = namePhoto.name
            senderPhotoUri = namePhoto.photoUri ?: ""
        }

        val message =
            MessagesModel(mmsId, body, type, status, participants, date, read, threadId, isMMS, attachment, senderName, senderPhotoUri, subscriptionId)
        messages.add(message)

        participants.forEach {
            contactsMap[it.rawId] = it
        }
    }

    return messages
}

fun Context.getMMSSender(msgId: Long): String {
    val uri = Uri.parse("${Mms.CONTENT_URI}/$msgId/addr")
    val projection = arrayOf(
        Mms.Addr.ADDRESS
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Mms.Addr.ADDRESS)
            }
        }
    } catch (ignored: Exception) {
    }
    return ""
}

fun Context.getConversations(
    threadId: Long? = null,
    privateContacts: ArrayList<ContactsModel> = ArrayList(),
    onUpdateView: (ArrayList<ConversationSmsModel>) -> Unit = {}
): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )
    val LAST_DATE: Long = getSharedPrefs().getString(LAST_SYNC_DATE, "0")!!.toLong()
    var selection = "${Threads.MESSAGE_COUNT} > ?" + " AND ${Threads.DATE} > " + LAST_DATE
    var selectionArgs = arrayOf("0"/*, "WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE)*//*, java.lang.String.valueOf(LAST_DATE)*/)
    if (threadId != null) {
        selection += " AND ${Threads._ID} = ?" + " AND ${Threads.DATE} > " + LAST_DATE
        selectionArgs = arrayOf("0", threadId.toString()/*,"WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE*//*, java.lang.String.valueOf(LAST_DATE)*/)
    }

//    selectionArgs = arrayOf<String>(java.lang.String.valueOf(LAST_DATE))
//    val pageNumber = ApplicationClass.PAGE_NO ?: 0 // Initial page number is 0
    val sortOrder = "${Threads.DATE} DESC LIMIT ${PAGE_SIZE} OFFSET ${PAGE_NO * PAGE_SIZE}"

    /*val limitOffset = "${ApplicationClass.PAGE_NO * ApplicationClass.PAGE_SIZE}, ${ApplicationClass.PAGE_SIZE}"
    val sortOrder = "${Threads.DATE} DESC LIMIT $limitOffset"*/

    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
    val blockedNumbers = getBlockedNumbers()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->

        if (cursor.count > 0) {

            val id = cursor.getLongValue(Threads._ID)
            var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
            if (snippet.isEmpty()) {
                snippet = getThreadSnippet(id)
            }

            val date = cursor.getLongValue(Threads.DATE)
//            if (date.toString().length > 10) {
//                date /= 1000
//            }

            val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
            val recipientIds = rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()
            val phoneNumbers = getThreadPhoneNumbers(recipientIds)
            if (phoneNumbers.any { isNumberBlocked(it, blockedNumbers) }) {
                return@queryCursor
            }

            val names = getThreadContactNames(phoneNumbers, privateContacts)
            Log.d("TAG_NAME", "getConversations: $names")
            val title = TextUtils.join(", ", names.toTypedArray())
            val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""
            val isGroupConversation = phoneNumbers.size > 1
            val read = cursor.getIntValue(Threads.READ) == 1
            Log.d("TAG_LAST_CONVERSATION", "getConversations: $snippet")
            val conversation = ConversationSmsModel(id, snippet, date, read, title, photoUri, isGroupConversation, phoneNumbers.first())
            conversations.add(conversation)
        } else {
            IS_LOOP_WORKING = false
        }
    }

    conversations.sortByDescending { it.date }
    return conversations
}

fun Context.getConversationIds(): List<Long> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(Threads._ID)
    val selection = "${Threads.MESSAGE_COUNT} > ?"
    val selectionArgs = arrayOf("0")
    val sortOrder = "${Threads.DATE} ASC"
    val conversationIds = mutableListOf<Long>()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->
        val id = cursor.getLongValue(Threads._ID)
        conversationIds.add(id)
    }
    return conversationIds
}


@SuppressLint("NewApi")
fun Context.getMmsAttachment(id: Long): AttachmentSMSModel {
    val uri = if (isQPlus()) {
        Mms.Part.CONTENT_URI
    } else {
        Uri.parse("content://mms/part")
    }

    val projection = arrayOf(
        Mms._ID,
        Mms.Part.CONTENT_TYPE,
        Mms.Part.TEXT
    )
    val selection = "${Mms.Part.MSG_ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    val messageAttachment = AttachmentSMSModel(id, "", arrayListOf())

    var attachmentName = ""
    queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
        val partId = cursor.getLongValue(Mms._ID)
        val mimetype = cursor.getStringValue(Mms.Part.CONTENT_TYPE)
        if (mimetype == "text/plain") {
            messageAttachment.text = cursor.getStringValue(Mms.Part.TEXT) ?: ""
        } else if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
            val attachment = AttachmentModel(partId, id, Uri.withAppendedPath(uri, partId.toString()).toString(), mimetype, 0, 0, "")
            messageAttachment.attachments.add(attachment)
        } else if (mimetype != "application/smil") {
            val attachment = AttachmentModel(partId, id, Uri.withAppendedPath(uri, partId.toString()).toString(), mimetype, 0, 0, attachmentName)
            messageAttachment.attachments.add(attachment)
        } else {
            val text = cursor.getStringValue(Mms.Part.TEXT)
            val cutName = text.substringAfter("ref src=\"").substringBefore("\"")
            if (cutName.isNotEmpty()) {
                attachmentName = cutName
            }
        }
    }

    return messageAttachment
}

fun Context.getLatestMMS(): MessagesModel? {
    val sortOrder = "${Mms.DATE} DESC LIMIT 1"
    return getMMS(sortOrder = sortOrder).firstOrNull()
}

fun Context.getThreadSnippet(threadId: Long): String {
    val sortOrder = "${Mms.DATE} DESC LIMIT 1"
    val latestMms = getMMS(threadId, sortOrder).firstOrNull()
    var snippet = latestMms?.body ?: ""

    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms.BODY
    )

    val selection = "${Sms.THREAD_ID} = ? AND ${Sms.DATE} > ?"
    val selectionArgs = arrayOf(
        threadId.toString(),
        latestMms?.date?.toString() ?: "0"
    )
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                snippet = cursor.getStringValue(Sms.BODY)
            }
        }
    } catch (ignored: Exception) {
    }
    return snippet
}

fun Context.getMessageRecipientAddress(messageId: Long): String {
    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms.ADDRESS
    )

    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(messageId.toString())

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Sms.ADDRESS)
            }
        }
    } catch (e: Exception) {
    }

    return ""
}

fun Context.getThreadParticipants(threadId: Long, contactsMap: HashMap<Int, ContactsModel>?): ArrayList<ContactsModel> {
    val uri = Uri.parse("${MmsSms.CONTENT_CONVERSATIONS_URI}?simple=true")
    val projection = arrayOf(
        ThreadsColumns.RECIPIENT_IDS
    )
    val selection = "${Mms._ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    val participants = ArrayList<ContactsModel>()
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val address = cursor.getStringValue(ThreadsColumns.RECIPIENT_IDS)
                address.split(" ").filter { it.areDigitsOnly() }.forEach {
                    val addressId = it.toInt()
                    if (contactsMap?.containsKey(addressId) == true) {
                        participants.add(contactsMap[addressId]!!)
                        return@forEach
                    }

                    val phoneNumber = getPhoneNumberFromAddressId(addressId)
                    val namePhoto = getNameAndPhotoFromPhoneNumber(phoneNumber)
                    val name = namePhoto.name
                    val photoUri = namePhoto.photoUri ?: ""
                    val contact = ContactsModel(addressId, addressId, name, photoUri, arrayListOf(phoneNumber), ArrayList(), ArrayList())
                    participants.add(contact)
                }
            }
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
    return participants
}

fun Context.getThreadPhoneNumbers(recipientIds: List<Int>): ArrayList<String> {
    val numbers = ArrayList<String>()
    recipientIds.forEach {
        numbers.add(getPhoneNumberFromAddressId(it))
    }
    return numbers
}

fun Context.getThreadContactNames(phoneNumbers: List<String>, privateContacts: ArrayList<ContactsModel>): ArrayList<String> {
    val names = ArrayList<String>()
    phoneNumbers.forEach { number ->
        val name = SimpleContactsHelperUtils(this).getNameFromPhoneNumber(number)
        if (name != number) {
            names.add(name)
        } else {
            val privateContact = privateContacts.firstOrNull { it.doesHavePhoneNumber(number) }
            if (privateContact == null) {
                names.add(name)
            } else {
                names.add(privateContact.name)
            }
        }
    }
    return names
}

fun Context.getPhoneNumberFromAddressId(canonicalAddressId: Int): String {
    val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")
    val projection = arrayOf(
        Mms.Addr.ADDRESS
    )

    val selection = "${Mms._ID} = ?"
    val selectionArgs = arrayOf(canonicalAddressId.toString())
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Mms.Addr.ADDRESS)
            }
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
    return ""
}

fun Context.getSuggestedContacts(privateContacts: ArrayList<ContactsModel>): ArrayList<ContactsModel> {
    val contacts = ArrayList<ContactsModel>()
    val uri = Sms.CONTENT_URI
    val projection = arrayOf(
        Sms.ADDRESS
    )

    val selection = "1 == 1) GROUP BY (${Sms.ADDRESS}"
    val selectionArgs = null
    val sortOrder = "${Sms.DATE} DESC LIMIT 20"
    val blockedNumbers = getBlockedNumbers()

    queryCursor(uri, projection, selection, selectionArgs, sortOrder, showErrors = true) { cursor ->
        val senderNumber = cursor.getStringValue(Sms.ADDRESS) ?: return@queryCursor
        val namePhoto = getNameAndPhotoFromPhoneNumber(senderNumber)
        var senderName = namePhoto.name
        var photoUri = namePhoto.photoUri ?: ""
        if (isNumberBlocked(senderNumber, blockedNumbers)) {
            return@queryCursor
        } else if (namePhoto.name == senderNumber) {
            if (privateContacts.isNotEmpty()) {
                val privateContact = privateContacts.firstOrNull { it.phoneNumbers.first() == senderNumber }
                if (privateContact != null) {
                    senderName = privateContact.name
                    photoUri = privateContact.photoUri
                } else {
                    return@queryCursor
                }
            } else {
                return@queryCursor
            }
        }

        val contact = ContactsModel(0, 0, senderName, photoUri, arrayListOf(senderNumber), ArrayList(), ArrayList())
        if (!contacts.map { it.phoneNumbers.first().trimToComparableNumber() }.contains(senderNumber.trimToComparableNumber())) {
            contacts.add(contact)
        }
    }

    return contacts
}

fun Context.getNameAndPhotoFromPhoneNumber(number: String): UserPhotoModel {
    if (!hasPermission(PERMISSION_READ_CONTACTS)) {
        return UserPhotoModel(number, null)
    }

    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    val projection = arrayOf(
        PhoneLookup.DISPLAY_NAME,
        PhoneLookup.PHOTO_URI
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor.use {
            if (cursor?.moveToFirst() == true) {
                val name = cursor.getStringValue(PhoneLookup.DISPLAY_NAME)
                val photoUri = cursor.getStringValue(PhoneLookup.PHOTO_URI)
                return UserPhotoModel(name, photoUri)
            }
        }
    } catch (e: Exception) {
    }

    return UserPhotoModel(number, null)
}

fun Context.insertNewSMS(address: String, subject: String, body: String, date: Long, read: Int, threadId: Long, type: Int, subscriptionId: Int): Long {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.ADDRESS, address)
        put(Sms.SUBJECT, subject)
        put(Sms.BODY, body)
        put(Sms.DATE, date)
        put(Sms.READ, read)
        put(Sms.THREAD_ID, threadId)
        put(Sms.TYPE, type)
        put(Sms.SUBSCRIPTION_ID, subscriptionId)
    }

    return try {
        val newUri = contentResolver.insert(uri, contentValues)
        newUri?.lastPathSegment?.toLong() ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun Context.getConversationsBlockedNumber(
    threadId: Long? = null,
    privateContacts: ArrayList<ContactsModel> = ArrayList(),
    contactNum: ArrayList<BlockedNumberModel>
): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )

    var selection = "${Threads.MESSAGE_COUNT} > ?"/* + Telephony.Sms.ADDRESS + " = ?"*/
    var selectionArgs = arrayOf("0"/*, contactNum[0].normalizedNumber*/)
    val sortOrder = "${Threads.DATE} DESC"

    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
//    val blockedNumbers = getBlockedNumbers()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->
        val id = cursor.getLongValue(Threads._ID)
        var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
        if (snippet.isEmpty()) {
            snippet = getThreadSnippet(id)
        }

        var date = cursor.getLongValue(Threads.DATE)

        val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
        val recipientIds = rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()
        val phoneNumbers = getThreadPhoneNumbers(recipientIds)
        Log.e("TAG", "getConversationsBlockedNumber: ${rawIds}")
        val i = contactNum.filter { it.normalizedNumber.contains(phoneNumbers.first()) }

        if (i.size == 0) {
            return@queryCursor
        }

        val names = getThreadContactNames(phoneNumbers, privateContacts)
        val title = TextUtils.join(", ", names.toTypedArray())
        val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""
        val isGroupConversation = phoneNumbers.size > 1
        val read = cursor.getIntValue(Threads.READ) == 1
        val conversation = ConversationSmsModel(id, snippet, date.toLong(), read, title, photoUri, isGroupConversation, phoneNumbers.first())
        conversations.add(conversation)
    }

    conversations.sortByDescending { it.date }
    return conversations
}

fun Context.getBlockedNumbers1(): java.util.ArrayList<BlockContactModel> {
    return blockContactDao.getAllBlockNo().toMutableList() as java.util.ArrayList<BlockContactModel>
}

fun Context.deleteConversation(threadId: Long) {
    var uri = Sms.CONTENT_URI
    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    try {
        contentResolver.delete(uri, selection, selectionArgs)
    } catch (e: Exception) {
        showErrorToast(e)
    }

    uri = Mms.CONTENT_URI
    contentResolver.delete(uri, selection, selectionArgs)

    conversationsDB.deleteThreadIdMessage(threadId)
    messagesDB.deleteThreadMessagesApp(threadId)
}


fun Context.deleteMessage(id: Long, isMMS: Boolean) {
    val uri = if (isMMS) Mms.CONTENT_URI else Sms.CONTENT_URI
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    try {
        contentResolver.delete(uri, selection, selectionArgs)
        messagesDB.delete(id)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.markMessageRead(id: Long, isMMS: Boolean) {
    val uri = if (isMMS) Mms.CONTENT_URI else Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.READ, 1)
        put(Sms.SEEN, 1)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
    messagesDB.markReadAsMessage(id)
}

fun Context.markThreadMessagesReadNew(threadId: Long) {
    arrayOf(Sms.CONTENT_URI, Mms.CONTENT_URI).forEach { uri ->
        val contentValues = ContentValues().apply {
            put(Sms.READ, 1)
            put(Sms.SEEN, 1)
        }
        val selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        contentResolver.update(uri, contentValues, selection, selectionArgs)
    }
    messagesDB.markThreadRead(threadId)
}

fun Context.markThreadMessagesUnreadNew(threadId: Long) {
    arrayOf(Sms.CONTENT_URI, Mms.CONTENT_URI).forEach { uri ->
        val contentValues = ContentValues().apply {
            put(Sms.READ, 0)
            put(Sms.SEEN, 0)
        }
        val selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        contentResolver.update(uri, contentValues, selection, selectionArgs)
    }
}

fun Context.updateMessageTypeApp(id: Long, type: Int) {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.TYPE, type)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
}

fun Context.updateMessageStatusNew(id: Long, status: Int) {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.STATUS, status)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
}

fun Context.updateMessageSubscriptionId(messageId: Long, subscriptionId: Int) {
    val uri = Sms.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.SUBSCRIPTION_ID, subscriptionId)
    }
    val selection = "${Sms._ID} = ?"
    val selectionArgs = arrayOf(messageId.toString())
    contentResolver.update(uri, contentValues, selection, selectionArgs)
}

fun Context.updateUnreadCountBadge(conversations: List<ConversationSmsModel>) {
    val unreadCount = conversations.count { !it.read }
    if (unreadCount == 0) {
        ShortcutBadger.removeCount(this)
    } else {
        ShortcutBadger.applyCount(this, unreadCount)
    }
}

@SuppressLint("NewApi")
fun Context.getThreadId(address: String): Long {
    return if (isMarshmallowPlus()) {
        try {
            Threads.getOrCreateThreadId(this, address)
        } catch (e: Exception) {
            0L
        }
    } else {
        0L
    }
}

@SuppressLint("NewApi")
fun Context.getThreadId(addresses: Set<String>): Long {
    return if (isMarshmallowPlus()) {
        try {
            Threads.getOrCreateThreadId(this, addresses)
        } catch (e: Exception) {
            0L
        }
    } else {
        0L
    }
}

fun Context.showReceivedMessageNotification(address: String, body: String, threadId: Long, bitmap: Bitmap?) {
    val privateCursor = getMyContactsCursor(false, true)?.loadInBackground()
    ensureBackgroundThread {
        val senderName = getNameFromAddress(address, privateCursor)

        Handler(Looper.getMainLooper()).post {
            showMessageNotification(address, body, threadId, bitmap, senderName)
        }
    }
}

fun Context.getNameFromAddress(address: String, privateCursor: Cursor?): String {
    var sender = getNameAndPhotoFromPhoneNumber(address).name
    if (address == sender) {
        val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(this, privateCursor)
        sender = privateContacts.firstOrNull { it.doesHavePhoneNumber(address) }?.name ?: address
    }
    return sender
}

@SuppressLint("NewApi", "UnspecifiedImmutableFlag")
fun Context.showMessageNotification(address: String, body: String, threadId: Long, bitmap: Bitmap?, sender: String) {
    CoroutineScope(Dispatchers.Main).launch {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (isOreoPlus()) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .build()

            val name = getString(R.string.channel_received_sms)
            val importance = NotificationManager.IMPORTANCE_HIGH
            NotificationChannel(NOTIFICATION_CHANNEL, name, importance).apply {
                setBypassDnd(false)
                enableLights(false)
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }
        }

        val intent = Intent(this@showMessageNotification, MessagesActivity::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(isNotification, true)
            putExtra(THREAD_TITLE, sender)
            putExtra(THREAD_NUMBER, getPhoneNumberFromThreadId(contentResolver, threadId))
        }

        val pendingIntent = PendingIntent.getActivity(
            this@showMessageNotification,
            threadId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summaryText = getString(R.string.new_message)
        val markAsReadIntent = Intent(this@showMessageNotification, MarkingAsReadService::class.java).apply {
            action = MARK_AS_READ
            putExtra(THREAD_ID, threadId)
        }
        var Otp = ""

        MainAppClass.setupSIMSelector()

        val markAsReadPendingIntent =
            PendingIntent.getBroadcast(this@showMessageNotification, 0, markAsReadIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

        var replyAction: NotificationCompat.Action? = null

        if (isNougatPlus()) {
            val replyLabel = getString(R.string.reply)
            val remoteInput = RemoteInput.Builder(REPLY)
                .setLabel(replyLabel)
                .build()

            val replyIntent = Intent(this@showMessageNotification, ReplySMSService::class.java).apply {
                putExtra(THREAD_ID, threadId)
                putExtra(THREAD_NUMBER, address)
            }

            val replyPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                threadId.hashCode(),
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            replyAction = NotificationCompat.Action.Builder(R.drawable.ic_send_vector, replyLabel, replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build()
        }
        var previewType = getString(R.string.app_show_name_and_message)
        var isWakeUpScreen = false
        withContext(Dispatchers.IO) {
            notificationPreViewDao.getUserNotificationPreview().forEach {
                if (it.name == sender) {
                    previewType = it.previewType
                    isWakeUpScreen = it.isWakeup == 1
                }
            }
        }
        val largeIcon = bitmap ?: SimpleContactsHelperUtils(this@showMessageNotification).getContactLetterIcon(sender)
        val builder = NotificationCompat.Builder(this@showMessageNotification, NOTIFICATION_CHANNEL).apply {
            when (config.lockScreenVisibilitySetting) {
                LOCK_SCREEN_SENDER_MESSAGE -> {
                    if (previewType != getString(R.string.app_hide_contents)) {
                        setContentTitle(sender)
                        setLargeIcon(largeIcon)
                    }
                    if (previewType == getString(R.string.app_show_name_and_message)) setContentText(body)
                }

                LOCK_SCREEN_SENDER -> {
                    if (previewType != getString(R.string.app_hide_contents)) {
                        setContentTitle(sender)
                        setLargeIcon(largeIcon)
                    }
                }
            }

            color = getAdjustedPrimaryColor()
//            setSmallIcon(R.drawable.ic_notification_icon_new)
            setSmallIcon(R.drawable.message_icon)
            if (previewType == getString(R.string.app_show_name_and_message)) setStyle(NotificationCompat.BigTextStyle().setSummaryText(summaryText).bigText(body))
            setContentIntent(pendingIntent)
            priority = NotificationCompat.PRIORITY_MAX
            setDefaults(Notification.DEFAULT_LIGHTS)
            setCategory(Notification.CATEGORY_MESSAGE)
            setAutoCancel(true)
            setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
        }

        if (replyAction != null && config.lockScreenVisibilitySetting == LOCK_SCREEN_SENDER_MESSAGE) {
            builder.addAction(replyAction)
        }

        builder.addAction(R.drawable.icon_check, getString(R.string.mark_as_read), markAsReadPendingIntent)
            .setChannelId(NOTIFICATION_CHANNEL)


        if (body.isNotEmpty() &&
            (body.lowercase().contains("otp") || body.lowercase().contains("code")
                || body.lowercase().contains("pin"))
        ) {
            val pattern_4 = Pattern.compile("(\\d{4})")
            val matcher_4 = pattern_4.matcher(body)

            val pattern_5 = Pattern.compile("(\\d{5})")
            val matcher_5 = pattern_5.matcher(body)

            val pattern_6 = Pattern.compile("(\\d{6})")
            val matcher_6 = pattern_6.matcher(body)


            if (matcher_6.find() && matcher_6.group(0).length == 6) {
                Otp = matcher_6.group(0)
            } else if (matcher_5.find() && matcher_5.group(0).length == 5) {
                Otp = matcher_5.group(0)
            } else if (matcher_4.find() && matcher_4.group(0).length == 4) {
                Otp = matcher_4.group(0)
            }
            if (Otp.isNotEmpty()) {
                val copyOTPIntent = Intent(this@showMessageNotification, MarkingAsReadService::class.java).apply {
                    action = COPY_OTP
                    putExtra(OTP, Otp)
                    putExtra(THREAD_ID, threadId)
                }
                val copyOTPPendingIntent =
                    PendingIntent.getBroadcast(this@showMessageNotification, 0, copyOTPIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)
                builder.addAction(R.drawable.logo_copy_black, getString(R.string.txt_copy) + " " + Otp, copyOTPPendingIntent)
                    .setChannelId(NOTIFICATION_CHANNEL)
            }

        }

        CoroutineScope(Dispatchers.IO).launch {
            val archivedConversation = archivedMessageDao.getArchivedUser()
            var isArchivedMsg = false
            archivedConversation.forEach {
                if (it.number == getPhoneNumberFromThreadId(contentResolver, threadId))
                    isArchivedMsg = true
            }
            isArchivedMsg = archivedConversation.any {
                it.number == getPhoneNumberFromThreadId(contentResolver, threadId)
            }
            withContext(Dispatchers.Main) {
                if (!isArchivedMsg) {
                    notificationManager.notify(threadId.hashCode(), builder.build())
                    if (isWakeUpScreen) wakeUpScreen(this@showMessageNotification)
                }
            }
        }
    }
}

@SuppressLint("Range")
fun getPhoneNumberFromThreadId(contentResolver: ContentResolver, threadId: Long): String? {
    val uri = Uri.parse("content://mms-sms/conversations/$threadId")
    val projection = arrayOf(Telephony.TextBasedSmsColumns.THREAD_ID, Telephony.TextBasedSmsColumns.ADDRESS)

    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val phoneNumber = cursor.getString(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS))
            return phoneNumber
        }
    }
    return null
}


private fun wakeUpScreen(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        if (!powerManager.isInteractive) {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp:WakeUpScreen"
            )
            wakeLock.acquire(5 * 60 * 1000L /*5 minutes*/)
        }
    } else {
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MyApp:WakeUpScreen"
        )
        wakeLock.acquire(5 * 60 * 1000L /*5 minutes*/)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        if (keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(context as Activity, null)
        }
    }
}

fun Context.removeDiacriticsIfNeeded(text: String): String {
    return if (config.useSimpleCharacters) text.normalizeString() else text
}

fun Context.getSmsDraft(threadId: Long): String? {
    val uri = Sms.Draft.CONTENT_URI
    val projection = arrayOf(Sms.BODY)
    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor.use {
            if (cursor?.moveToFirst() == true) {
                return cursor.getString(0)
            }
        }
    } catch (e: Exception) {
    }

    return null
}

fun Context.getAllDrafts(): HashMap<Long, String?> {
    val drafts = HashMap<Long, String?>()
    val uri = Sms.Draft.CONTENT_URI
    val projection = arrayOf(Sms.BODY, Sms.THREAD_ID)

    try {
        queryCursor(uri, projection) { cursor ->
            cursor.use {
                val threadId = cursor.getLongValue(Sms.THREAD_ID)
                val draft = cursor.getStringValue(Sms.BODY) ?: return@queryCursor
                drafts[threadId] = draft
            }
        }
    } catch (e: Exception) {
    }

    return drafts
}

fun Context.saveSmsDraft(body: String, threadId: Long) {
    val uri = Sms.Draft.CONTENT_URI
    val contentValues = ContentValues().apply {
        put(Sms.BODY, body)
        put(Sms.DATE, System.currentTimeMillis().toString())
        put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT)
        put(Sms.THREAD_ID, threadId)
    }

    try {
        contentResolver.insert(uri, contentValues)
    } catch (e: Exception) {
    }
}

fun Context.deleteSmsDraft(threadId: Long) {
    val uri = Sms.Draft.CONTENT_URI
    val projection = arrayOf(Sms._ID)
    val selection = "${Sms.THREAD_ID} = ?"
    val selectionArgs = arrayOf(threadId.toString())
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor.use {
            if (cursor?.moveToFirst() == true) {
                val draftId = cursor.getLong(0)
                val draftUri = Uri.withAppendedPath(Sms.CONTENT_URI, "/${draftId}")
                contentResolver.delete(draftUri, null, null)
            }
        }
    } catch (e: Exception) {
    }
}

fun Context.updateLastConversationMessage(messageModel: MessagesModel?) {
    CoroutineScope(Dispatchers.IO).launch {
        messageModel?.let {
            val uri = Threads.CONTENT_URI
            val selection = "${Threads._ID} = ?"
            val selectionArgs = arrayOf(it.threadId.toString())
            try {
                contentResolver.delete(uri, selection, selectionArgs)
                val newConversation = getConversationByThreadId(it.threadId)
                newConversation?.phoneNumber = getPhoneNumberFromThreadId(contentResolver, it.threadId).toString()
                newConversation?.title = it.senderName
                newConversation?.photoUri = it.senderPhotoUri
                Log.d("TAG_LAST_CONVERSATION", "updateLastConversationMessage: ${Gson().toJson(newConversation)}")
                newConversation?.let {
                    conversationsDB.insertOrUpdateMessage(newConversation)
                    MainAppClass.getConversationDataFromDB()
                    EventBus.getDefault().post(LoadConversationsModel())
                }
            } catch (e: Exception) {
                Log.d("TAG_ERROR", "updateLastConversationMessage: ${e.message}")
            }
        }
    }
}

fun Context.getContactNameFromPhoneNumber(phoneNumber: String): String {
    val uri: Uri = Uri.withAppendedPath(
        PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber)
    )
    val projection: Array<String> = arrayOf(PhoneLookup.DISPLAY_NAME)
    val cursor: Cursor? = contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )
    var contactName = ""
    cursor?.use {
        if (it.moveToFirst()) {
            contactName = it.getString(it.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME))
        }
    }
    return contactName
}

fun Context.getFileSizeFromUri(uri: Uri): Long {
    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(uri, "r")
    } catch (e: FileNotFoundException) {
        null
    }

    // uses ParcelFileDescriptor#getStatSize underneath if failed
    val length = assetFileDescriptor?.use { it.length } ?: FILE_SIZE_NONE
    if (length != -1L) {
        return length
    }

    // if "content://" uri scheme, try contentResolver table
    if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            // maybe shouldn't trust ContentResolver for size:
            // https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex == -1) {
                return@use FILE_SIZE_NONE
            }
            cursor.moveToFirst()
            return try {
                cursor.getLong(sizeIndex)
            } catch (_: Throwable) {
                FILE_SIZE_NONE
            }
        } ?: FILE_SIZE_NONE
    } else {
        return FILE_SIZE_NONE
    }
}

fun Context.dialNumber(phoneNumber: String, callback: (() -> Unit)? = null) {
    Intent(Intent.ACTION_DIAL).apply {
        data = Uri.fromParts("tel", phoneNumber, null)

        try {
            startActivity(this)
            callback?.invoke()
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app_found)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

fun Context.getAddressId(): ArrayList<RecipientModel> {
    val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")
    val projection = arrayOf(
        Mms.Addr.ADDRESS
    )

    // val selection = "${Mms._ID} = ?"
    // val selectionArgs = arrayOf(canonicalAddressId.toString())
    val items = ArrayList<RecipientModel>()
    try {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    items.add(RecipientModel(cursor.getStringValue(Mms._ID).toLong(), cursor.getStringValue(Mms.Addr.ADDRESS)))
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()
    } catch (e: Exception) {
        showErrorToast(e)
    }
    return items
}

fun Context.getConversationss(): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )
    val LAST_DATE: Long = getSharedPrefs().getString(LAST_SYNC_DATE, "0")!!.toLong()
    var selection = "${Threads.MESSAGE_COUNT} > ?" + " AND ${Threads.DATE} > " + LAST_DATE
    var selectionArgs = arrayOf("0"/*, "WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE)*//*, java.lang.String.valueOf(LAST_DATE)*/)

    val sortOrder = " ${Threads.DATE} DESC"

    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->

        if (cursor.count > 0) {

            try {
                val id = cursor.getLongValue(Threads._ID)
                var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
                if (snippet.isEmpty()) {
                    //  snippet = getThreadSnippet(id)
                }
                var date = cursor.getLongValue(Threads.DATE)
                val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
                if (rawIds.isNullOrEmpty()) {
                    return@queryCursor
                }
                val title = ""//TextUtils.join(", ", names.toTypedArray())
                val photoUri = /*if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else*/ ""
                val isGroupConversation = false//phoneNumbers.size > 1
                val read = cursor.getIntValue(Threads.READ) == 1
                val conversation = ConversationSmsModel(id, snippet, date, read, title, photoUri, isGroupConversation, "", rawIds)
                conversations.add(conversation)
                //insertManualyAndGetChat(conversation)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TAG", "getConversations: errorororor ${e}")
            }

            EventBus.getDefault().post(ProgressCountModel(conversations.size, cursor.count))
        } else {
            IS_LOOP_WORKING = false
        }
    }

    //conversations.sortByDescending { it.date }
    return conversations
}

@SuppressLint("Range")
fun Context.getConversationByThreadId(threadId: Long): ConversationSmsModel? {

    val uri: Uri = Uri.parse("content://sms/")
    val projection = arrayOf(
        Sms.THREAD_ID,
        Sms.BODY,
        Sms.DATE,
        Sms.ADDRESS,
        Sms.READ
    )

    val cursor: Cursor? = contentResolver.query(
        uri,
        projection,
        "${Sms.THREAD_ID}=?",
        arrayOf(threadId.toString()),
        null
    )

    cursor?.let {
        if (it.moveToFirst()) {
            val snippet = it.getString(it.getColumnIndex(Sms.BODY))
            val date = it.getLong(it.getColumnIndex(Sms.DATE))
            val recipientIds = it.getString(it.getColumnIndex(Sms.ADDRESS))
            val read = it.getInt(it.getColumnIndex(Sms.READ)) == 1

            println("TAG_SMS Snippet: $snippet")
            println("TAG_SMS Date: $date")
            println("TAG_SMS Recipient IDs: $recipientIds")
            println("TAG_SMS Read: $read")
            return ConversationSmsModel(threadId, snippet, date, read, "", "", false, "", recipientIds)
        }
        it.close()
    }
    return null
}


fun Context.getConversationsNewTry(threadId: Long? = null, privateContacts: ArrayList<ContactsModel> = ArrayList()): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )
    val LAST_DATE: Long = getSharedPrefs().getString(LAST_SYNC_DATE, "0")!!.toLong()
    var selection = "${Threads.MESSAGE_COUNT} > ?" + " AND ${Threads.DATE} > " + LAST_DATE
    var selectionArgs = arrayOf("0"/*, "WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE)*//*, java.lang.String.valueOf(LAST_DATE)*/)
    if (threadId != null) {
        selection += " AND ${Threads._ID} = ?" + " AND ${Threads.DATE} > " + LAST_DATE
        selectionArgs = arrayOf("0", threadId.toString()/*,"WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE*//*, java.lang.String.valueOf(LAST_DATE)*/)
    }

    val sortOrder = " ${Threads.DATE} DESC"// limit ${ApplicationClass.PAGE_SIZE} OFFSET ${ApplicationClass.PAGE_SIZE * ApplicationClass.PAGE_NO}"
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    var sortOrder1 = Bundle().apply {
        //putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${Threads.DATE} DESC")
        putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
        // putInt(ContentResolver.QUERY_ARG_OFFSET, ApplicationClass.PAGE_NO * ApplicationClass.PAGE_SIZE)
    }
//    }

//    ContentResolver.QUERY_ARG_LIMIT
    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
    //val blockedNumbers = getBlockedNumbers1()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->

        if (cursor.count > 0) {

            try {
                val id = cursor.getLongValue(Threads._ID)
                var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
                if (snippet.isEmpty()) {
                    snippet = getThreadSnippet(id)
                }

                var date = cursor.getLongValue(Threads.DATE)

                val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
                Log.e("Naimish", "Recipientids______$rawIds")
                val recipientIds = rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()
                val phoneNumbers = getThreadPhoneNumbers(recipientIds)
                /* if (phoneNumbers.any { isNumberBlocked2(id, blockedNumbers) }) {
                     return@queryCursor
                 }*/

                val names = getThreadContactNames(phoneNumbers, privateContacts)
                val title = TextUtils.join(", ", names.toTypedArray())
                val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""
                val isGroupConversation = phoneNumbers.size > 1
                val read = cursor.getIntValue(Threads.READ) == 1
                val conversation = ConversationSmsModel(id, snippet, date, read, title, photoUri, isGroupConversation, phoneNumbers.first())
                conversations.add(conversation)
                insertManualyAndGetChat(conversation)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TAG", "getConversations: errorororor ${e}")
            }

            EventBus.getDefault().post(ProgressCountModel(conversations.size, cursor.count))
        } else {
            IS_LOOP_WORKING = false
        }
    }

    //conversations.sortByDescending { it.date }
    return conversations
}

private fun Context.insertManualyAndGetChat(newConvo: ConversationSmsModel) {
    CoroutineScope(Dispatchers.IO).launch {
        conversationsDB.insertOrUpdateMessage(newConvo)

        withContext(Dispatchers.IO) {
            setupThread(newConvo.threadId)
        }
    }
}

fun Context.getSnippetFromThreadId(threadId: Long): String {

    val uri = Uri.parse("content://sms/")
    val projection = arrayOf("body")
    val selection = "thread_id = ?"
    val selectionArgs = arrayOf(threadId.toString())
    val sortOrder = "date DESC LIMIT 1"

    val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
        if (it.moveToFirst()) {
            val snippet = it.getString(it.getColumnIndexOrThrow("body"))
            Log.d("SMS Snippet", "Snippet: $snippet")
            return snippet;
        }
    }
    return ""
}


fun Context.setupThread(threadId: Long) {
//        val privateCursor = mActivity!!.getMyContactsCursor(false, true)?.loadInBackground()
    CoroutineScope(Dispatchers.IO).launch {
//            val cachedMessagesCode = messages.clone().hashCode()

        val messages = getMessages(threadId)
        Log.d("TAG_MESSAGES_SIZE", "setupThread: ${messages.size}")
        messagesDB.insertAllInMSGTransaction(messages)
        Log.d("TAG_REFRESH", "setupThread: ")

    }
}

fun Context.getConversationsImportNew(): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )

    var selection = "${Threads.MESSAGE_COUNT} > ?" //+ " AND ${Threads.DATE} > " + LAST_DATE
    var selectionArgs = arrayOf("0"/*, "WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE)*//*, java.lang.String.valueOf(LAST_DATE)*/)

    val sortOrder = " ${Threads.DATE} DESC"

    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->

        if (cursor.count > 0) {

            try {
                val id = cursor.getLongValue(Threads._ID)
                var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
                if (snippet.isEmpty()) {
                    //  snippet = getThreadSnippet(id)
                }

                var date = cursor.getLongValue(Threads.DATE)

                val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)

                if (rawIds.isNullOrEmpty()) {
                    return@queryCursor
                }
                // val names = getThreadContactNames(phoneNumbers, privateContacts)
                val title = ""//TextUtils.join(", ", names.toTypedArray())
                val photoUri = /*if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else*/ ""
                val isGroupConversation = false//phoneNumbers.size > 1
                val read = cursor.getIntValue(Threads.READ) == 1
                val conversation = ConversationSmsModel(id, snippet, date, read, title, photoUri, isGroupConversation, "", rawIds)
                conversations.add(conversation)
                //insertManualyAndGetChat(conversation)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TAG", "getConversations: errorororor ${e}")
            }

            EventBus.getDefault().post(ProgressCountModel(conversations.size, cursor.count))
        } else {
            IS_LOOP_WORKING = false
        }
    }

    //conversations.sortByDescending { it.date }
    return conversations
}

fun Context.getConversations1(threadId: Long? = null, privateContacts: ArrayList<ContactsModel> = ArrayList()): ArrayList<ConversationSmsModel> {
    val uri = Uri.parse("${Threads.CONTENT_URI}?simple=true")
    val projection = arrayOf(
        Threads._ID,
        Threads.SNIPPET,
        Threads.DATE,
        Threads.READ,
        Threads.RECIPIENT_IDS
    )
    var selection = "${Threads.MESSAGE_COUNT} > ?"
    var selectionArgs = arrayOf("0"/*, "WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE)*//*, java.lang.String.valueOf(LAST_DATE)*/)
    if (threadId != null) {
        selection += " AND ${Threads._ID} = ?"
        selectionArgs = arrayOf("0", threadId.toString()/*,"WHERE " + Telephony.Sms.DATE + "> " + LAST_DATE*//*, java.lang.String.valueOf(LAST_DATE)*/)
    }

//    selectionArgs = arrayOf<String>(java.lang.String.valueOf(LAST_DATE))

    val sortOrder = "${Threads.DATE} DESC"

    val conversations = ArrayList<ConversationSmsModel>()
    val simpleContactHelper = SimpleContactsHelperUtils(this)
    val blockedNumbers = getBlockedNumbers1()
    queryCursor(uri, projection, selection, selectionArgs, sortOrder, true) { cursor ->
        val id = cursor.getLongValue(Threads._ID)
        var snippet = cursor.getStringValue(Threads.SNIPPET) ?: ""
        if (snippet.isEmpty()) {
            snippet = getThreadSnippet(id)
        }

        var date = cursor.getLongValue(Threads.DATE)
        /*if (date.toString().length > 10) {
            date /= 1000
        }*/

        val rawIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
        val recipientIds = rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()
        val phoneNumbers = getThreadPhoneNumbers(recipientIds)
        if (phoneNumbers.any { isNumberBlocked2(id, blockedNumbers) }) {
            return@queryCursor
        }

        val names = getThreadContactNames(phoneNumbers, privateContacts)
        val title = TextUtils.join(", ", names.toTypedArray())
        val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""
        val isGroupConversation = phoneNumbers.size > 1
        val read = cursor.getIntValue(Threads.READ) == 1
        val conversation = ConversationSmsModel(id, snippet, date, read, title, photoUri, isGroupConversation, phoneNumbers.first())
        conversations.add(conversation)
    }

    conversations.sortByDescending { it.date }
    return conversations
}
