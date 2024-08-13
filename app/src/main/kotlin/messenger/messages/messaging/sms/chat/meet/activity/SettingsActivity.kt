package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.view.Window
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope

import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.dialogs.RadioButtonsDialog
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.model.RadioModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.ads.AdsManager
import messenger.messages.messaging.sms.chat.meet.databinding.ActivitySettingsBinding
import java.util.Objects


class SettingsActivity : BaseHomeActivity() {
    private var blockedNumbersAtPause = -1
    lateinit var dialog: ProgressDialog

    private lateinit var binding: ActivitySettingsBinding
    var appTopToolbar: Toolbar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.txtHeading.text = getString(R.string.app_setting)
        setDialog(this)
        loadBannerAd()
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadBannerAd() {
        AdsManager.showSmallBannerAds(binding.mBannerAds, this)
    }

    private fun setDialog(context: Context, theme: Int = R.style.AppTheme_Dark_Dialog) {
        dialog = ProgressDialog(context, theme)
        dialog.isIndeterminate = true
        Objects.requireNonNull<Window>(dialog.window)
            .setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
    }


    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        setUpSyncMessages()
        setupCustomizeNotifications()
        setupTheme()
        setupUseSystemFont()
        setupFontSize()
        setupLockScreenVisibility()
        setupMMSFileSizeLimit()
        binding.txtVersion.text = "Version ${packageManager.getPackageInfo(packageName, 0).versionName}"
        if (blockedNumbersAtPause != -1 && blockedNumbersAtPause != getBlockedNumbers().hashCode()) {
            Log.d("TAG_REFRESH", "onResume: Setting")
            MainAppClass.getAllMessagesFromDb {
                refreshMessages()
            }
        }
    }

    private fun setUpSyncMessages() {
        binding.rlSyncDatabase.setOnClickListener {
            dialog.show()
            syncAllSmsToNativeDatabase()
        }
    }

    override fun onPause() {
        super.onPause()
        blockedNumbersAtPause = getBlockedNumbers().hashCode()
    }


    private fun setupCustomizeNotifications() {
        binding.rlCustomNotifications.beVisibleIf(isOreoPlus())

        if (binding.rlCustomNotifications.isGone()) {
            binding.rlLockScreen.background = resources.getDrawable(R.drawable.ripple_all_corners, theme)
        }

        binding.rlCustomNotifications.setOnClickListener {
            gotoSystenNotificationsSetting()
        }
    }


    fun gotoSystenNotificationsSetting() {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(this)
        }
    }


    private fun setupTheme() {

        binding.rlTheme.setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
        }
    }


    private fun setupUseSystemFont() {
        binding.switchSystemFont.isChecked = mPref.isUseSystemFont
        binding.rlUseSystemFont.setOnClickListener {
            binding.switchSystemFont.toggle()
            mPref.isUseSystemFont = binding.switchSystemFont.isChecked
            Log.e("TAG_REFRESH", " setupChangeDateTimeFormat")
            MainAppClass.getAllMessagesFromDb {
                refreshMessages()
            }
        }
    }

    private fun setupFontSize() {
        binding.rlFontSize.setOnClickListener {
            startActivity(Intent(this, FontSizeActivity::class.java))
        }
        binding.txtFontSize.text = getFontSizeText()
    }


    private fun setupLockScreenVisibility() {
        binding.tvLockScreenDesc.text = getLockScreenVisibilityText()
        binding.rlLockScreen.setOnClickListener {
            val items = arrayListOf(
                RadioModel(LOCK_SCREEN_SENDER_MESSAGE, getString(R.string.sender_and_message)),
                RadioModel(LOCK_SCREEN_SENDER, getString(R.string.sender_only)),
                RadioModel(LOCK_SCREEN_NOTHING, getString(R.string.nothing)),
            )

            RadioButtonsDialog(this@SettingsActivity, items, config.lockScreenVisibilitySetting) {
                config.lockScreenVisibilitySetting = it as Int
                binding.tvLockScreenDesc.text = getLockScreenVisibilityText()
            }
        }
    }

    private fun getLockScreenVisibilityText() = getString(
        when (config.lockScreenVisibilitySetting) {
            LOCK_SCREEN_SENDER_MESSAGE -> R.string.sender_and_message
            LOCK_SCREEN_SENDER -> R.string.sender_only
            else -> R.string.nothing
        }
    )

    private fun setupMMSFileSizeLimit() {
        binding.tvMMSFileSizeDesc.text = getMMSFileLimitText()
        binding.rlMMSFileSize.setOnClickListener {
            startActivity(Intent(this, AutoCompressActivity::class.java))
        }
    }

    private fun getMMSFileLimitText() = getString(
        when (config.mmsFileSizeLimit) {
            FILE_SIZE_AUTOMATIC -> R.string.app_automatic
            FILE_SIZE_100_KB -> R.string.app_1000_kb
            FILE_SIZE_200_KB -> R.string.app_200_kb
            FILE_SIZE_300_KB -> R.string.app_300_kb
            FILE_SIZE_600_KB -> R.string.app_600_kb
            FILE_SIZE_1_MB -> R.string.app_1000_kb
            FILE_SIZE_2_MB -> R.string.app_2000_kb
            else -> R.string.app_no_compression
        }
    )

    @SuppressLint("Range")
    private fun syncAllSmsToNativeDatabase() {


        Log.d("TAG_MESSAGE", "getNewConversations: ")
        CoroutineScope(Dispatchers.IO).launch {

            val privateCursor = getMyContactsCursor(false, true)?.loadInBackground()
            ensureBackgroundThread {
                val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(this@SettingsActivity, privateCursor)
                val conversations = getConversations(privateContacts = privateContacts)
                val cachedConversations = try {
                    conversationsDB.getAllList().toMutableList() as ArrayList<ConversationSmsModel>
                } catch (e: Exception) {
                    ArrayList()
                }

                conversations.forEach { clonedConversation ->
                    if (!cachedConversations.map { it.threadId }.contains(clonedConversation.threadId)) {
                        conversationsDB.insertOrUpdateMessage(clonedConversation)
                        cachedConversations.add(clonedConversation)
                    }
                }

//                cachedConversations.forEach { cachedConversation ->
//                    if (!conversations.map { it.threadId }.contains(cachedConversation.threadId)) {
//                        conversationsDB.deleteThreadIdMessage(cachedConversation.threadId)
//                    }
//                }

                cachedConversations.forEach { cachedConversation ->
                    val conv = conversations.firstOrNull { it.threadId == cachedConversation.threadId && it.toString() != cachedConversation.toString() }
                    if (conv != null) {
                        conversationsDB.insertOrUpdateMessage(conv)
                    }
                }

                if (config.appRunCount == 1) {
                    conversations.map { it.threadId }.forEach { threadId ->
                        val messages = getMessages(threadId)
                        messages.forEach { currentMessages ->
                            messagesDB.insertAddMessages(currentMessages)
                        }
                    }

                    config.appRunCount++
                    MainAppClass.getAllMessagesFromDb {
                        refreshMessages()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                Toast.makeText(this@SettingsActivity, "Sync successfully", Toast.LENGTH_SHORT).show()
            }

        }
    }

}
