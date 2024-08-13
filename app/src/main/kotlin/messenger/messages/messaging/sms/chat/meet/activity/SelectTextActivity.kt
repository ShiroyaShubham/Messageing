package messenger.messages.messaging.sms.chat.meet.activity

import android.os.Bundle
import messenger.messages.messaging.sms.chat.meet.extensions.copyToClipboard
import messenger.messages.messaging.sms.chat.meet.extensions.shareTextIntent
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.dialogs.AlertDialogCustom
import messenger.messages.messaging.sms.chat.meet.utils.THREAD_TITLE
import messenger.messages.messaging.sms.chat.meet.databinding.ActivitySelectTextBinding


class SelectTextActivity : BaseHomeActivity() {
    private lateinit var binding: ActivitySelectTextBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.whiteColor)
        binding = ActivitySelectTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val msgBody = intent.getStringExtra(THREAD_TITLE)
        binding.tvSelectMessageBody.text = msgBody
//        binding.tvSelectMessageBody.setLinkTextColor(resources.getColor(R.color.text_link))




        val bitmapLocal = bitmapFromResourceApp(
            resources, R.drawable.bg_text_seleted, 500, 500
        )
        binding.ivBanner.setImageBitmap(bitmapLocal)



        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        binding.llCopy.setOnClickListener {
            copyToClipboard(msgBody!!)
        }

        binding.llShare.setOnClickListener {
            shareTextIntent(msgBody!!)
        }

        binding.llDelete.setOnClickListener {
            askConfirmDelete()
        }


    }


    private fun askConfirmDelete() {

        AlertDialogCustom(this,getString(R.string.delete),getString(R.string.app_delete_message)) {
            setResult(RESULT_OK)
            finish()
        }
    }
}
