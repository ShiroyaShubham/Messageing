package messenger.messages.messaging.sms.chat.meet

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.multidex.MultiDex
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.ads.AppOpenManager
import messenger.messages.messaging.sms.chat.meet.extensions.mPref
import messenger.messages.messaging.sms.chat.meet.listners.OnActivityResultLauncher
import messenger.messages.messaging.sms.chat.meet.model.*
import messenger.messages.messaging.sms.chat.meet.utils.*


class MainAppClass : Application() {
    var sContext: Context? = null
    val ADMOB_TAG = "Ads:"
    var context: Context? = null

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        sContext = applicationContext
        application = this

        AppOpenManager(application)

        when (mPref.appTheme) {
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

        MobileAds.initialize(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        checkAppReplacingState()
    }


    public override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }

    private fun checkAppReplacingState() {
        if (resources == null) {
            Process.killProcess(Process.myPid())
        }
    }

    companion object {
        private var mInstance: MainAppClass? = null
        var application: MainAppClass? = null
        val availableSIMCardsAPP = ArrayList<SimModel>()
        var conversationsDBDATA: MutableList<ConversationSmsModel> = arrayListOf()
        var personalMessageDBDATA: List<ConversationSmsModel> = arrayListOf()
        var otpMessageDBDATA: List<ConversationSmsModel> = arrayListOf()
        var transactionMessageDBDATA: List<ConversationSmsModel> = arrayListOf()
        var offerMessageDBDATA: List<ConversationSmsModel> = arrayListOf()
        var mAllContacts: ArrayList<ContactsModel> = arrayListOf()
        var IS_LOOP_WORKING = true

        fun getConversationDataFromDB() {
            Log.e("TAG_WHITESCREEN", "getConversationDataFromDB: 2")
            CoroutineScope(Dispatchers.IO).launch {
                conversationsDBDATA = try {
                    Log.d("TAG_DB_CONS", "getConversationDataFromDB: ${application?.conversationsDB?.getAllList()?.toMutableList()}")
                    application?.conversationsDB?.getAllList()!!.toMutableList()
                } catch (e: Exception) {
                    Log.d("TAG_ERROR", "getConversationDataFromDB: " + e.message)
                    ArrayList()
                }
                Log.e("TAG_WHITESCREEN", "getConversationDataFromDB: 2.1 ${conversationsDBDATA.size}")
                if (conversationsDBDATA.size > 0) {
                    conversationsDBDATA = removeCommonItems(conversationsDBDATA, application?.archivedMessageDao!!.getArchivedUser())
                }

                if (conversationsDBDATA.size > 0) {

                    val localAll =
                        conversationsDBDATA.sortedWith(compareByDescending<ConversationSmsModel> { application?.config!!.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending {
                            it.date
                        }).toMutableList() as ArrayList<ConversationSmsModel>

                    if (isInternetAvailable(application)) {
                        if (conversationsDBDATA.filter { it.date == 1.toLong() }.size > 0) {

                        } else {
                            localAll.add(0, ConversationSmsModel(0, "", 1, false, "", "", false, ""))
                        }
                    }
                    conversationsDBDATA.clear()
                    conversationsDBDATA.addAll(localAll)
                    Log.e("TAG_WHITESCREEN", "getConversationDataFromDB: 3 ${conversationsDBDATA.size}")

                }

            }
        }

        fun getAllMessagesFromDb(onTaskFinish: () -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    application?.let {
//                Personal Message

                        val searchKeyCredit = "Credit"
                        val searchQueryCredit = "%$searchKeyCredit%"
                        val messagesCredit = application?.conversationsDB?.getConversationWithText(searchQueryCredit)

                        val searchKeyDebit = "Debit"
                        val searchQueryDebit = "%$searchKeyDebit%"
                        val messagesDebit = application?.conversationsDB?.getConversationWithText(searchQueryDebit)

                        val searchKeyOTP = "OTP"
                        val searchQueryOTP = "%$searchKeyOTP%"
                        val messagesOTP = application?.conversationsDB?.getConversationWithText(searchQueryOTP)

                        val searchKeyLink = "Link"
                        val searchQueryLink = "%$searchKeyLink%"
                        val messagesLink = it.conversationsDB.getConversationWithText(searchQueryLink)

//                    personalMessageDBDATA = messagesAll - messagesCredit - messagesDebit - messagesOTP - messagesLink

//                OTP Message
                        otpMessageDBDATA = messagesOTP!!

//                Transaction Message
                        transactionMessageDBDATA = messagesCredit!! + messagesDebit!!

//                Offer Message
                        offerMessageDBDATA = messagesLink

                        onTaskFinish.invoke()

                    }
                } catch (e: Exception) {
                    Log.d("TAG_ERROR", "getAllMessagesFromDb: ${e.localizedMessage}")
                }
            }
        }

