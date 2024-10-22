package messenger.messages.messaging.sms.chat.meet.activity

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import com.adsdk.plugin.AdsUtils
import com.adsdk.plugin.AppOpenAdsManager
import com.adsdk.plugin.model.AdsConfig
import com.android.billingclient.api.*
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import messenger.messages.messaging.sms.chat.meet.BuildConfig
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass

open class BaseHomeActivity : BaseActivity() {
    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher
    )

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        initLoadingDialog(this)
    }

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    fun bitmapFromResourceApp(
        res: Resources?, resId: Int,
        reqWidth: Int, reqHeight: Int
    ): Bitmap {
        val options: BitmapFactory.Options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)
        options.inSampleSize = convertSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    fun convertSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val height: Int = options.outHeight
        val width: Int = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initSKU()
        checkPurchases()
        Log.e("TAG", "onCreate:>>>>> "+ PrefClass.isProUser )
        if (!PrefClass.isProUser) {
            Log.e("TAG", "onCreate:>>>>>IF "+ PrefClass.isProUser )
            getBanner()
        }
        else {
            MainAppClass.getAllAvailableContact {
                Log.e("TAG", "onCreate:>>>>>else "+ PrefClass.isProUser )
//                handler()
            }
        }
    }

    private fun checkPurchases() {
        val billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult: BillingResult?, list: List<Purchase?>? -> }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    val subsParams = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()

                    billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
                        if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (subsPurchases.isNotEmpty()) {
                                PrefClass.isProUser = true
                                AppOpenAdsManager.allowAdsShowing(false)
                            } else {
                                PrefClass.isProUser = false
                                AppOpenAdsManager.allowAdsShowing(true)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun getBanner() {
        if (Utils.isInternetAvailable(this)) {
            val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task: Task<Boolean?> ->
                if (task.isSuccessful) {
                    val adsDetails = firebaseRemoteConfig.getString("ads_details")
                    Log.e("Remote", ">>>> $adsDetails")
                    if (!adsDetails.isEmpty()) {
                        AdsUtils.adsConfig = MainAppClass.gson.fromJson(adsDetails, AdsConfig::class.java)
                        if (BuildConfig.DEBUG) {
                            AdsUtils.adsConfig.adMob.bannerAdsId = "/6499/example/banner"
                            AdsUtils.adsConfig.adMob.bannerLargeAdsId = "/6499/example/banner"
                            AdsUtils.adsConfig.adMob.bannerMediumRectAdsId = "/6499/example/banner"
                            AdsUtils.adsConfig.adMob.nativeAdsId = "/6499/example/native"
                            AdsUtils.adsConfig.adMob.rewardAdsId = ""
                            AdsUtils.adsConfig.adMob.interstitialAdsId = "/6499/example/interstitial"
//                            AdsUtils.adsConfig.adsStatus = true
                        }

                        if (AdsUtils.adsConfig.adsStatus && !PrefClass.isProUser) {
                            if (AdsUtils.adsConfig.preloadAdsBanner) {
                                AdsUtils.loadBannerPreload(this)
                            }
                            if (AdsUtils.adsConfig.preloadAdsInterstitial) {
                                AdsUtils.loadInterstitialAdsPreload(this)
                            }

                            if (AdsUtils.adsConfig.preloadAdsNative) {
                                AdsUtils.loadLargeNativeAdsPreload(this)
                            }
                        }
                    }
                }
            }
//            MainAppClass.getAllAvailableContact {
//                handler()
//            }
        }
//        else {
//            MainAppClass.getAllAvailableContact {
//                handler()
//            }
//        }
    }



}
