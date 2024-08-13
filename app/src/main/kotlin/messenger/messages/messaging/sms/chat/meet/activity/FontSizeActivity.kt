package messenger.messages.messaging.sms.chat.meet.activity

import android.os.Bundle
import androidx.core.view.isVisible
import messenger.messages.messaging.sms.chat.meet.adapters.FontSizeAdapter
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityFontSizeBinding
import messenger.messages.messaging.sms.chat.meet.model.ImageWithTextModel
import messenger.messages.messaging.sms.chat.meet.utils.config
import messenger.messages.messaging.sms.chat.meet.R

class FontSizeActivity : BaseHomeActivity() {
    private var _binding: ActivityFontSizeBinding? = null
    private val binding get() = _binding!!
    private lateinit var fontSizeAdapter: FontSizeAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        _binding = ActivityFontSizeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindHandlers()
        setupRecyclerView()
    }

    private fun bindHandlers() {
        binding.header.txtHeading.text = getString(R.string.app_font_size)
        binding.header.imgSave.setImageResource(R.drawable.ic_tick)
        binding.header.imgSave.isVisible = true
        binding.header.imgSave.setOnClickListener {
            config.fontSize = fontSizeAdapter.getSelectedPos()
            finish()
        }
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        fontSizeAdapter = FontSizeAdapter(getFontSizes(), config.fontSize)
        binding.rvFontSize.apply {
            adapter = fontSizeAdapter
        }
    }

    private fun getFontSizes() = listOf(
        ImageWithTextModel(getString(R.string.small), R.drawable.ic_small),
        ImageWithTextModel(getString(R.string.app_normal), R.drawable.ic_normal),
        ImageWithTextModel(getString(R.string.large), R.drawable.ic_use_system_font),
        ImageWithTextModel(getString(R.string.extra_large), R.drawable.ic_font_size)
    )

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
