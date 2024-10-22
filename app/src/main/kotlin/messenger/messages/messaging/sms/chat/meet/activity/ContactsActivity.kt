package messenger.messages.messaging.sms.chat.meet.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import com.google.gson.Gson
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.adapters.PhoneContactsAdapter
import messenger.messages.messaging.sms.chat.meet.utils.getSuggestedContacts
import messenger.messages.messaging.sms.chat.meet.utils.getThreadId
import messenger.messages.messaging.sms.chat.meet.dialogs.RadioButtonsDialog
import messenger.messages.messaging.sms.chat.meet.model.RadioModel
import messenger.messages.messaging.sms.chat.meet.model.ContactsModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityContactsBinding
import org.greenrobot.eventbus.MainThreadSupport
import java.net.URLDecoder
import java.util.*

class ContactsActivity : BaseHomeActivity() {
    private lateinit var binding: ActivityContactsBinding
    private var mAllContacts = ArrayList<ContactsModel>()
    private var mPrivateContacts = ArrayList<ContactsModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.txtHeading.text = getString(R.string.app_new_conversation)

        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }

        handlePermission(PERMISSION_READ_CONTACTS) {
            initContacts()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvNoContact2.underlineText()
    }

    private fun initContacts() {
        if (isThirdPartyIntent()) {
            return
        }

        fetchContacts()


        binding.tvSearchText.setOnClickListener {
            binding.llHeader.performClick()
        }

        binding.llHeader.setOnClickListener {
            val intent = Intent(this, ContactsActivity2::class.java)
//            val transition_1 = Pair.create<View, String>(binding.llEditText, getString(R.string.txt_transition_1))
//            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, transition_1)
//            startActivity(intent, options.toBundle())
            startActivity(intent)
            overridePendingTransition(0, 0);
        }

        binding.tvNoContact2.setOnClickListener {
            handlePermission(PERMISSION_READ_CONTACTS) {
                if (it) {
                    fetchContacts()
                }
            }
        }

        binding.fastScrollerViewContact.textColor = getColorStateList()
        binding.fastScrollerThumbViewContacts.setupWithFastScroller(binding.fastScrollerViewContact)
        binding.fastScrollerThumbViewContacts?.textColor = resources.getColor(android.R.color.white)
        binding.fastScrollerThumbViewContacts?.textColor = resources.getColor(android.R.color.white)
        binding.fastScrollerThumbViewContacts?.thumbColor = getColorStateList()
    }

    fun getColorStateList(): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_pressed)
        )
        val colors = intArrayOf(
            resources.getColor(R.color.only_blue),
            resources.getColor(R.color.only_blue),
            resources.getColor(R.color.only_blue),
            resources.getColor(R.color.only_blue)
        )
        return ColorStateList(states, colors)
    }

    private fun isThirdPartyIntent(): Boolean {
        if ((intent.action == Intent.ACTION_SENDTO || intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW) && intent.dataString != null) {
            val number = intent.dataString!!.removePrefix("sms:").removePrefix("smsto:").removePrefix("mms").removePrefix("mmsto:").replace("+", "%2b").trim()
            launchThreadActivity(URLDecoder.decode(number), "")
            finish()
            return true
        }
        return false
    }

    private fun fetchContacts() {
        fillSuggestedContacts {
            CoroutineScope(Dispatchers.IO).launch {
                mAllContacts = MainAppClass.mAllContacts.ifEmpty {
                    SimpleContactsHelperUtils(this@ContactsActivity).getAvailableContacts(false)
                }

                if (mPrivateContacts.isNotEmpty()) {
                    mAllContacts.addAll(mPrivateContacts)
                    mAllContacts.sort()
                }

                Log.d("TAG_CONTACT_SIZE", "fetchContacts: All contact ${mAllContacts.size}")
                val archiveList = archivedMessageDao.getArchivedUser()
                val blockList = blockContactDao.getAllBlockNo()
                val contactRemoveFromArchived = removeArchiveFromContacts(mAllContacts, archiveList)
                val contactRemoveFromBlock = removeBlockFromContacts(contactRemoveFromArchived, blockList)
                Log.d("TAG_CONTACT_SIZE", "fetchContacts: $contactRemoveFromBlock")
                withContext(Dispatchers.Main) {
                    setupAdapter(contactRemoveFromBlock)
                }
            }

        }
    }

    private fun setupAdapter(contacts: ArrayList<ContactsModel>) {
        val hasContacts = contacts.isNotEmpty()
        binding.recyclerViewContacts.beVisibleIf(hasContacts)
        binding.tvNoContact.beVisibleIf(!hasContacts)
        binding.ivThumbNoContatc.beVisibleIf(!hasContacts)
        binding.tvNoContact2.beVisibleIf(!hasContacts && !hasPermission(PERMISSION_READ_CONTACTS))

        if (!hasContacts) {
            val placeholderText = if (hasPermission(PERMISSION_READ_CONTACTS)) R.string.no_contacts_found else R.string.no_access_to_contacts
            binding.tvNoContact.text = getString(placeholderText)
        }

        val currAdapter = binding.recyclerViewContacts.adapter
        if (currAdapter == null) {
            PhoneContactsAdapter(this, contacts, binding.recyclerViewContacts, null) {
                hideKeyboard()
                val contact = it as ContactsModel
                val phoneNumbers = contact.phoneNumbers
                if (phoneNumbers.size > 1) {
                    val items = ArrayList<RadioModel>()
                    phoneNumbers.forEachIndexed { index, phoneNumber ->
                        items.add(RadioModel(index, phoneNumber, phoneNumber))
                    }

                    RadioButtonsDialog(this, items) {
                        launchThreadActivity(it as String, contact.name)
                    }
                } else {
                    launchThreadActivity(phoneNumbers.first(), contact.name)
                }
            }.apply {
                binding.recyclerViewContacts.adapter = this
            }


        } else {
            (currAdapter as PhoneContactsAdapter).updateContacts(contacts)
        }

        setupLetterFastscroller(contacts)
    }

    private fun fillSuggestedContacts(callback: () -> Unit) {
        val privateCursor = getMyContactsCursor(false, true)?.loadInBackground()
        ensureBackgroundThread {
            mPrivateContacts = MyContactsContentProviderUtils.getSimpleContacts(this, privateCursor)
            val suggestions = getSuggestedContacts(mPrivateContacts)
            runOnUiThread {
                binding.llSuggestionsView.removeAllViews()
                if (suggestions.isEmpty()) {
                    binding.tvSuggestions.beGone()
                    binding.suggestionsHorizontalScroll.beGone()
                } else {
                    binding.tvSuggestions.beVisible()
                    binding.suggestionsHorizontalScroll.beVisible()
                    suggestions.forEach {
                        val contact = it
                        val view = LayoutInflater.from(this).inflate(R.layout.list_raw_suggestions, null, false)

                        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                        val ivThumb = view.findViewById<ImageView>(R.id.ivThumb)
                        val txtFirstLetter = view.findViewById<TextView>(R.id.txtFirstLetter)

                        tvTitle.text = contact.name
                        val firstChar: Char = contact.name[0]
                        if (Utils.isAlphabet(firstChar)) {
                            ivThumb.isVisible = false
                            txtFirstLetter.isVisible = true
                            txtFirstLetter.text = firstChar.toString()
                        } else {
                            ivThumb.isVisible = true
                            txtFirstLetter.isVisible = false
                        }

                        if (!isDestroyed) {
                            SimpleContactsHelperUtils(this@ContactsActivity).loadContactImage(
                                contact.photoUri,
                                ivThumb, contact.name
                            )
                            binding.llSuggestionsView.addView(view)
                            view.setOnClickListener {
                                launchThreadActivity(contact.phoneNumbers.first(), contact.name)
                            }
                        }
                    }
                }
                callback()
            }
        }
    }

    private fun setupLetterFastscroller(contacts: ArrayList<ContactsModel>) {
        binding.fastScrollerViewContact.setupWithRecyclerView(binding.recyclerViewContacts, { position ->
            try {
                val name = contacts[position].name
                val character = if (name.isNotEmpty()) name.substring(0, 1) else ""
                FastScrollItemIndicator.Text(character.toUpperCase(Locale.getDefault()).normalizeString())
            } catch (e: Exception) {
                FastScrollItemIndicator.Text("")
            }
        })
    }

    private fun launchThreadActivity(phoneNumber: String, name: String) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val numbers = phoneNumber.split(";").toSet()
        val number = if (numbers.size == 1) phoneNumber else Gson().toJson(numbers)
        val intentApp = Intent(this, MessagesActivity::class.java)
        intentApp.putExtra(THREAD_ID, getThreadId(numbers))
        intentApp.putExtra(THREAD_TITLE, name)
        intentApp.putExtra(THREAD_TEXT, text)
        intentApp.putExtra(THREAD_NUMBER, number)
        if (intent.action == Intent.ACTION_SEND && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            intentApp.putExtra(THREAD_ATTACHMENT_URI, uri?.toString())
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            intentApp.putExtra(THREAD_ATTACHMENT_URIS, uris)
        }
        startActivity(intentApp)

    }
}
