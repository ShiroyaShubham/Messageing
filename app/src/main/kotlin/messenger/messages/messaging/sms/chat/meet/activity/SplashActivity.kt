package messenger.messages.messaging.sms.chat.meet.activity

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.google.gson.Gson
import kotlinx.coroutines.launch
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.ads.AppOpenManager
import messenger.messages.messaging.sms.chat.meet.utils.SharedPrefrenceClass
import messenger.messages.messaging.sms.chat.meet.extensions.mPref
import messenger.messages.messaging.sms.chat.meet.extensions.toast
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors

class SplashActivity : BaseHomeActivity() {
    private var isAppOpenAdLoad = false
    var isSuccessful = false
    private var billingClient: BillingClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguage(this)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN
//        )
        setContentView(R.layout.activity_splash)


        billingClient = BillingClient.newBuilder(this).setListener { billingResult, purchases -> }
            .enablePendingPurchases().build()
        SharedPrefrenceClass.getInstance()!!.setBoolean("isSplash", true)

        mPref.dateFormat = DATE_FORMAT_SIX
        Log.e(
            "TAG_WHITESCREEN", "onCreate: SplashScreen"
        )

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
        MainAppClass.getAllMessagesFromDb{

        }
        MainAppClass.getAllAvailableContact {
            handler()
        }
        initSKU()
        Log.d("TAG_GETDATA", "onCreate: after get data")
    }

    private fun setLanguage(context: Context) {
        Utils.setLocale(context, SharedPrefrenceClass.getInstance()?.getString(LANG_CODE,"en"))
    }

    private fun initSKU() {
        billingClient!!.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute {

                    billingClient!!.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    ) { billingResult, purchaseList ->

                        for (purchases in purchaseList) {
                            if (purchases != null && purchases.isAcknowledged) {
                                isSuccessful = true
                                PrefClass.isProUser = true
                                for (i in 0 until purchases.products.size) {
                                    Log.e("BILLING_CLIENT", purchases.products[i].toString())
//                                    var data = purchases.originalJson
                                }
                            } else {
                                PrefClass.isProUser = false
                            }
                        }
                    }


                }

                runOnUiThread {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
//                    onConfig()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("BILLING_CLIENT", "onBillingServiceDisconnected")
                runOnUiThread {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
//                    onConfig()
                }
            }
        })
    }


//    private fun onConfig() {
//        if (MainAppClass.isNetworkConnected(this)) {
//            doNext(
//                "{\n" +
//                    "\"Id\":1,\n" +
//                    "\"bannerid\":\"ca-app-pub-3940256099942544/6300978111\",\n" +
//                    "\"banner\":1,\n" +
//                    "\"nativeids\":[\n" +
//                    "\"ca-app-pub-3940256099942544/2247696110\"\n" +
//                    "],\n" +
//                    "\"native\":1,\n" +
//                    "\"interstitialid\":\"ca-app-pub-3940256099942544/1033173712\",\n" +
//                    "\"interstitial\":1,\n" +
//                    "\"openadid\":\"ca-app-pub-3940256099942544/3419835294\",\n" +
//                    "\"openad\":1,\n" +
//                    "\"rewardadid\":\"ca-app-pub-3940256099942544/5224354917\",\n" +
//                    "\"rewardad\":1,\n" +
//                    "\"adtype\":\"admob\",\n" +
//                    "\"is_ad_enable\":1,\n" +
//                    "\"in_appreview\":1,\n" +
//                    "\"review\":0,\n" +
//                    "\"isactive\":1,\n" +
//                    "\"ads_per_click\":3,\n" +
//                    "\"ads_per_session\":100,\n" +
//                    "\"app_open_count\":100,\n" +
//                    "\"is_splash_on\":1,\n" +
//                    "\"splash_time\":10,\n" +
//                    "\"review_popup_count\":1\n" +
//                    "}"
//            )
//        } else {
//            AdsHelperClass.setIsAdEnable(0)
//            toHome()
//        }
//    }

//    private fun parseAppUserListModel(jsonObject: String?): RemoteAppDataModel? {
//        try {
//            val gson = Gson()
//            val token: TypeToken<RemoteAppDataModel> =
//                object : TypeToken<RemoteAppDataModel>() {}
//            return gson.fromJson(jsonObject, token.type)
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }
//


//    private fun toHome() {
//
//        if (!Utility.getIsIntroShow()) {
//            startActivity(Intent(this, ActivityIntroduction::class.java))
//            finish()
//        } else {
//            getPermission()
//        }
//
//    }


    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    startActivity(Intent(this, ActivityPermissions::class.java))
                    finish()
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                startActivity(Intent(this, ActivityPermissions::class.java))
                finish()
            }
        }
    }

    private fun handler() {

        Handler(Looper.getMainLooper()).postDelayed({
            if (SharedPrefrenceClass.getInstance()?.getBoolean(IS_LOGIN, false) == true) {

                val intent = Intent(
                    this@SplashActivity, HomeActivity::class.java
                )
                startActivity(intent)
                finish()
            } else {

                val intent = Intent(
                    this@SplashActivity, LanguageActivity::class.java
                )
                startActivity(intent)
                finish()
            }
        }, 2000)
    }

    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()

                        }
                    } else {
                        startActivity(Intent(this, LanguageActivity::class.java))
                        finish()
                    }
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        }
    }
}
