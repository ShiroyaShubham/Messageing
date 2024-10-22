package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.adapters.ChatHistoryAdapter1
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityArchivedMessageBinding
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.model.*
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ArchivedMessageActivity : BaseHomeActivity() {
    private lateinit var binding: ActivityArchivedMessageBinding
    private var bus: EventBus? = null
    var mAdapter: ChatHistoryAdapter1? = null
    private var storedFontSize = 0
    var sortedConversations: ArrayList<ConversationSmsModel> = ArrayList()
    private val MAKE_DEFAULT_APP_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        binding = ActivityArchivedMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        bindHandlers()
        if (!PrefClass.isProUser){
        showBannerAds(findViewById(R.id.mBannerAdsContainer))
        }else{
            findViewById<ViewGroup>(R.id.mBannerAdsContainer)?.visibility = View.GONE
        }
    }


    private fun bindHandlers() {
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }


    private fun initData() {
        binding.header.txtHeading.text = getString(R.string.app_archived)
        mAdapter = ChatHistoryAdapter1(this@ArchivedMessageActivity)
        mAdapter!!.itemClickListener = { position, data, isLeft ->
            if (!isLeft) {

            } else {

            }
        }
        mAdapter!!.itemClickListenerSelect = { position, data ->
            if (mAdapter!!.getIsShowSelection()) {

            } else {
                val intent = Intent(this@ArchivedMessageActivity, MessagesActivity::class.java)
                intent.putExtra(THREAD_ID, data.threadId)
                intent.putExtra(THREAD_TITLE, data.title)
                intent.putExtra(THREAD_NUMBER, data.phoneNumber)
                intent.putExtra(SNIPPET, data.snippet)
                intent.putExtra(DATE, data.date)
                startActivity(intent)
            }
        }
        binding.recyclerViewChatHistory.adapter = mAdapter
        getPermission()
    }

    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("Event: ", "All Message refreshMessages")
        Log.e("Event: ", "initMessenger 1")
        Log.d("TAG_MESSAGE", "refreshMessages: ")
        initMessenger()
    }

    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            Log.e("Event: ", "initMessenger 2")
                            initMessenger()
                            bus = EventBus.getDefault()
                            try {
                                bus!!.register(this)
                            } catch (e: Exception) {
                            }
                        }
                    } else {
                        toast(getString(R.string.unknown_error_occurred))
                        finish()
                    }
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        }
    }

    private fun initMessenger() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
            Log.d("TAG_ERROR", "initMessenger: ${e.message}")
        }
        storeStateVariables()
        Log.e("Event: ", "getCachedConversations 2")
        getCachedConversations()

    }

    private fun storeStateVariables() {
        storedFontSize = config.fontSize
    }


    private fun getCachedConversations() {
        Log.d("TAG_MESSAGE", "getCachedConversations: ")
        if (!isFinishing) {
            getAlldataFromArchive()
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnarchiveMSGS(unarchive: ArchivedModel) {
        Log.e("TAG_BACK: ", "onUnarchiveMSGS ")
        getCachedConversations()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInsertMSG(event: InsertNewMsgModel) {
        Log.e("TAG_BACK_ARCHIVED: ", "onInsertMSG  onInsertMSG Archived ${event.threadID} ${sortedConversations.size}")
        if (event.threadID != 0L) {

            /*var counter = 0
            var dataDemo: ConversationSmsModel? = null*/

            if (sortedConversations.withIndex().none { sortedConversations[it.index].threadId == event.threadID }) {
                getCachedConversations()
                return
            }

            sortedConversations.withIndex().filter {
                Log.d("TAG_SIZE_ARCHIVED", "onInsertMSG: ${sortedConversations[it.index].threadId} ${event.threadID}")
                sortedConversations[it.index].threadId == event.threadID
            }.map {
                sortedConversations[it.index].read = event.read
                sortedConversations[it.index].snippet = event.snipet
                sortedConversations[it.index].date = event.dateTime
                mAdapter?.notifyItemChanged(it.index)

                CoroutineScope(Dispatchers.IO).launch {
                    conversationsDB.insertOrUpdateMessage(sortedConversations[it.index])

                    if (sortedConversations[0].date == 1.toLong()) {
                        sortedConversations.removeAt(0)
                    }

                    val localAll =
                        sortedConversations.sortedWith(compareByDescending<ConversationSmsModel> { config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                            it.date
                        }).toMutableList() as ArrayList<ConversationSmsModel>

                    sortedConversations.clear()
                    CoroutineScope(Dispatchers.Main).launch {
                        sortedConversations.addAll(localAll)
                        mAdapter?.setData(sortedConversations)
                    }

                }
            }

        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getAlldataFromArchive() {

        CoroutineScope(Dispatchers.IO).launch {

            val conversations1 = try {
                archivedMessageDao.getArchivedUser().toMutableList() as ArrayList<ArchivedModel>
            } catch (e: Exception) {
                ArrayList()
            }

            var conversations: ArrayList<ConversationSmsModel> = arrayListOf()

            Log.e("TAG_MESSAGE", "getAlldataFromArchive: 1 ${conversations1}")

            conversations1.forEach {
                val conversation = conversationsDB.getDataFromThreadId(it.threadId)
                if (conversation != null) conversations.add(conversation)
            }
            Log.e("TAG_SIZE", "getAlldataFromArchive: 2 $conversations ")

            withContext(Dispatchers.Main) {
                if (conversations.size > 0) {

                    conversations.sortByDescending {
                        it.date
                    }

                    Log.e("TAG_SIZE", "getAlldataFromArchive: 2 ${conversations.size} $conversations")
                    sortedConversations.clear()

                    sortedConversations.addAll(conversations)
                    mAdapter?.setData(conversations)
                    binding.recyclerViewChatHistory.isVisible = true
                    binding.llNoDataFound.beGone()
                } else {
                    binding.recyclerViewChatHistory.isVisible = false
                    binding.llNoDataFound.beVisible()
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) {
            if (storedFontSize != config.fontSize) {
                if (mAdapter != null) {
                    mAdapter!!.updateFontSize()
                }
            }
            if (mAdapter != null) {
                mAdapter!!.updateDrafts()
            }
            val adjustedPrimaryColor = getAdjustedPrimaryColor()
            binding.conversationsFastscroller.updateColors(adjustedPrimaryColor)
        }
    }
}
