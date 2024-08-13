package messenger.messages.messaging.sms.chat.meet.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.gson.Gson
import kotlinx.coroutines.*
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.activity.MessagesActivity
import messenger.messages.messaging.sms.chat.meet.extensions.beGone
import messenger.messages.messaging.sms.chat.meet.extensions.beVisible
import messenger.messages.messaging.sms.chat.meet.extensions.getAdjustedPrimaryColor
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.activity.HomeActivity
import messenger.messages.messaging.sms.chat.meet.adapters.ChatHistoryAdapter1
import messenger.messages.messaging.sms.chat.meet.databinding.FragmentSmsBinding
import messenger.messages.messaging.sms.chat.meet.extensions.getSharedPrefs
import messenger.messages.messaging.sms.chat.meet.model.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PersonalFragment : BaseFragment() {
    private lateinit var binding: FragmentSmsBinding
    private var mAdapter: ChatHistoryAdapter1? = null
    private var sortedConversations: ArrayList<ConversationSmsModel> = ArrayList()
    fun newInstance(): PersonalFragment {
        return PersonalFragment()
    }

    fun newInstance2() {
        if (isAdded && !mActivity!!.isFinishing) {
            onOptionTypeNew()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSmsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = ChatHistoryAdapter1(mActivity!! as BaseHomeActivity)
        mAdapter!!.itemClickListenerSelect = { position, it ->
            if (!mAdapter!!.getIsShowSelection()) {
                showInterstitialAdPerDayOnMessageClick {
                    val intent = Intent(mActivity!!, MessagesActivity::class.java)
                    intent.putExtra(THREAD_ID, it.threadId)
                    intent.putExtra(THREAD_TITLE, it.title)
                    intent.putExtra(THREAD_NUMBER, it.phoneNumber)
                    intent.putExtra(SNIPPET, it.snippet)
                    intent.putExtra(DATE, it.date)
//                    intent.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                    startActivity(intent)
                }
            }
        }
        val layoutManager = CustomLayoutManager(requireContext(), LinearLayout.VERTICAL, false)
        binding.recyclerViewChatHistory.layoutManager = layoutManager
        binding.recyclerViewChatHistory.adapter = mAdapter
        ensureBackgroundThread {
            onOptionTypeNew()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && !mActivity!!.isFinishing) {
            if (mAdapter != null) {
                mAdapter!!.updateFontSize()
            }

            val adjustedPrimaryColor = requireContext().getAdjustedPrimaryColor()
            binding.conversationsFastscroller.updateColors(adjustedPrimaryColor)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("TAG_REFRESH: ", "Personal_Message RefreshMessages $event")
//        onOptionTypeNew()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConveration(event: LoadConversationsModel) {
        Log.e("TAG_REFRESH: ", "refreshConveration REFRESH Personal MSG")
        onOptionTypeNew()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshWhileBackMessages(event: RefreshWhileBackModel.RefreshMessages) {
        Log.e("TAG_PERSONAL: ", "refreshWhileBackMessages REFRESH MSG")
        onOptionTypeNew()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackupImport(event: BackupImportModel) {
        Log.e("TAG_PERSONAL: ", " onBackupImport REFRESH MSG")
        onOptionTypeNew()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMarkAsReadOrUnread(event: MarkAsReadModel) {
        Log.e("TAG_PERSONAL: ", "onMarkAsReadOrUnread MARK AS READ ${event.isRead} ${sortedConversations.size}")
        if (event.isRead) {
            sortedConversations.withIndex().filter {
                sortedConversations[it.index].threadId == event.threadID
            }.map {
                sortedConversations[it.index].read = event.isRead
                mAdapter?.notifyItemChanged(it.index)

                Log.e("TAG_PERSONAL", "onMarkAsReadOrUnread: ${it.index}")
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnarchiveMSGS(unarchive: ArchivedModel) {
        Log.e("TAG_PERSONAL: ", "onUnarchiveMSGS ")
        onOptionTypeNew()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInsertMSG(event: InsertNewMsgModel) {
        Log.e("TAG_PERSONAL: ", "onInsertMSG  onInsertMSG  ${event.threadID}")
        if (event.threadID != 0L) {

            var isPersonalNo = false
            for (contact in MainAppClass.mAllContacts) {
                if (contact.name.lowercase() == mActivity!!.getContactNameFromPhoneNumber(event.number).lowercase()) {
                    isPersonalNo = true
                }
            }

            if (!isPersonalNo) {
                return
            }

            if (sortedConversations.withIndex().none { sortedConversations[it.index].threadId == event.threadID }) {
                Log.d("TAG_MESSAGE", "onInsertMSG: $event")
                CoroutineScope(Dispatchers.IO).launch {
                    val conversation = ConversationSmsModel(event.threadID, event.snipet, event.dateTime, event.read, event.number, "", false, event.number, "")
                    sortedConversations.add(conversation)
                    val localAll =
                        sortedConversations.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                            it.date
                        }).toMutableList() as ArrayList<ConversationSmsModel>
                    withContext(Dispatchers.Main) {
                        if (sortedConversations.size == 1) {
                            noDataView()
                        } else {
                            visibleDataView()
                            mAdapter!!.setData(localAll)
                        }
                    }
                }
                return
            }

            sortedConversations.withIndex().filter { sortedConversations[it.index].threadId == event.threadID }.map {
                sortedConversations[it.index].read = event.read
                sortedConversations[it.index].snippet = event.snipet
                sortedConversations[it.index].date = event.dateTime
                mAdapter?.notifyItemChanged(it.index)

                CoroutineScope(Dispatchers.IO).launch {
                    if (sortedConversations[0].date == 1.toLong()) {
                        sortedConversations.removeAt(0)
                    }

                    val localAll =
                        sortedConversations.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                            it.date
                        }).toMutableList() as ArrayList<ConversationSmsModel>

                    CoroutineScope(Dispatchers.Main).launch {
                        if (isInternetAvailable(requireActivity())) {
                            if (sortedConversations[0].date == 1.toLong()) {

                            } else {
                                localAll.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                            }
                        }
                        sortedConversations.clear()
                        sortedConversations.addAll(localAll)
                        Log.d("TAG_PERSONAL", "onInsertMSG: ")
                        mAdapter?.setData(sortedConversations)
                    }

                }
            }


        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnBlockMessages(numberList: UnBlockMsgListModel) {
        Log.e("TAG_PERSONAL: ", "onUnBlockMessages   ${numberList.msgList}")

//        val privateCursor = mActivity!!.getMyContactsCursor(false, true)?.loadInBackground()

        CoroutineScope(Dispatchers.IO).launch {
            val conversations: ArrayList<ConversationSmsModel> = arrayListOf()
            numberList.msgList.forEachIndexed { index, blockContactModel ->
                val conversation = ConversationSmsModel(
                    blockContactModel.threadID, blockContactModel.msg, blockContactModel.dateTime, true, blockContactModel.name, "", false,

                    blockContactModel.number
                )
                for (contact in MainAppClass.mAllContacts) {
                    if (contact.name.lowercase() == blockContactModel.name.lowercase()) {
                        conversations.add(conversation)
                    }
                }
            }

            Log.d("TAG_PERSONAL", "onUnBlockMessages: ${Gson().toJson(conversations)}")

            sortedConversations.addAll(conversations)

            if (sortedConversations[0].date == 1.toLong()) {
                sortedConversations.removeAt(0)
            }

//            mActivity!!.conversationsDB.insertAllInConversationTransaction(conversations)
//            MainAppClass.getAllMessagesFromDb {
//                refreshMessages()
//            }

            val localAll =
                sortedConversations.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                    it.date
                }).toMutableList() as ArrayList<ConversationSmsModel>
            CoroutineScope(Dispatchers.Main).launch {

                if (isInternetAvailable(requireActivity())) {
                    if (sortedConversations.any { it.date == 1.toLong() }) {

                    } else {
                        localAll.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                    }
                }
                Log.d("TAG_PERSONAL", "onUnBlockMessages: 1 ${sortedConversations.size}")
                sortedConversations.clear()
                sortedConversations.addAll(localAll)
                Log.d("TAG_PERSONAL", "onUnBlockMessages: 2 ${sortedConversations.size}")
                if (sortedConversations.size == 1) {
                    noDataView()
                } else {
                    visibleDataView()
                    mAdapter!!.setData(localAll)
                }
//                getCachedConversationsOrignal()
                binding.recyclerViewChatHistory.smoothScrollToPosition(0)
            }
        }

    }

    //    var searchKey = ""
    private var bus: EventBus? = null
//    private fun onOptionType() {
//        bus = EventBus.getDefault()
//        try {
//            bus!!.register(this)
//        } catch (e: Exception) {
//            Log.d("TAG_EXCEPTION", "onOptionType: ${e.message}")
//        }
//
//        try {
//            if (isAdded && !mActivity!!.isFinishing) {
//                CoroutineScope(Dispatchers.IO).launch {
//
//                    Log.d("TAG_CONTACT", "onOptionType: ${MainAppClass.personalMessageDBDATA.size}")
//                    val messagesLocal = MainAppClass.personalMessageDBDATA.ifEmpty {
//                        val messagesAll = mActivity!!.messagesDB.getAllList()
//                        Log.e("TAG_SIZE_PERSONAL", "messagesAll size: " + messagesAll.size)
//
//                        val searchKeyCredit = "Credit"
//                        val searchQueryCredit = "%$searchKeyCredit%"
//                        val messagesCredit = mActivity!!.messagesDB.getMessagesWithText(searchQueryCredit)
//
//                        val searchKeyDebit = "Debit"
//                        val searchQueryDebit = "%$searchKeyDebit%"
//                        val messagesDebit = mActivity!!.messagesDB.getMessagesWithText(searchQueryDebit)
//
//                        val searchKeyOTP = "OTP"
//                        val searchQueryOTP = "%$searchKeyOTP%"
//                        val messagesOTP = mActivity!!.messagesDB.getMessagesWithText(searchQueryOTP)
//
//                        val searchKeyLink = "Link"
//                        val searchQueryLink = "%$searchKeyLink%"
//                        val messagesLink = mActivity!!.messagesDB.getMessagesWithText(searchQueryLink)
//
//
//                        messagesAll - messagesCredit.toSet() - messagesDebit.toSet() - messagesOTP.toSet() - messagesLink.toSet()
//
//                    }
//
//                    Log.d("TAG_PERSONAL", "onOptionType: 1 ")
//
//                    val mAllContacts: ArrayList<ContactsModel>
//                    if (MainAppClass.mAllContacts.isNotEmpty()) {
//                        mAllContacts = MainAppClass.mAllContacts
//                        compareAllMessagesWithContacts(mAllContacts, messagesLocal)
//                    } else {
//                        SimpleContactsHelperUtils(mActivity!!).getAvailableContacts(false) {
//                            Log.d("TAG_CONTACT_SIZE", "onOptionType: inital ${it.size}")
//                            compareAllMessagesWithContacts(it, messagesLocal)
//                        }
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            Log.d("TAG_ERROR", "onOptionType: ${e.message}")
//        }
//    }

//    private fun compareAllMessagesWithContacts(mAllContacts: ArrayList<ContactsModel>, messagesLocal: List<MessagesModel>) {
//        val messages = ArrayList<MessagesModel>()
//        val mContactsNumber = ArrayList<String>()
//        Log.d("TAG_ALL_CONTACT", "onOptionType: ${mAllContacts.size}")
//
//        try {
//
//
//            for (item in mAllContacts) {
//                for (phoneNumber in item.phoneNumbers) {
//                    mContactsNumber.add(phoneNumber.replace("[", "").replace("]", ""))
//                }
////                        mContactsNumber.add(item.phoneNumbers.toString().replace("[", "").replace("]", ""))
//            }
//
//            Log.d("TAG_PERSONAL", "onOptionType: 2 ")
//            for (messageItem in messagesLocal) {
//                for (participants in messageItem.participants) {
//                    for (number in participants.phoneNumbers) {
//                        for (contactNumber in mContactsNumber) {
//                            if (contactNumber.contains(number)) {
//                                messages.add(messageItem)
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (e: ConcurrentModificationException) {
//            Log.d(
//                "TAG_CONCURRE" +
//                    "NT_MODIFICATION", "compareAllMessagesWithContacts: ${e.localizedMessage}"
//            )
//            return
//        }
////                }
//        Log.d("TAG_PERSONAL", "onOptionType: 3 ")
//        val sortedConversationsmessages =
//            messages.sortedWith(compareByDescending<MessagesModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending { it.date })
//                .toMutableList() as ArrayList<MessagesModel>
//
//        Log.e("Event: ", "result Personal: " + sortedConversationsmessages.size)
//
//        val messagesNew = ArrayList<MessagesModel>()
//
//        Log.d("TAG_PERSONAL", "onOptionType: 4 ")
//        for (i in sortedConversationsmessages.indices) {
//            if (i == 0) {
//                messagesNew.add(sortedConversationsmessages.get(i))
//            } else {
//                if (!checkIsInList(sortedConversationsmessages.get(i).participants.get(0).phoneNumbers.toString(), messagesNew)) {
//                    messagesNew.add(sortedConversationsmessages.get(i))
//                }
//            }
//        }
//        Log.d("TAG_PERSONAL", "onOptionType: 5 ")
////        showSearchResultsNew(messagesNew, searchKey)
//    }


    private fun onOptionTypeNew() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
            Log.d("TAG_EXCEPTION", "onOptionType: ${e.message}")
        }

        try {
            if (isAdded && !mActivity!!.isFinishing) {
                CoroutineScope(Dispatchers.IO).launch {
                    val allConversation = mActivity!!.conversationsDB.getAllList().toMutableList()
                    val actualConversation = MainAppClass.removeCommonItems(allConversation, MainAppClass.application?.archivedMessageDao!!.getArchivedUser())

                    Log.d("TAG_PERSONAL", "onOptionType: 1 ${actualConversation.size} ")

                    if (MainAppClass.mAllContacts.isNotEmpty()) {
                        setPersonalConversation(MainAppClass.mAllContacts, actualConversation)
                    } else {
                        SimpleContactsHelperUtils(mActivity!!).getAvailableContacts(false) {
                            setPersonalConversation(it, actualConversation)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("TAG_ERROR", "onOptionType: ${e.message}")
        }
    }

    private fun setPersonalConversation(mAllContacts: ArrayList<ContactsModel>, messagesLocal: List<ConversationSmsModel>) {
        CoroutineScope(Dispatchers.IO).launch {
            val messages = ArrayList<ConversationSmsModel>()
            Log.d("TAG_ALL_CONTACT", "onOptionType: ${mAllContacts.size}")
            for (contact in mAllContacts) {
                for (conversation in messagesLocal) {
                    Log.d("TAG_CONTACT", "onOptionTypeNew: ${contact.name} ${conversation.title}")
                    if (contact.name.lowercase() == conversation.title.lowercase()) {
                        messages.add(conversation)
                    }
                }
            }

            Log.d("TAG_PERSONAL", "onOptionType: 3 ")
            sortedConversations =
                messages.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending { it.date })
                    .toMutableList() as ArrayList<ConversationSmsModel>

            if (isInternetAvailable(requireActivity())) {
                if (!sortedConversations.any { it.date == 1.toLong() }) {
                    sortedConversations.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                }
            }

            Log.e("TAG_CURRENT_POS: ", "result Personal: " + sortedConversations.size)
            withContext(Dispatchers.Main) {
                if (mActivity!!.getSharedPrefs().getBoolean(IS_FETCH_PERSONAL_CONVERSATION, false)) {
                    (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = false
                }
                if (sortedConversations.size == 1) {
                    noDataView()
                } else {
                    visibleDataView()
                    mAdapter!!.setData(sortedConversations)
                }
            }
        }
    }

//    @SuppressLint("NotifyDataSetChanged")
//    private fun showSearchResultsNew(messages: ArrayList<MessagesModel>, searchedText: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val searchResults = ArrayList<SearchModel>()
//                messages.forEach { message ->
//                    var recipient = message.senderName
//                    val phoneNumber = message.participants[0].phoneNumbers[0]
//                    if (recipient.isEmpty() && message.participants.isNotEmpty()) {
//                        val participantNames = message.participants.map { it.name }
//                        recipient = TextUtils.join(", ", participantNames)
//                    }
//
//                    val date = message.date.formatDateOrTime(mActivity!!, true, true)
//                    val searchResult =
//                        SearchModel(message.id, recipient, message.body, message.date.toString(), message.threadId, message.senderPhotoUri, phoneNumber)
//                    searchResults.add(searchResult)
//                }
//                Log.d("TAG_PERSONAL", "onOptionType: 7 ")
//                searchResultsAds.clear()
//                val filterAfterRemoveArchive = removeArchiveItem(searchResults, mActivity!!.archivedMessageDao.getArchivedUser())
//                val filterLocalAll = removeBlockItem(filterAfterRemoveArchive, mActivity!!.blockContactDao.getAllBlockNo())
//                for (i in filterLocalAll.indices) {
//                    searchResultsAds.add(filterLocalAll[i])
//                }
////
//                Log.d("TAG_PERSONAL", "onOptionType: 8 ")
//                if (isAdded && isInternetAvailable(requireActivity())) {
//                    if (searchResultsAds.any { it.date == "1" }) {
//
//                    } else {
//                        searchResultsAds.add(0, SearchModel(0, "", "", "1", 0, "", ""))
//                    }
//                }
//
//                Log.d("TAG_SIZE", "showSearchResultsNew: ${searchResults.size}")
//
//                withContext(Dispatchers.Main) {
//                    mAdapter!!.setData(searchResultsAds)
//                    if (searchResultsAds.size == 1) {
//                        noDataView()
//                    } else {
//                        visibleDataView()
//                    }
//                }
//                Log.d("TAG_PERSONAL", "onOptionType: final ")
//            } catch (e: Exception) {
//                Log.d("TAG_ERROR", "showSearchResultsNew: ${e.message}")
//            }
//        }
//
//    }

    fun noDataView() {
        try {
            if (isAdded && mActivity!!.getSharedPrefs().getBoolean(IS_FETCH_PERSONAL_CONVERSATION, false)) {
                binding.conversationsFastscroller.beGone()
                binding.tvNoData1.beVisible()
                binding.ivThumbNodata.beVisible()
                binding.ivThumbNodata.setImageResource(R.drawable.icon_placeholder)
                binding.tvNoData1.text = resources.getString(R.string.no_personal_conversations_found)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun visibleDataView() {
        try {
            if (isAdded) {
                binding.conversationsFastscroller.beVisible()
                binding.tvNoData1.beGone()
                binding.ivThumbNodata.beGone()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
