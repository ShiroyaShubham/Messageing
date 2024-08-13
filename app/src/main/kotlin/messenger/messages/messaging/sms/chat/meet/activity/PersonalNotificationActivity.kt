package messenger.messages.messaging.sms.chat.meet.activity

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityPersonalNotificationBinding
import messenger.messages.messaging.sms.chat.meet.extensions.beVisibleIf
import messenger.messages.messaging.sms.chat.meet.model.NotificationPreviewModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.R

class PersonalNotificationActivity : BaseHomeActivity() {
    private var _binding: ActivityPersonalNotificationBinding? = null
    private var mTitle = ""
    private var mNumber = ""
    private var isWakeup = false
    private var previewType = ""
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        _binding = ActivityPersonalNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        bindHandlers()
    }

    private fun setupWakeupSleep() {
        previewType = getString(R.string.app_show_name_and_message)
        CoroutineScope(Dispatchers.IO).launch {
            notificationPreViewDao.getUserNotificationPreview().forEach {
                Log.d("TAG_PREVIEW", "setupRecyclerview: ${it.name} ${it.previewType} ${it.isWakeup} ")
                if (it.number == mNumber) {
                    previewType = it.previewType
                    isWakeup = it.isWakeup == 1
                }
            }
            withContext(Dispatchers.Main) {
                binding.tvNotificationPreview.text = previewType
                binding.switchWakeScreen.isChecked = isWakeup
            }
        }
    }

    private fun bindHandlers() {
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }

        binding.llCustomiseNotification.setOnClickListener {
            gotoSystemNotificationsSetting()
        }

        binding.llNotificationPreview.setOnClickListener {
            val intent = Intent(this, NotificationPreviewActivity::class.java)
            intent.putExtra(THREAD_TITLE, mTitle)
            intent.putExtra(THREAD_NUMBER, mNumber)
            intent.putExtra(IS_WAKEUP, binding.switchWakeScreen.isChecked)
            startActivity(intent)
        }

        binding.llWakeScreen.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    binding.switchWakeScreen.toggle()
                    isWakeup = binding.switchWakeScreen.isChecked
                }
                notificationPreViewDao.insertUserPreview(NotificationPreviewModel(mTitle, mNumber, previewType, if (isWakeup) 1 else 0))
            }
        }
    }

    private fun initData() {
        mTitle = intent.getStringExtra(THREAD_TITLE).toString()
        mNumber = intent.getStringExtra(THREAD_NUMBER).toString()
        binding.header.txtHeading.text = mTitle
        binding.llCustomiseNotification.beVisibleIf(isOreoPlus())
    }

    private fun gotoSystemNotificationsSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupWakeupSleep()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
