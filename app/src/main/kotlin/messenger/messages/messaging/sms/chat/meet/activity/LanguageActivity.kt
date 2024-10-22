package messenger.messages.messaging.sms.chat.meet.activity

import android.Manifest
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.adapters.LanguageAdapter
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityLanguageBinding
import messenger.messages.messaging.sms.chat.meet.model.LanguageModel
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.utils.*
import java.util.ArrayList

class LanguageActivity : BaseActivity() {
    private var _binding: ActivityLanguageBinding? = null
    private val binding get() = _binding!!
    private lateinit var languageAdapter: LanguageAdapter
    private val MAKE_DEFAULT_APP_REQUEST = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLanguageBinding.inflate(layoutInflater)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        setContentView(binding.root)
        binding.header.tvSave.isVisible = true
        if (!PrefClass.isProUser){
        showBannerAds(findViewById(R.id.mBannerAdsContainer))
        }else{
            findViewById<ViewGroup>(R.id.mBannerAdsContainer)?.visibility = View.GONE
        }
        setupRecyclerView()
        bindHandlers()

        Log.e("TAG", "onCreate:>>>>>>> "+SharedPrefrenceClass.getInstance()?.getBoolean(IS_LOGIN, false) )
        if (SharedPrefrenceClass.getInstance()?.getBoolean(IS_LOGIN, false)==true)
        {
            binding.header.imgBack.visibility=View.VISIBLE
        }else{
            binding.header.imgBack.visibility=View.GONE
        }
    }


    private fun bindHandlers() {
        binding.header.tvSave.setOnClickListener {
            SharedPrefrenceClass.getInstance()?.setInt(LANG_POS, languageAdapter.getSelectedLangPos())
            SharedPrefrenceClass.getInstance()?.setString(LANG_CODE, getLanguages()[languageAdapter.getSelectedLangPos()].code)
            SharedPrefrenceClass.getInstance()?.setBoolean(IS_LOGIN, true)
            Utils.setLocale(this, SharedPrefrenceClass.getInstance()?.getString(LANG_CODE, "en"))
            startActivity(
                Intent(this, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
        binding.header.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val langList = getLanguages()
        languageAdapter = LanguageAdapter(langList) {}
        binding.rvLanguage.apply {
            adapter = languageAdapter
        }
    }


    private fun getLanguages(): List<LanguageModel> {
        return listOf(
            LanguageModel("English", "en"),
            LanguageModel("বাংলা (Bengali)", "bn"),
            LanguageModel("Français (French)", "fr"),
            LanguageModel("Deutsch (German)", "de"),
            LanguageModel("bahasa Indonesia (Indonesian)", "id"),
            LanguageModel("Italiano (Italian)", "it"),
            LanguageModel("日本語 (Japanese)", "ja"),
            LanguageModel("Polski (Polish)", "pl"),
            LanguageModel("Português (Portuguese)", "pt"),
            LanguageModel("Română (Romansh)", "rm"),
            LanguageModel("Español (Spanish)", "es"),
            LanguageModel("Kiswahili (Swahili)", "sw"),
            LanguageModel("한국인 (Korean)", "ko"),
            LanguageModel("اردو (Urdu)", "ur"),
            LanguageModel(")Arabic( عربي", "ar"),
            LanguageModel("中国人(Chinese)", "zh")
        )
    }


    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            checkNotificationPermission()
                        }
                    } else {
                        showPermissionDialog()
                    }
                }
            } else {
                showPermissionDialog()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            Utility.showSnackBar(
                this,
                getString(R.string.txt_please_grant_notification_permission_from_app_settings),
                this.findViewById<View>(android.R.id.content).rootView,
            )
        } else {
            this.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gotoNotificationScreen()
        }
    }

    private fun gotoNotificationScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun getAppIconIDs(): ArrayList<Int> {
        return arrayListOf(
            R.mipmap.ic_launcher
        )
    }

    override fun getAppLauncherName(): String {
        return intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""
    }

    private fun showPermissionDialog() {
        val mDialog = Dialog(this@LanguageActivity)
        mDialog.setContentView(R.layout.dialog_permission)
        mDialog.setCanceledOnTouchOutside(false)
        mDialog.setCancelable(false)
        mDialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mDialog.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
        mDialog.show()

        (mDialog.findViewById<View>(R.id.tvSetAsDefault) as TextView).setOnClickListener {
            if (!this?.isFinishing!! && mDialog.isShowing) {
                mDialog.dismiss()
            }
            getPermission()
        }
    }


    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = this@LanguageActivity.getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {

            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this@LanguageActivity) == this.packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, this.packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
