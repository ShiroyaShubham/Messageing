package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import messenger.messages.messaging.sms.chat.meet.adapters.NotificationPreviewAdapter
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityNotificationPreviewBinding
import messenger.messages.messaging.sms.chat.meet.model.AutoCompressModel
import messenger.messages.messaging.sms.chat.meet.model.NotificationPreviewModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.R
import java.util.ArrayList

class NotificationPreviewActivity : BaseHomeActivity() {
    private var _binding: ActivityNotificationPreviewBinding? = null
    private val binding get() = _binding!!
    private var mTitle = ""
    private var mNumber = ""
    private lateinit var notificationPreviewAdapter: NotificationPreviewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityNotificationPreviewBinding.inflate(layoutInflater)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        setContentView(binding.root)
        setupRecyclerview()
        bindHandlers()
        initData()
    }

    private fun initData() {
        binding.header.txtHeading.text = getString(R.string.app_notification_preview)
    }

    private fun bindHandlers() {
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerview() {
        mTitle = intent.getStringExtra(THREAD_TITLE).toString()
        mNumber = intent.getStringExtra(THREAD_NUMBER).toString()
        val isWakeUp = intent.getBooleanExtra(IS_WAKEUP, true)
        var previewName = getString(R.string.app_show_name_and_message)
        CoroutineScope(Dispatchers.IO).launch {
            notificationPreViewDao.getUserNotificationPreview().forEach {
                Log.d("TAG_PREVIEW", "setupRecyclerview: ${it.name} ${it.previewType}")
                if (it.number == mNumber) {
                    previewName = it.previewType
                }
            }

            withContext(Dispatchers.Main) {
                notificationPreviewAdapter = NotificationPreviewAdapter(getAutoCompressSize(), previewName) {
                    CoroutineScope(Dispatchers.IO).launch {
                        notificationPreViewDao.insertUserPreview(NotificationPreviewModel(mTitle, mNumber, it, if (isWakeUp) 1 else 0))
                        withContext(Dispatchers.Main){
                            notificationPreviewAdapter.notifyDataSetChanged()
                        }
                    }
                }

                binding.rvNotificationPreview.apply {
                    adapter = notificationPreviewAdapter

                }
            }
        }
    }

    private fun getAutoCompressSize() = listOf(
        AutoCompressModel(getString(R.string.app_show_name_and_message), 0),
        AutoCompressModel(getString(R.string.app_show_name), 0),
        AutoCompressModel(getString(R.string.app_hide_contents), 0),
    )


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
