package messenger.messages.messaging.sms.chat.meet.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import messenger.messages.messaging.sms.chat.meet.adapters.AutoCompressAdapter
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityAutoCompressBinding
import messenger.messages.messaging.sms.chat.meet.model.AutoCompressModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass

class AutoCompressActivity : BaseHomeActivity() {
    private var _binding: ActivityAutoCompressBinding? = null
    private val binding get() = _binding!!
    private lateinit var autoCompressAdapter: AutoCompressAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAutoCompressBinding.inflate(layoutInflater)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        setContentView(binding.root)
        bindHandlers()
        setupRecyclerView()
        if (!PrefClass.isProUser){
        showBannerAds(findViewById(R.id.mBannerAdsContainer))
        }else{
            findViewById<ViewGroup>(R.id.mBannerAdsContainer)?.visibility = View.GONE
        }
    }

    private fun bindHandlers() {
        binding.header.txtHeading.text = getString(R.string.app_auto_compress)
        binding.header.imgSave.setImageResource(R.drawable.ic_tick)
        binding.header.imgSave.isVisible = true
        binding.header.imgSave.setOnClickListener {
            if (autoCompressAdapter.getSelectedPos() != -1) {
                config.mmsFileSizeLimit = getAutoCompressSize()[autoCompressAdapter.getSelectedPos()].size
                finish()
            }else{
                Toast.makeText(this, "Please select size", Toast.LENGTH_SHORT).show()
            }
        }
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        autoCompressAdapter = AutoCompressAdapter(getAutoCompressSize(), config.mmsFileSizeLimit)
        binding.rvAutoCompress.apply {
            adapter = autoCompressAdapter
        }
    }

    private fun getAutoCompressSize() = listOf(
        AutoCompressModel(getString(R.string.app_automatic), FILE_SIZE_AUTOMATIC),
        AutoCompressModel(getString(R.string.app_100_kb), FILE_SIZE_100_KB),
        AutoCompressModel(getString(R.string.app_200_kb), FILE_SIZE_200_KB),
        AutoCompressModel(getString(R.string.app_300_kb), FILE_SIZE_300_KB),
        AutoCompressModel(getString(R.string.app_600_kb), FILE_SIZE_600_KB),
        AutoCompressModel(getString(R.string.app_1000_kb), FILE_SIZE_1_MB),
        AutoCompressModel(getString(R.string.app_2000_kb), FILE_SIZE_2_MB),
        AutoCompressModel(getString(R.string.app_no_compression), FILE_SIZE_NONE),
    )

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