        fun getAllAvailableContact(callBack: () -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                mAllContacts = SimpleContactsHelperUtils(application!!).getAvailableContacts(false) {
                    mAllContacts = it
                    callBack.invoke()
                }
                Log.d("TAG_GETDATA", "getAllAvailableContact: before get data ${mAllContacts.size}")
            }
        }

        fun removeCommonItems(list1: MutableList<ConversationSmsModel>, list2: List<ArchivedModel>): ArrayList<ConversationSmsModel> {
            list1.removeAll { item1 ->
                list2.any { item2 ->
                    // Customize this condition according to your object comparison logic
                    item1.phoneNumber == item2.number // Assuming 'id' is a property used for comparison
                }
            }

            return list1 as ArrayList<ConversationSmsModel>
        }

        open fun isInternetAvailable(context: Context?): Boolean {
            return if (context != null) {
                val cm = context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
                // test for connection
                if (cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isAvailable && cm.activeNetworkInfo!!.isConnected) {
                    true
                } else {
                    false
                }
            } else false
        }


        @JvmStatic
        @get:Synchronized
        val instance: MainAppClass?
            get() {
                synchronized(MainAppClass::class.java) {
                    synchronized(MainAppClass::class.java) {
                        application = mInstance
                    }
                }
                return application
            }

        fun isNetworkConnected(activity: Activity): Boolean {
            val cm = activity.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        }

        fun setupSIMSelector() {
            val subscriptionManager =
                application!!.getSystemService(SubscriptionManager::class.java)
//        val infoList = subscriptionManager?.activeSubscriptionInfoList

//        val availableSIMs = SubscriptionManager.from(this).activeSubscriptionInfoList ?: return
            val availableSIMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ActivityCompat.checkSelfPermission(application?.sContext!!, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(application?.sContext!!, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                subscriptionManager?.activeSubscriptionInfoList
            } else {
                if (ActivityCompat.checkSelfPermission(application?.sContext!!, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(application?.sContext!!, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                subscriptionManager?.activeSubscriptionInfoList
            }
            Log.d("TAG_SIMCARD", "setupSIMSelector: ${availableSIMs?.size}")

            availableSIMCardsAPP.clear()
            availableSIMs!!.forEachIndexed { index, subscriptionInfo ->
                var label = subscriptionInfo.displayName?.toString() ?: ""
                if (subscriptionInfo.number?.isNotEmpty() == true) {
                    label += " (${subscriptionInfo.number})"
                }
                val SIMCard = SimModel(index + 1, subscriptionInfo.subscriptionId, label)
                availableSIMCardsAPP.add(SIMCard)
                Log.d("TAG_SIM_SIZE", "setupSIMSelector: 1 ${availableSIMs.size}")
            }

        }


    }


    private fun doNext(activity: Activity, intent: Intent?, isFinished: Boolean) {
        if (intent != null) {
            activity.startActivity(intent)
        }
        if (isFinished) {
            activity.finish()
        }
    }

    private fun doNext(activity: Activity, resultLauncher: OnActivityResultLauncher) {
        resultLauncher.onLauncher()
    }


}
