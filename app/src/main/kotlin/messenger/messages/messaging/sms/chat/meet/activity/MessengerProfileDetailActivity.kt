package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityMessengerProfileDetailBinding
import messenger.messages.messaging.sms.chat.meet.dialogs.AlertDialogCustom
import messenger.messages.messaging.sms.chat.meet.model.ArchivedModel
import messenger.messages.messaging.sms.chat.meet.model.ContactsModel
import messenger.messages.messaging.sms.chat.meet.model.MessagesModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.model.BlockContactModel
import org.greenrobot.eventbus.EventBus

class MessengerProfileDetailActivity : BaseHomeActivity() {
    private var _binding: ActivityMessengerProfileDetailBinding? = null
    private val binding get() = _binding!!
    private var mTitle: String? = ""
    private var mNumber: String? = ""
    private var threadId = 0L
    private var participants = ArrayList<ContactsModel>()
    private var messages = ArrayList<MessagesModel>()
    private var photoUri = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMessengerProfileDetailBinding.inflate(layoutInflater)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        setContentView(binding.root)
        initData()
        bindHandlers()
    }

    @SuppressLint("Range")
    private fun bindHandlers() {
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
        binding.imgAddUser.setOnClickListener {

        }
        binding.imgColorPallete.setOnClickListener {

        }
        binding.txtBlock.setOnClickListener {
            blockNumber()
        }
        binding.txtDeleteConversation.setOnClickListener {
            askConfirmDelete()
        }
        binding.imgAddUser.setOnClickListener {
            openEditContact()
        }
        binding.txtNotification.setOnClickListener {
            val intent = Intent(this, PersonalNotificationActivity::class.java)
            intent.putExtra(THREAD_TITLE, mTitle)
            intent.putExtra(THREAD_NUMBER, mNumber)
            startActivity(intent)
        }
        binding.txtArchived.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("TAG_ARCHIVED_CONVERSATION", "bindHandlers: ${ArchivedModel(mNumber!!, threadId, mTitle!!)}")
                archivedMessageDao.insertArchivedUser(ArchivedModel(mNumber!!, threadId, mTitle!!))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MessengerProfileDetailActivity, "Archived successfully", Toast.LENGTH_SHORT).show()
                    binding.txtArchived.isVisible = false
                    binding.txtUnArchived.isVisible = true
                    Log.d("TAG_REFRESH", "bindHandlers: Messenger")
                    MainAppClass.getConversationDataFromDB()
                    EventBus.getDefault().post(ArchivedModel(mNumber!!, threadId, mTitle!!))
                    refreshMessages()
                }
            }
        }
        binding.txtUnArchived.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                archivedMessageDao.deleteArchivedUser(threadId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MessengerProfileDetailActivity, "UnArchived successfully", Toast.LENGTH_SHORT).show()
                    binding.txtUnArchived.isVisible = false
                    binding.txtArchived.isVisible = true
                    Log.d("TAG_REFRESH", "bindHandlers: unarchived")
                    MainAppClass.getConversationDataFromDB()
                    EventBus.getDefault().post(ArchivedModel(mNumber!!, threadId, mTitle!!))
                    refreshMessages()
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun openEditContact() {
        val contactUri: Uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mNumber))
        val cursor = contentResolver.query(contactUri, null, null, null, null)
        Log.d("TAG_CURSOR", "openEditContact: $cursor")
        cursor?.let {
            if (it.moveToFirst()) {
                val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))

                // Open the edit screen for the contact
                val intent = Intent(Intent.ACTION_EDIT)
                val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
                intent.data = contactUri
                startActivity(intent)
            } else {
                val intent = Intent(Intent.ACTION_INSERT)
                intent.type = ContactsContract.Contacts.CONTENT_TYPE
                intent.putExtra(ContactsContract.Intents.Insert.NAME, mTitle)
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, mNumber)
                startActivity(intent)
            }
            it.close()
        }
    }

    private fun initData() {
        binding.header.txtHeading.text = getString(R.string.app_detail)
        mTitle = intent.getStringExtra(THREAD_TITLE)
        mNumber = intent.getStringExtra(THREAD_NUMBER)
        threadId = intent.getLongExtra(THREAD_ID, 0L)
        binding.txtName.text = mTitle
        binding.txtPhoneNo.text = mNumber
        setupParticipants()
        participants.forEach {
            Log.d("TAG_TITLE", "initData: ${mNumber} $mTitle")
            if (it.name == mTitle) {
                Log.d("TAG_TITLE", "initData: ${it.photoUri}")
                photoUri = it.photoUri
            }
        }
        if (photoUri.isNotEmpty()) {
            binding.imgPlaceHolder.isVisible = false
            Glide.with(this).load(photoUri).into(binding.imgProfile)
        }

        CoroutineScope(Dispatchers.IO).launch {
            archivedMessageDao.getArchivedUser().forEach {
                if (it.threadId == threadId) {
                    withContext(Dispatchers.Main) {
                        binding.txtUnArchived.isVisible = true
                        binding.txtArchived.isVisible = false
                    }
                }
            }
        }
    }


    private fun setupParticipants() {
        messages = try {
            messagesDB.getThreadMessages(threadId).toMutableList() as ArrayList<MessagesModel>
        } catch (e: Exception) {
            ArrayList()
        }
        if (participants.isEmpty()) {
            participants = if (messages.isEmpty()) {
                val intentNumbers = getPhoneNumbersFromIntent()
                val participants = getThreadParticipants(threadId, null)
                fixParticipantNumbers(participants, intentNumbers)
            } else {
                messages.first().participants
            }
        }
    }


    private fun getPhoneNumbersFromIntent(): ArrayList<String> {

        val numbers = ArrayList<String>()

        if (mNumber != null) {
            if (mNumber!!.startsWith('[') && mNumber!!.endsWith(']')) {
                val type = object : TypeToken<List<String>>() {}.type
                numbers.addAll(Gson().fromJson(mNumber!!, type))
            } else {
                numbers.add(mNumber!!)
            }
        }
        return numbers
    }

    private fun fixParticipantNumbers(participants: ArrayList<ContactsModel>, properNumbers: ArrayList<String>): ArrayList<ContactsModel> {
        for (number in properNumbers) {
            for (participant in participants) {
                participant.phoneNumbers = participant.phoneNumbers.map {
                    val numberWithoutPlus = number.replace("+", "")
                    if (numberWithoutPlus == it.trim()) {
                        if (participant.name == it) {
                            participant.name = number
                        }
                        number
                    } else {
                        it
                    }
                } as ArrayList<String>
            }
        }

        return participants
    }

    private fun blockNumber() {
        val numbers = ArrayList<String>()
        participants.forEach {
            it.phoneNumbers.forEach {
                numbers.add(it)
            }
        }

        val numbersString = TextUtils.join(", ", numbers)
        val question = String.format(resources.getString(R.string.app_block_confirmation), numbersString)
        val title = getString(R.string.app_block)

        AlertDialogCustom(this, title, question) {
            ensureBackgroundThread {
//                numbers.forEach {
//                    addBlockedNumber(it)
//                }
                val snippet = intent.getStringExtra(SNIPPET)
                val date = intent.getLongExtra(DATE, 0L)
                Log.d("TAG_BLOCK", "blockNumber: $snippet $date")

                Log.e("Event: ", "SMS_Dialog_Alert")
                blockContactDao.insertBlockNo(
                    BlockContactModel(
                        threadID = threadId,
                        number = mNumber!!,
                        name = mTitle!!,
                        msg = snippet!!,
                        dateTime = date
                    )
                )
                Log.d("TAG_THREAD_ID", "blockNumber: $threadId")
                archivedMessageDao.deleteArchivedUser(threadId)
                conversationsDB.deleteThreadIdMessage(threadId)
                MainAppClass.getConversationDataFromDB()
                val intent = Intent(this, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun askConfirmDelete() {
        val title = getString(R.string.app_delete)
        val message = getString(R.string.app_delete_message)
        AlertDialogCustom(this, title, message) {
            ensureBackgroundThread {
                deleteConversation(threadId)
                archivedMessageDao.deleteArchivedUser(threadId)
                runOnUiThread {
                    Log.e("TAG_REFRESH", "askConfirmDelete")
                    MainAppClass.getAllMessagesFromDb {
                        refreshMessages()
                    }
                    MainAppClass.getConversationDataFromDB()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
