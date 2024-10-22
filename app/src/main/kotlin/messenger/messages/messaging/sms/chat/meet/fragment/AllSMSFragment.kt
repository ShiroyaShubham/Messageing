package messenger.messages.messaging.sms.chat.meet.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.*
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.HomeActivity
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.activity.MessagesActivity
import messenger.messages.messaging.sms.chat.meet.activity.ContactsActivity
import messenger.messages.messaging.sms.chat.meet.adapters.ChatHistoryAdapter1
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.databinding.FragmentSmsBinding
import messenger.messages.messaging.sms.chat.meet.model.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar
import java.util.Locale


class AllSMSFragment : BaseFragment() {
    private lateinit var binding: FragmentSmsBinding
    private var bus: EventBus? = null
    var mAdapter: ChatHistoryAdapter1? = null
    private var storedFontSize = 0
    var sortedConversations: ArrayList<ConversationSmsModel> = ArrayList()
    private val MAKE_DEFAULT_APP_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSmsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bus!!.isRegistered(this)) {
            bus!!.unregister(this)
        }
//        bus?.unregister(this)
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && !mActivity!!.isFinishing) {
            if (mAdapter != null) {
                mAdapter!!.updateFontSize()
                mAdapter!!.updateDrafts()
            }
            binding.tvNoData2.underlineText()
            val adjustedPrimaryColor = requireContext().getAdjustedPrimaryColor()
            binding.conversationsFastscroller.updateColors(adjustedPrimaryColor)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("Event: ", "onViewCreated")
        mAdapter = ChatHistoryAdapter1(mActivity!! as BaseHomeActivity)
        binding.recyclerViewChatHistory.setHasFixedSize(true)
        binding.recyclerViewChatHistory.isNestedScrollingEnabled = false

        mAdapter!!.itemClickListenerSelect = { position, data ->
//            if (mAdapter!!.getIsShowSelection()) {
//                mAdapter!!.getFileListData()[position].isSelected = !mAdapter!!.getFileListData()[position].isSelected
//                mAdapter!!.notifyItemChanged(position)
//                selectedConversations = mAdapter!!.getFileListDataSelected()
//
//                visibilityOfPin()
//                showCounterLayout()
//
//                Log.e("TAG", "onViewCreated: ${sortedConversations.filter { it.isSelected }}")

//            } else {
//
//                showInterstitialAdPerDayOnMessageClick {
//                    val intent = Intent(mActivity!!, MessagesActivity::class.java)
//                    intent.putExtra(THREAD_ID, data.threadId)
//                    intent.putExtra(THREAD_TITLE, data.title)
//                    intent.putExtra(THREAD_NUMBER, data.phoneNumber)
//                    intent.putExtra(SNIPPET, data.snippet)
//                    intent.putExtra(DATE, data.date)
//                    startActivity(intent)
//                }
//            }
            Log.e("TAG", "onViewCreated:>>>> "+data.title)

            showInterstitialAdPerDayOnMessageClick {
                val intent = Intent(mActivity!!, MessagesActivity::class.java)
                intent.putExtra(THREAD_ID, data.threadId)
                intent.putExtra(THREAD_TITLE, data.title)
                intent.putExtra(THREAD_NUMBER, data.phoneNumber)
                intent.putExtra(SNIPPET, data.snippet)
                intent.putExtra(DATE, data.date)
                startActivity(intent)
            }
        }

        val layoutManager = CustomLayoutManager(requireContext(), LinearLayout.VERTICAL, false)
        binding.recyclerViewChatHistory.layoutManager = layoutManager
        binding.recyclerViewChatHistory.adapter = mAdapter
        binding.tvNoData2.setOnClickListener {
            Intent(mActivity!!, ContactsActivity::class.java).apply {
                startActivity(this)
            }
        }
        getPermission()
        if (MainAppClass.conversationsDBDATA.size > 0) {
            sortedConversations.clear() // Clear the existing data
            sortedConversations.addAll(MainAppClass.conversationsDBDATA)
            Log.e("TAG_ALL_SMS", "getConversationDataFromDB: 5")
            mAdapter?.setData(sortedConversations)
        }
    }

    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {
                toast(requireContext().getString(R.string.unknown_error_occurred))
                (mActivity!! as HomeActivity).finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(requireContext()) == mActivity!!.packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, mActivity!!.packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConveration(event: LoadConversationsModel) {
        Log.e("TAG_ALL_SMS: ", "refreshConveration REFRESH MSG")
        getCachedConversationsOrignal()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshWhileBackMessages(event: RefreshWhileBackModel.RefreshMessages) {
        Log.e("TAG_ALL_SMS: ", "refreshWhileBackMessages REFRESH MSG")
        getCachedConversationsOrignal()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackupImport(event: BackupImportModel) {
        Log.e("TAG_ALL_SMS: ", " onBackupImport REFRESH MSG")
        getImportedChatFromData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMarkAsReadOrUnread(event: MarkAsReadModel) {
        Log.e("TAG_ALL_SMS: ", "onMarkAsReadOrUnread MARK AS READ ${event.isRead} ${sortedConversations.size}")
        if (event.isRead) {
            sortedConversations.withIndex().filter {
                sortedConversations[it.index].threadId == event.threadID
            }.map {
                sortedConversations[it.index].read = event.isRead
                mAdapter?.notifyItemChanged(it.index)
                CoroutineScope(Dispatchers.IO).launch {
                    mActivity!!.conversationsDB.insertOrUpdateMessage(sortedConversations[it.index])
                }
                Log.e("TAG_ALL_SMS", "onMarkAsReadOrUnread: ${it.index}")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnarchiveMSGS(unarchive: ArchivedModel) {
        Log.e("TAG_ALL_SMS: ", "onUnarchiveMSGS ")
        getCachedConversationsOrignal()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInsertMSG(event: InsertNewMsgModel) {
        Log.e("TAG_ALL_SMS: ", "onInsertMSG  onInsertMSG  ${event.threadID}")
        if (event.threadID != 0L) {

            /*var counter = 0
            var dataDemo: ConversationSmsModel? = null*/

            if (sortedConversations.withIndex().none { sortedConversations[it.index].threadId == event.threadID }) {
                Log.d("TAG_MESSAGE", "onInsertMSG: $event")
                CoroutineScope(Dispatchers.IO).launch {
                    val conversation = ConversationSmsModel(event.threadID, event.snipet, event.dateTime, event.read, event.number, "", false, event.number, "")
                    mActivity!!.conversationsDB.insertOrUpdateMessage(conversation)
                    sortedConversations.add(conversation)
                    val localAll =
                        sortedConversations.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                            it.date
                        }).toMutableList() as ArrayList<ConversationSmsModel>
                    withContext(Dispatchers.Main) {
                        mAdapter?.setData(localAll)
                    }
                    MainAppClass.getConversationDataFromDB()
                }
//                getCachedConversationsOrignal()
                return
            }

            sortedConversations.withIndex().filter { sortedConversations[it.index].threadId == event.threadID }.forEach  {
                sortedConversations[it.index].read = event.read
                sortedConversations[it.index].snippet = event.snipet
                sortedConversations[it.index].date = event.dateTime
                mAdapter?.notifyItemChanged(it.index)
//                mAdapter?.notifyItemMoved(it.index, 1)
                /* counter = it.index
                 dataDemo = sortedConversations[it.index]*/
                CoroutineScope(Dispatchers.IO).launch {
                    mActivity!!.conversationsDB.insertOrUpdateMessage(sortedConversations[it.index])
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
                        Log.d("TAG_ALL_SMS", "onInsertMSG: ")
                        mAdapter?.setData(sortedConversations)
                    }

                }
            }


        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInsertNewChat(event: InsertNewChatModel) {
        Log.e("TAG_ALL_SMS: ", "onInsertNewChat MARK AS READ")

//        getCachedConversationsOrignal()
        CoroutineScope(Dispatchers.IO).launch {
            mActivity!!.conversationsDB.insertOrUpdateMessage(
                ConversationSmsModel(
                    event.threadID, event.snipet, event.dateTime, true, event.title, "", false, event.number
                )
            )

            if (isInternetAvailable(requireActivity())) {
                if (sortedConversations[0].date == 1.toLong()) {
                    sortedConversations.add(
                        1, ConversationSmsModel(
                            event.threadID, event.snipet, event.dateTime, true, event.title, "", false, event.number
                        )
                    )
                } else {
                    sortedConversations.add(
                        0, ConversationSmsModel(
                            event.threadID, event.snipet, event.dateTime, true, event.title, "", false, event.number
                        )
                    )
                }
            } else {
                sortedConversations.add(
                    0, ConversationSmsModel(
                        event.threadID, event.snipet, event.dateTime, true, event.title, "", false, event.number
                    )
                )
            }

            withContext(Dispatchers.Main) {
                Log.d("TAG_ALL_SMS", "onInsertNewChat: ")
                mAdapter?.setData(sortedConversations)
                binding.recyclerViewChatHistory.smoothScrollToPosition(0)
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnBlockMessages(numberList: UnBlockMsgListModel) {
        Log.e("TAG_ALL_SMS: ", "onUnBlockMessages   ${numberList.msgList}")

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
                conversations.add(conversation)
            }

            Log.d("TAG_ALL_SMS", "onUnBlockMessages: ${Gson().toJson(conversations)}")

            sortedConversations.addAll(conversations)

            if (sortedConversations[0].date == 1.toLong()) {
                sortedConversations.removeAt(0)
            }

            mActivity!!.conversationsDB.insertAllInConversationTransaction(conversations)
            MainAppClass.getAllMessagesFromDb {
                refreshMessages()
            }

            val localAll =
                sortedConversations.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                    it.date
                }).toMutableList() as ArrayList<ConversationSmsModel>
            CoroutineScope(Dispatchers.Main).launch {

                if (isInternetAvailable(requireActivity())) {
                    if (sortedConversations.filter { it.date == 1.toLong() }.size > 0) {

                    } else {
                        localAll.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                    }
                }
                Log.d("TAG_ALL_SMS", "onUnBlockMessages: 1 ${sortedConversations.size}")
                sortedConversations.clear()
                sortedConversations.addAll(localAll)
                Log.d("TAG_ALL_SMS", "onUnBlockMessages: 2 ${sortedConversations.size}")
                mAdapter?.setData(localAll)
//                getCachedConversationsOrignal()
                binding.recyclerViewChatHistory.smoothScrollToPosition(0)
            }
        }

    }


    fun newInstance2() {
        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                mActivity!!.runOnUiThread {
//                getCachedConversationsFirst()
                    getCachedConversationsOrignal()
                }
            }
        }
    }

    fun newInstance(): AllSMSFragment {
        val fragment = AllSMSFragment()
        return fragment
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("Event: ", "All Message refreshMessages")
        Log.e("Event: ", "initMessenger 1")
        Log.d("TAG_ALL_SMS", "refreshMessages: ")
//        initMessenger()
    }

    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional. If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            Log.e("Event: ", "initMessenger 2")
                            initMessenger()
                            MainAppClass.setupSIMSelector()
                            MainAppClass.getAllAvailableContact {
                                refreshMessages()
                            }
                            bus = EventBus.getDefault()
                            try {
                                bus!!.register(this)
                            } catch (e: Exception) {
                            }
                        }
                    } else {
                        toast(requireContext().getString(R.string.unknown_error_occurred))
                        (mActivity!! as HomeActivity).finish()
                    }
                }
            } else {
                toast(requireContext().getString(R.string.unknown_error_occurred))
                (mActivity!! as HomeActivity).finish()
            }
        }
    }

    private fun initMessenger() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
        }
        storeStateVariables()
        Log.e("Event: ", "getCachedConversations 2")
        getCachedConversations()

    }

    private fun storeStateVariables() {
        storedFontSize = mActivity!!.config.fontSize
    }

    private fun getCachedConversationsOrignal() {
        Log.d("TAG_ALL_SMS", "getCachedConversations: ")
        if (isAdded && !mActivity!!.isFinishing) {
            sortedConversations.clear()
            CoroutineScope(Dispatchers.IO).launch {

                val conversations = try {
                    mActivity!!.conversationsDB.getAllList().toMutableList() as ArrayList<ConversationSmsModel>
                } catch (e: Exception) {
                    ArrayList()
                }


                var LocalAllWithArchive: ArrayList<ConversationSmsModel> = arrayListOf()
                if (conversations.size > 0) {
                    LocalAllWithArchive = removeCommonItems(conversations, mActivity!!.archivedMessageDao.getArchivedUser())
                }

                withContext(Dispatchers.Main) {
                    if (conversations.size > 0) {

//                        val LocalAllWithArchive = removeCommonItems(conversations, mActivity!!.archivedMessageDao.getArchivedUser())

                        val localAll =
                            LocalAllWithArchive.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                                it.date
                            }).toMutableList() as ArrayList<ConversationSmsModel>

                        if (isInternetAvailable(requireActivity())) {
                            if (sortedConversations.filter { it.date == 1.toLong() }.size > 0) {

                            } else {
                                localAll.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                            }
                        }

                        sortedConversations.addAll(localAll)
                        Log.d("TAG_ALL_SMS", "getCachedConversationsOrignal: ")
                        mAdapter?.setData(localAll)

                        getNewConversations(localAll)

                    } else {
                        // binding.mPBSyncMessage.visibility = View.VISIBLE
                        //binding.txtSyncronizeData.visibility = View.VISIBLE

                        withContext(Dispatchers.IO) {
                            //getNewConversations1(conversations)
                            getNewConversations2()
                        }
                    }
                    mActivity!!.updateUnreadCountBadge(conversations)
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun getCachedConversations() {
        Log.d("TAG_ALL_SMS", "getCachedConversations: ")
        if (isAdded && !mActivity!!.isFinishing) {
//            sortedConversations.clear()
            lifecycleScope.launch(Dispatchers.IO) {
//                val conversations = try {
//                    mActivity!!.conversationsDB.getAllList().toMutableList() as ArrayList<ConversationSmsModel>
//                } catch (e: Exception) {
//                    Log.d("TAG_ERROR", "getCachedConversations: ${e.message}")
//                    ArrayList()
//                }
//
//                var LocalAllWithArchive: ArrayList<ConversationSmsModel> = arrayListOf()
//                if (conversations.size > 0) {
//                    LocalAllWithArchive = removeCommonItems(conversations, mActivity!!.archivedMessageDao.getArchivedUser())
//                }

                val conversations = MainAppClass.conversationsDBDATA
                Log.d("TAG_ALL_SMS", "getCachedConversations: ${conversations.size}")
                withContext(Dispatchers.Main) {
                    if (conversations.size > 0) {
//                        val localAll =
//                            LocalAllWithArchive.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
//                                it.date
//                            }).toMutableList() as ArrayList<ConversationSmsModel>
//
//                        Log.d("TAG_CONVERSATION", "getCachedConversations: ${localAll.size}")
//                        sortedConversations.addAll(localAll)
//                        mAdapter!!.setData(localAll)
                        (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = false
                        getNewConversations(sortedConversations)
                    } else {
//                        withContext(Dispatchers.IO) {
//                        (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = true
                        getNewConversations2()
//                        }
                    }
                    mActivity!!.updateUnreadCountBadge(conversations)
                }
//                withContext(Dispatchers.Main) {
//                    Log.e("Event: ", "setupConversa
                //                    tions 1")

//                    setupConversations(conversations)
//                }
            }
        }
    }

    private fun getNewConversations2() {
        //val privateCursor = mActivity!!.getMyContactsCursor(false, true)?.loadInBackground()
        var arrayData = ArrayList<ConversationSmsModel>()
        CoroutineScope(Dispatchers.IO).launch {
            /*  val job = async {
                  val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(mActivity!!, privateCursor)
                  val conversations = mActivity!!.getConversationss(privateContacts = privateContacts)
                  conversations
              }
              arrayData = job.await()*/


            val itemsAddressId = ArrayList<RecipientModel>()
            val contactList = ArrayList<Contact>()
            withContext(Dispatchers.IO) {
                val simpleContactHelper = SimpleContactsHelperUtils(mActivity!!)
                Log.d("TAG_ALL_SMS", "getNewConversations2: ${simpleContactHelper.getContacts().size}")
                contactList.addAll(simpleContactHelper.getContacts())
                itemsAddressId.addAll(mActivity!!.getAddressId())
            }

            withContext(Dispatchers.IO) {
                // val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(mActivity!!, privateCursor)
                val conversations = mActivity!!.getConversationss()
                arrayData.addAll(conversations)
            }

            withContext(Dispatchers.IO) {
                for (item in arrayData) {
                    val recipientIds = item.rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()

                    //  val phoneNumbers = itemsAddressId.find { it.id == item.rawIds.toLong() }?.address
                    val phoneNumbers = getAddressFromRecipientModel(recipientIds, itemsAddressId)
                    // val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""

                    val names = ArrayList<String>()
                    phoneNumbers.forEach { number ->
                        val contact = contactList.firstOrNull {
                            it.address == number
                        }

                        val name = contact?.name
                        if (name.isNullOrEmpty()) {
                            val title = mActivity!!.getContactNameFromPhoneNumber(number)
                            if (title.isNotEmpty()) names.add(title)
                            else names.add(number)
                        } else {
                            names.add(name)
                        }

                    }

                    val title = TextUtils.join(", ", names.toTypedArray())
                    val isGroupConversation = false
                    val photoUri = try {
                        contactList.firstOrNull {
                            it.address == phoneNumbers.first()
                        }?.photoUri ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                    item.phoneNumber = try {
                        phoneNumbers.first()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                    item.title = title
                    item.photoUri = photoUri
                    item.isGroupConversation = isGroupConversation
                }
            }

            withContext(Dispatchers.Main) {
                setupConversations1(arrayData)
                (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = false
                requireActivity().getSharedPrefs().edit().putString(LAST_SYNC_DATE, Calendar.getInstance(Locale.US).timeInMillis.toString()).apply()
            }

            withContext(Dispatchers.IO) {
                mActivity!!.conversationsDB.insertAllInConversationTransaction(arrayData)
                MainAppClass.getConversationDataFromDB()
                mActivity!!.getSharedPrefs().edit().putBoolean(IS_FETCH_ALL_CONVERSATION, true).apply()
                EventBus.getDefault().post(RefreshConversationsModel())
                MainAppClass.getAllAvailableContact {
                    Log.d("TAG_REFRESH", "getNewConversations2: ")
                    mActivity!!.getSharedPrefs().edit().putBoolean(IS_FETCH_PERSONAL_CONVERSATION, true).apply()
                    EventBus.getDefault().post(LoadConversationsModel())
                }
            }

            withContext(Dispatchers.IO) {
                for (item in arrayData) {
                    mActivity!!.setupThread(item.threadId)
                }
                MainAppClass.getAllMessagesFromDb {
                    refreshMessages()
                }
            }
        }
    }

    private fun setupConversations1(conversations: ArrayList<ConversationSmsModel>) {
        if (isAdded && !mActivity!!.isFinishing) {
            val hasConversations = conversations.isNotEmpty()/*val localAll =
                conversations.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                    it.date
                }).toMutableList() as ArrayList<ConversationSmsModel>*/

            Log.d("TAG_CONVERSATION", "setupConversations1: $hasConversations")
            if (isAdded && !mActivity!!.isFinishing) {
//                binding.conversationsFastscroller.beVisibleIf(hasConversations)
                binding.tvNoData1.beGoneIf(hasConversations)
                binding.tvNoData2.beGoneIf(hasConversations)
                binding.ivThumbNodata.beGoneIf(hasConversations)
                setAdapterNew1(conversations, hasConversations)
            }
        }
    }

    private fun setAdapterNew1(localAll: ArrayList<ConversationSmsModel>, hasConversations: Boolean) {
//        sortedConversations.clear()

        Log.e("TAG_ALL_SMS", "setupConversations  4444 ")
        CoroutineScope(Dispatchers.IO).launch {

            //   withContext(Dispatchers.IO) {
//                val filterLocalAll = removeCommonItems(localAll, mActivity!!.archivedMessageDao.getArchivedUser())
//                val size = filterLocalAll.size

            sortedConversations.addAll(localAll)
//
            //   withContext(Dispatchers.Main) {
            Log.e("TAG_ALL_SMS", "setupConversations 5555 ")
            if (isInternetAvailable(requireActivity())) {
                if (sortedConversations.any { it.date == 1.toLong() }) {

                } else {
                    sortedConversations.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                }
            }
            withContext(Dispatchers.Main) {
                Log.d("TAG_ALL_SMS", "setAdapterNew1: ")
                mAdapter!!.setData(sortedConversations)
                Log.d("TAG_CURRENT_POS", "setAdapterNew1: ${(activity as HomeActivity).binding.viewPager.currentItem}")
                if ((activity as HomeActivity).binding.viewPager.currentItem != 2) (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = false
                // }

                // }
                Log.d("TAG_ALL_SMS", "setAdapterNew: ")

                if (mAdapter == null) {
                    Log.e("Event: ", "all message currAdapter null")

                } else {
                    Log.e("Event: ", "all message currAdapter not null")
                    Log.e("Event: ", "sortedConversations: " + sortedConversations.size)
//                mAdapter!!.notifyDataSetChanged()
                    if (mAdapter!!.mConversations.isEmpty()) {
//                    binding.conversationsFastscroller.beGone()
                        binding.tvNoData1.beVisible()
                        binding.tvNoData2.beVisible()
                        binding.ivThumbNodata.beVisible()

                        if (!hasConversations && mActivity!!.config.appRunCount == 1) {
                            binding.tvNoData1.text = getString(R.string.loading_messages)
                            binding.tvNoData2.beGone()
                            binding.ivThumbNodata.beGone()
                        } else if (!hasConversations && mActivity!!.config.appRunCount != 1) {
                            binding.tvNoData1.text = getString(R.string.no_conversations_found)

                        } else {
                        }
                    } else {
//                    binding.conversationsFastscroller.beVisible()
                        binding.tvNoData1.beGone()
                        binding.tvNoData2.beGone()
                        binding.ivThumbNodata.beGone()
                    }
                }
            }

        }

    }

    private fun getAddressFromRecipientModel(recipientIds: List<Int>, itemsAddressId: ArrayList<RecipientModel>): ArrayList<String> {
        val addresses = ArrayList<String>()
        recipientIds.forEach { item ->
            val address = itemsAddressId.find { it.id == item.toLong() }?.address ?: ""
            if (address.isNotEmpty()) {
                addresses.add(address)
            }
        }

        return addresses
    }


    private fun getNewConversations(cachedConversations: ArrayList<ConversationSmsModel>) {
        Log.d("TAG_MESSAGE", "getNewConversations: Every Time")
        lifecycleScope.launch(Dispatchers.IO) {
            val privateCursor = mActivity!!.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)?.loadInBackground()
            val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(mActivity!!, privateCursor)
            val conversations = mActivity!!.getConversations(privateContacts = privateContacts)
            val blockConversations = mActivity!!.getBlockedNumbers1()
            mActivity!!.getSharedPrefs().edit().putString(LAST_SYNC_DATE, Calendar.getInstance(Locale.US).timeInMillis.toString()).apply()

            if (isAdded && !mActivity!!.isFinishing) {
//                withContext(Dispatchers.Main) {
//                    Log.e("Event: ", "setupConversations 2")
//                    setupConversations1(conversations)
//                }

                conversations.forEach { clonedConversation ->
                    if (!cachedConversations.map { it.threadId }.contains(clonedConversation.threadId)) {
                        if (blockConversations.map { it.threadID }.contains(clonedConversation.threadId)) {
                            mActivity!!.conversationsDB.insertOrUpdateMessage(clonedConversation)
                            cachedConversations.add(clonedConversation)
                        }
                        //sortedConversations.add(clonedConversation)
                    }
                }
//
//                cachedConversations.forEach { cachedConversation ->
//                    if (!conversations.map { it.threadId }.contains(cachedConversation.threadId)) {
//                        mActivity!!.conversationsDB.deleteThreadIdMessage(cachedConversation.threadId)
//                    }
//                }

                Log.d("TAG_CONVERSATION_NEW", "getNewConversations: ${cachedConversations.size}")
//                cachedConversations.forEach { cachedConversation ->
//                    val conv = conversations.firstOrNull { it.threadId == cachedConversation.threadId && it.toString() != cachedConversation.toString() }
//                    Log.d("TAG_NEW_CONVERSATION", "getNewConversations: $conv")
//                    if (conv != null) {
//                        Log.d("TAG_CURRENT_MESSAGE", "getNewConversations: ${conv.title}")
//                        mActivity!!.conversationsDB.insertOrUpdateMessage(conv)
//                    }
//                }

                conversations.forEach {
                    mActivity!!.conversationsDB.insertOrUpdateMessage(it)
                }

                Log.d("TAG_CONVERSATION_NEW", "getNewConversations: ${mActivity!!.config.appRunCount}")
//                if (mActivity!!.config.appRunCount == 1) {
                Log.d("TAG_CONVERSATION_NEW", "getNewConversations: ${conversations.size}")
                conversations.forEach {
                    val messages = mActivity!!.getMessages(it.threadId)
                    Log.d("TAG_CONVERSATION_NEW", "getNewConversations: ${messages.size}")
                    messages.forEach { message ->
                        Log.d("TAG_CONVERSATION_NEW", "getNewConversations: $message")
                        mActivity!!.messagesDB.insertAddMessages(message)
                    }
                }

                if (conversations.isEmpty()) {
                    return@launch
                }

                EventBus.getDefault().post(LoadConversationsModel())
                mActivity!!.config.appRunCount++
//                }

                CoroutineScope(Dispatchers.IO).launch {
                    val conversations = try {
                        mActivity!!.conversationsDB.getAllList().toMutableList() as ArrayList<ConversationSmsModel>
                    } catch (e: Exception) {
                        ArrayList()
                    }
                    //setupConversations(conversations)
                    sortedConversations.clear()
                    if (isAdded && !mActivity!!.isFinishing) {
                        val hasConversations = conversations.isNotEmpty()
                        var LocalAllWithArchive: ArrayList<ConversationSmsModel> = arrayListOf()
                        if (conversations.size > 0) {
                            LocalAllWithArchive = removeCommonItems(conversations, mActivity!!.archivedMessageDao.getArchivedUser())
                        }

                        val localAll =
                            LocalAllWithArchive.sortedWith(compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                                it.date
//                    Log.e("TAG_SORTING", "setupConversations: 333333 ")
                            }).toMutableList() as ArrayList<ConversationSmsModel>

                        withContext(Dispatchers.Main) {
                            if (isAdded && !mActivity!!.isFinishing) {
                                binding.conversationsFastscroller.beVisibleIf(hasConversations)
                                binding.tvNoData1.beGoneIf(hasConversations)
                                binding.tvNoData2.beGoneIf(hasConversations)
                                binding.ivThumbNodata.beGoneIf(hasConversations)
                                Log.d("TAG_CONVERSATION", "getNewConversations: ${localAll.size} $hasConversations")
                                MainAppClass.getConversationDataFromDB()
                                setAdapterNew(localAll, hasConversations)
                            }
                        }
                        MainAppClass.getAllMessagesFromDb {
                            refreshMessages()
                        }
                    }


                }

            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun setAdapterNew(localAll: ArrayList<ConversationSmsModel>, hasConversations: Boolean) {
//        sortedConversations.clear()
        CoroutineScope(Dispatchers.Main).launch {

            withContext(Dispatchers.IO) {
//                val filterLocalAll = removeCommonItems(localAll, mActivity!!.archivedMessageDao.getArchivedUser())
                Log.d("TAG_ALL_SMS", "setAdapterNew: ${localAll.size}")
                sortedConversations.addAll(localAll)


//
                Log.e("TAG_ALL_SMS", "setupConversations 5555 ${sortedConversations.size}")
                if (isInternetAvailable(requireActivity())) {
                    if (sortedConversations.filter { it.date == 1.toLong() }.isNotEmpty()) {

                    } else {
                        sortedConversations.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                    }
                }
            }
            Log.d("TAG_ALL_SMS", "setAdapterNew: ")
            mAdapter!!.setData(sortedConversations)
            (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = false

//            mAdapter!!.notifyDataSetChanged()

            if (mAdapter == null) {
                Log.e("Event: ", "all message currAdapter null")

            } else {
                Log.e("Event: ", "all message currAdapter not null")
                Log.e("Event: ", "sortedConversations: " + sortedConversations.size)
                if (mAdapter!!.mConversations.isEmpty()) {
                    binding.conversationsFastscroller.beGone()
                    binding.tvNoData1.beVisible()
                    binding.tvNoData2.beVisible()
                    binding.ivThumbNodata.beVisible()

                    if (!hasConversations && mActivity!!.config.appRunCount == 1) {
                        binding.tvNoData1.beGone()
                        binding.tvNoData2.beGone()
                        binding.ivThumbNodata.beGone()
                    } else if (!hasConversations && mActivity!!.config.appRunCount != 1) {
                        binding.tvNoData1.text = getString(R.string.no_conversations_found)
                    }
                } else {
                    binding.conversationsFastscroller.beVisible()
                    binding.tvNoData1.beGone()
                    binding.tvNoData2.beGone()

                    binding.ivThumbNodata.beGone()
                }
            }

        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e("Event: ", "askPermissions RESULT_OK")
                askPermissions()
            } else {
                toast(requireContext().getString(R.string.unknown_error_occurred))
                getPermission()
            }
        }
    }

    private fun getImportedChatFromData() {
        sortedConversations.clear()
        //val privateCursor = mActivity!!.getMyContactsCursor(false, true)?.loadInBackground()
        var arrayData = ArrayList<ConversationSmsModel>()
        CoroutineScope(Dispatchers.Default).launch {
            /*  val job = async {
                  val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(mActivity!!, privateCursor)
                  val conversations = mActivity!!.getConversationss(privateContacts = privateContacts)
                  conversations
              }
              arrayData = job.await()*/


            val itemsAddressId = ArrayList<RecipientModel>()
            val contactList = ArrayList<Contact>()
            withContext(Dispatchers.IO) {
                val simpleContactHelper = SimpleContactsHelperUtils(mActivity!!)
                contactList.addAll(simpleContactHelper.getContacts())
                itemsAddressId.addAll(mActivity!!.getAddressId())
            }

            withContext(Dispatchers.IO) {
                // val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(mActivity!!, privateCursor)
                val conversations = mActivity!!.getConversationsImportNew()
                arrayData.addAll(conversations)
            }

            withContext(Dispatchers.IO) {
                for (item in arrayData) {
                    val recipientIds = item.rawIds.split(" ").filter { it.areDigitsOnly() }.map { it.toInt() }.toMutableList()

                    //  val phoneNumbers = itemsAddressId.find { it.id == item.rawIds.toLong() }?.address
                    val phoneNumbers = getAddressFromRecipientModel(recipientIds, itemsAddressId)
                    // val photoUri = if (phoneNumbers.size == 1) simpleContactHelper.getPhotoUriFromPhoneNumber(phoneNumbers.first()) else ""

                    val names = ArrayList<String>()
                    phoneNumbers.forEach { number ->
                        val contact = contactList.firstOrNull {
                            it.address == number
                        }
                        val name = contact?.name ?: ""
                        if (name.isNotEmpty()) {
                            names.add(name)
                        } else {
                            names.add(number)
                        }
                    }

                    val title = TextUtils.join(", ", names.toTypedArray())
                    val isGroupConversation = false
                    val photoUri = try {
                        contactList.firstOrNull {
                            it.address == phoneNumbers.first()
                        }?.photoUri ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                    item.phoneNumber = try {
                        phoneNumbers.first()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                    item.title = title
                    item.photoUri = photoUri
                    item.isGroupConversation = isGroupConversation
                }
            }

            withContext(Dispatchers.Main) {
                setupConversations1(arrayData)
                (activity as HomeActivity).binding.llSyncMsgProgress.isVisible = false
                requireActivity().getSharedPrefs().edit().putString(LAST_SYNC_DATE, Calendar.getInstance(Locale.US).timeInMillis.toString()).apply()
            }

            withContext(Dispatchers.IO) {
                mActivity!!.conversationsDB.insertAllInConversationTransaction(arrayData)
                MainAppClass.getConversationDataFromDB()
            }

            withContext(Dispatchers.IO) {
                for (item in arrayData) {
                    mActivity!!.setupThread(item.threadId)
                }
                MainAppClass.getAllMessagesFromDb {
                    refreshMessages()
                }
            }
        }
    }

}
