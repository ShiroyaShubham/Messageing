package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.adsdk.plugin.AdsUtils
import com.adsdk.plugin.AppOpenAdsManager
import com.adsdk.plugin.model.AdsConfig
import com.android.billingclient.api.*
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import messenger.messages.messaging.sms.chat.meet.BuildConfig
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.extensions.mPref
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.send_message.Utils.isInternetAvailable
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.utils.DATE_FORMAT_SIX
import messenger.messages.messaging.sms.chat.meet.utils.IS_LOGIN
import messenger.messages.messaging.sms.chat.meet.utils.LANG_CODE
import messenger.messages.messaging.sms.chat.meet.utils.SharedPrefrenceClass

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseHomeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguage(this)
        setContentView(R.layout.activity_splash)
        SharedPrefrenceClass.getInstance()!!.setBoolean("isSplash", true)
        mPref.dateFormat = DATE_FORMAT_SIX

        val imgIntro: ImageView = findViewById(R.id.imgIntro)
        val imgIntroDarkMode: ImageView = findViewById(R.id.imgIntroDarkMode)
        if (Utils.isDarkMode(this)) {
            imgIntro.isInvisible = true
            imgIntroDarkMode.isVisible = true
        } else {
            imgIntroDarkMode.isInvisible = true
            imgIntro.isVisible = true
        }

        MainAppClass.getConversationDataFromDB()
        MainAppClass.getAllMessagesFromDb {

        }

        handler()

    }

    private fun setLanguage(context: Context) {
        Utils.setLocale(context, SharedPrefrenceClass.getInstance()?.getString(LANG_CODE, "en"))
    }




    private fun handler() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (SharedPrefrenceClass.getInstance()?.getBoolean(IS_LOGIN, false) == true) {
                val intent = Intent(this@SplashActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this@SplashActivity, LanguageActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 3000)
    }

}
