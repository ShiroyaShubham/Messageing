package messenger.messages.messaging.sms.chat.meet.activity

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.utils.THREAD_TITLE
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityCopingTextBinding

class CopingTextActivity : BaseHomeActivity() {
    private lateinit var binding:ActivityCopingTextBinding
    var appTopToolbar: Toolbar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCopingTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appTopToolbar = findViewById(R.id.appTopToolbar)
        setSupportActionBar(appTopToolbar)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        appTopToolbar?.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.icon_back))


        val msgBody = intent.getStringExtra(THREAD_TITLE)
        binding.tvSelectMessageBody.setText(msgBody)
        binding.tvSelectMessageBody.setLinkTextColor(resources.getColor(R.color.text_link))

    }
}
