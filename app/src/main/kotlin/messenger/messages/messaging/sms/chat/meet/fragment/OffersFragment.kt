package messenger.messages.messaging.sms.chat.meet.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.gson.Gson
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.activity.MessagesActivity
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.utils.config
import messenger.messages.messaging.sms.chat.meet.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.adapters.ChatHistoryAdapter1
import messenger.messages.messaging.sms.chat.meet.databinding.FragmentSmsBinding
import messenger.messages.messaging.sms.chat.meet.model.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.regex.Matcher
import java.util.regex.Pattern


class OffersFragment : BaseFragment() {
    private lateinit var binding: FragmentSmsBinding
    var sortedConversations: ArrayList<ConversationSmsModel> = ArrayList()
    fun newInstance(): OffersFragment {
        val fragment = OffersFragment()
        return fragment
    }

    var mAdapter: ChatHistoryAdapter1? = null
    fun newInstance2() {
        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                onOptionTypeNew()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
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
                    intent.putExtra(DATE, it.date?.toLong())
                    startActivity(intent)
                }
            }
        }
        val layoutManager = CustomLayoutManager(requireContext(), LinearLayout.VERTICAL, false)
        binding.recyclerViewChatHistory.layoutManager = layoutManager
        binding.recyclerViewChatHistory.adapter = mAdapter


        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                onOptionTypeNew()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (isAdded && !mActivity!!.isFinishing) {
//            if (mActivity!!.config.fontSize != mActivity!!.config.fontSize) {
            if (mAdapter != null) {
                mAdapter!!.updateFontSize()
            }
//            }

            val adjustedPrimaryColor = requireContext().getAdjustedPrimaryColor()
            binding.conversationsFastscroller.updateColors(adjustedPrimaryColor)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversation(event: RefreshConversationsModel) {
        Log.e("Event: ", "OTP RefreshMessages")
        onOptionTypeNew()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConveration(event: LoadConversationsModel) {
        Log.e("TAG_REFRESH: ", "refreshConveration REFRESH Transaction MSG")
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

            if (extractUrls(event.snipet).isNotEmpty()) {
                return
            }

            if (sortedConversations.withIndex().none { sortedConversations[it.index].threadId == event.threadID }) {
                Log.d("TAG_MESSAGE", "onInsertMSG: $event")
                CoroutineScope(Dispatchers.IO).launch {
                    val conversation =
                        ConversationSmsModel(event.threadID, event.snipet, event.dateTime, event.read, event.number, "", false, event.number, "")
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
                    blockContactModel.threadID,
                    blockContactModel.msg,
                    blockContactModel.dateTime,
                    true,
                    blockContactModel.name,
                    "",
                    false,

                    blockContactModel.number
                )
                if (extractUrls(mActivity!!.getSnippetFromThreadId(blockContactModel.threadID).toString()).isNotEmpty()) {
                    conversations.add(conversation)
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

    val searchKey = "Link"
    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private fun onOptionTypeNew() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
        }
        try {
            if (isAdded && !mActivity!!.isFinishing) {
                CoroutineScope(Dispatchers.IO).launch {

                    val searchQuery = "%$searchKey%"

                    val offerConversation = mActivity!!.conversationsDB.getConversationWithText(searchQuery).toMutableList()
                    Log.e("Event: ", "messagesAll size offer: " + offerConversation.size)
                    val offerMessage = checkLinkIsValid(offerConversation).toMutableList()

                    val actualOfferConversation =
                        MainAppClass.removeCommonItems(offerMessage, MainAppClass.application?.archivedMessageDao!!.getArchivedUser())

                    sortedConversations = actualOfferConversation.sortedWith(
                        compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }
                            .thenByDescending { it.date.toString() }
                    ).toMutableList() as ArrayList<ConversationSmsModel>

                    if (isInternetAvailable(requireActivity())) {
                        if (!sortedConversations.any { it.date == 1.toLong() }) {
                            sortedConversations.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (sortedConversations.size == 1) {
                            noDataView()
                        } else {
                            visibleDataView()
                            mAdapter!!.setData(sortedConversations)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("TAG_ERROR", "onOptionType: ${e.message}")
        }
    }

    private var bus: EventBus? = null

    fun checkLinkIsValid(messages: List<ConversationSmsModel>): List<ConversationSmsModel> {

        val localResult = ArrayList<ConversationSmsModel>()
        messages.forEach { message ->
            val text = message.snippet
            val listUrl = extractUrls(text)
            if (listUrl.isNotEmpty()) {
                localResult.add(message)
            }
        }

        return localResult

    }

    fun extractUrls(text: String): List<String> {
        val containedUrls: MutableList<String> = ArrayList()
        val urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"
        val pattern: Pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlMatcher: Matcher = pattern.matcher(text)
        while (urlMatcher.find()) {
            containedUrls.add(
                text.substring(
                    urlMatcher.start(0),
                    urlMatcher.end(0)
                )
            )
        }
        return containedUrls
    }


    fun noDataView() {
        try {
            if (!requireActivity().isFinishing && isAdded) {
                binding.conversationsFastscroller.beGone()
                binding.tvNoData1.beVisible()
                binding.ivThumbNodata.beVisible()
                binding.ivThumbNodata.setImageResource(R.drawable.icon_placeholder)
                binding.tvNoData1.text = resources.getString(R.string.no_offer_messages_found)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun visibleDataView() {
        try {
            if (!requireActivity().isFinishing && isAdded) {
                binding.conversationsFastscroller.beVisible()
                binding.tvNoData1.beGone()
                binding.ivThumbNodata.beGone()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
