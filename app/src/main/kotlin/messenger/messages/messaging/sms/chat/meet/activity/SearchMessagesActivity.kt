package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.adapters.SearchAdapter
import messenger.messages.messaging.sms.chat.meet.adapters.SuggestionAdapter
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.utils.config
import messenger.messages.messaging.sms.chat.meet.utils.conversationsDB
import messenger.messages.messaging.sms.chat.meet.utils.messagesDB
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.model.MessagesModel
import messenger.messages.messaging.sms.chat.meet.model.SearchModel
import messenger.messages.messaging.sms.chat.meet.model.ContactsModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.databinding.ActivitySearchMessageBinding

class SearchMessagesActivity : BaseHomeActivity() {
    private lateinit var binding: ActivitySearchMessageBinding
    private var mLastSearchedText = ""

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSearch()

        binding.tvCancel.setOnClickListener {
            onBackPressed()
        }



        ensureBackgroundThread {
            val messagesAll = messagesDB.getAllList()

            val searchKeyCredit = "Credit"
            val searchQueryCredit = "%$searchKeyCredit%"
            val messagesCredit = messagesDB.getMessagesWithText(searchQueryCredit)

            val searchKeyDebit = "Debit"
            val searchQueryDebit = "%$searchKeyDebit%"
            val messagesDebit = messagesDB.getMessagesWithText(searchQueryDebit)

            val searchKeyOTP = "OTP"
            val searchQueryOTP = "%$searchKeyOTP%"
            val messagesOTP = messagesDB.getMessagesWithText(searchQueryOTP)

            val searchKeyLink = "Link"
            val searchQueryLink = "%$searchKeyLink%"
            val messagesLink = messagesDB.getMessagesWithText(searchQueryLink)


            val messagesLocal = messagesAll - messagesCredit - messagesDebit - messagesOTP - messagesLink

            val messages = ArrayList<MessagesModel>()
            var mAllContacts: ArrayList<ContactsModel>
            val mContactsNumber = ArrayList<String>()

//            SimpleContactsHelperUtils(this).getAvailableContacts(false) {
                mAllContacts = MainAppClass.mAllContacts

                for (item in mAllContacts) {
                    mContactsNumber.add(item.phoneNumbers.toString().replace("[", "").replace("]", ""))
                }

                for (messageItem in messagesLocal) {
                    val messageNum = messageItem.participants.get(0).phoneNumbers
                    for (contactNumber in mContactsNumber) {
                        if (messageNum.contains(contactNumber)) {
                            messages.add(messageItem)
                        }
                    }
                }
//            }

            val sortedConversationsmessages = messages.sortedWith(
                compareByDescending<MessagesModel> { config.pinnedConversations.contains(it.threadId.toString()) }
                    .thenByDescending { it.date }
            ).toMutableList() as ArrayList<MessagesModel>

            val messagesNew = ArrayList<MessagesModel>()
            for (i in sortedConversationsmessages.indices) {
                if (i == 0) {
                    messagesNew.add(sortedConversationsmessages.get(i))
                } else {
                    if (!checkIsInList(sortedConversationsmessages.get(i).participants.get(0).phoneNumbers.toString(), messagesNew)) {
                        messagesNew.add(sortedConversationsmessages.get(i))
                    }
                }
            }
            runOnUiThread {
                val searchResults = ArrayList<SearchModel>()
                messagesNew.forEach { message ->
                    var recipient = message.senderName
                    val phoneNumber = message.participants[0].phoneNumbers[0]
                    if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                        val participantNames = message.participants.map { it.name }
                        recipient = TextUtils.join(", ", participantNames)
                    }

//                    val date = message.date.formatDateOrTime(this, true, true)
                    val searchResult = SearchModel(message.id, recipient, message.body, message.date.toString(), message.threadId, message.senderPhotoUri, phoneNumber)
                    searchResults.add(searchResult)
                }

                SuggestionAdapter(this, searchResults, binding.recyclerViewsSuggestions) {
                    val intentApp = Intent(this, MessagesActivity::class.java)
                    intentApp.putExtra(THREAD_ID, (it as SearchModel).threadId)
                    intentApp.putExtra(THREAD_TITLE, it.title)
                    intentApp.putExtra(THREAD_NUMBER, it.phoneNumber)
                    intentApp.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                    startActivity(intentApp)
                }.apply {
                    binding.recyclerViewsSuggestions.adapter = this
                }
            }
        }
    }


    fun setupSearch() {
        binding.etSearchText.requestFocus()
        binding.etSearchText.apply {

            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(newText: CharSequence?, start: Int, before: Int, count: Int) {
                    mLastSearchedText = newText.toString()
                    textChanged(newText.toString())
                }
            })
        }
    }

    fun checkIsInList(phoneNumber: String, messagesNew: ArrayList<MessagesModel>): Boolean {
        for (contactNumber in messagesNew) {
            if (phoneNumber.contains(contactNumber.participants[0].phoneNumbers.toString())) {
                return true
            }
        }
        return false
    }

    private fun textChanged(text: String) {
        binding.tvNoData2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            ensureBackgroundThread {
                val searchQuery = "%$text%"
                val messages = messagesDB.getMessagesWithText(searchQuery)
                val conversations = conversationsDB.getConversationsWithTextMessage(searchQuery)
                if (text == mLastSearchedText) {
                    showSearchResults(messages, conversations, text)
                }
            }
        } else {
            binding.tvNoData1.beVisible()
            binding.ivThumbNodata.beVisible()
            binding.recyclerSearchContact.beGone()
        }
    }

    private fun showSearchResults(messages: List<MessagesModel>, conversations: List<ConversationSmsModel>, searchedText: String) {
        val searchResults = ArrayList<SearchModel>()
        val searchResultsAds = ArrayList<SearchModel>()
        conversations.forEach { conversation ->
//            val date = conversation.date.formatDateOrTime(this, true, true)
            val searchResult =
                SearchModel(-1, conversation.title, conversation.phoneNumber, conversation.date.toString(), conversation.threadId, conversation.photoUri, conversation.phoneNumber)
            searchResults.add(searchResult)
        }

        messages.forEach { message ->
            var recipient = message.senderName
            val phoneNumber = message.participants[0].phoneNumbers[0]
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                val participantNames = message.participants.map { it.name }
                recipient = TextUtils.join(", ", participantNames)
            }

//            val date = message.date.formatDateOrTime(this, true, true)
            val searchResult = SearchModel(message.id, recipient, message.body, message.date.toString(), message.threadId, message.senderPhotoUri,phoneNumber)
            searchResults.add(searchResult)
        }

        runOnUiThread {
            binding.recyclerSearchContact.beVisibleIf(searchResults.isNotEmpty())
            binding.tvNoData1.beVisibleIf(searchResults.isEmpty())
            binding.ivThumbNodata.beVisibleIf(searchResults.isEmpty())

            val currAdapter = binding.recyclerSearchContact.adapter
            if (currAdapter == null) {
                searchResultsAds.clear()
                for (i in searchResults.indices) {

                    searchResultsAds.add(searchResults[i])
                }
                SearchAdapter(this, searchResultsAds, binding.recyclerSearchContact, searchedText) {
                    hideKeyboard()
                    val intentApp = Intent(this, MessagesActivity::class.java)
                    intentApp.putExtra(THREAD_ID, (it as SearchModel).threadId)
                    intentApp.putExtra(THREAD_TITLE, it.title)
                    intentApp.putExtra(THREAD_NUMBER, it.phoneNumber)
                    intentApp.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                    startActivity(intentApp)
                }.apply {
                    binding.recyclerSearchContact.adapter = this
                }
            } else {
                searchResultsAds.clear()
                for (i in searchResults.indices) {
                    searchResultsAds.add(searchResults[i])
                }
                (currAdapter as SearchAdapter).updateItems(searchResultsAds, searchedText)
            }
        }
    }
}
