package messenger.messages.messaging.sms.chat.meet.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import messenger.messages.messaging.sms.chat.meet.adapters.ThemeAdapter
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityThemeBinding
import messenger.messages.messaging.sms.chat.meet.extensions.mPref
import messenger.messages.messaging.sms.chat.meet.model.ImageWithTextModel
import messenger.messages.messaging.sms.chat.meet.R

class ThemeActivity : BaseHomeActivity() {
    private var _binding: ActivityThemeBinding? = null
    private val binding get() = _binding!!
    private lateinit var themeAdapter: ThemeAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        _binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindHandlers()
        setupRecyclerView()
    }

    private fun bindHandlers() {
        binding.header.txtHeading.text = getString(R.string.app_theme)
        binding.header.imgSave.setImageResource(R.drawable.ic_tick)
        binding.header.imgSave.isVisible = true
        binding.header.imgSave.setOnClickListener {
            try {
            val selectedPos = themeAdapter.getSelectedPos()
            mPref.appTheme = selectedPos
            Log.d("TAG_SELECTED", "bindHandlers: $selectedPos")
            when (selectedPos) {
                0 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

                1 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                2 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
                recreate()
//            val intent = Intent(this,HomeActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
//            startActivity(intent)
            finish()
            } catch (e: Exception) {
                Log.e("ThemeSwitchError", "Error switching theme", e)
            }
        }
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        themeAdapter = ThemeAdapter(getThemes(),mPref.appTheme)
        binding.rvTheme.apply {
            adapter = themeAdapter
        }
    }

    private fun getThemes() = listOf(
        ImageWithTextModel(getString(R.string.app_system), R.drawable.ic_system),
        ImageWithTextModel(getString(R.string.app_light_mode), R.drawable.ic_light_mode),
        ImageWithTextModel(getString(R.string.app_dark_mode), R.drawable.ic_dark_mode),
    )

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
