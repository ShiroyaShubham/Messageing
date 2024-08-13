package messenger.messages.messaging.sms.chat.meet.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.gson.Gson
import messenger.messages.messaging.sms.chat.meet.extensions.getSharedPrefs
import messenger.messages.messaging.sms.chat.meet.listners.AdsDismissCallback
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.utils.*
import naimishtrivedi.`in`.googleadsmanager.BannerAds
import naimishtrivedi.`in`.googleadsmanager.InterstitialAds
import naimishtrivedi.`in`.googleadsmanager.NativeBannerAds
import naimishtrivedi.`in`.googleadsmanager.interfaces.OnAdsListener
import naimishtrivedi.`in`.googleadsmanager.interfaces.OnAdsViewListener
import naimishtrivedi.`in`.googleadsmanager.model.LogPaidImpression


@SuppressLint("StaticFieldLeak")
object AdsManager {

    // private var context: Context? = null
    fun showSmallBannerAds(mAds: BannerAds, context: Context) {
        if (context.getSharedPrefs().getBoolean(PREF_KEY_PURCHASE_STATUS, false)
            || context.getSharedPrefs().getString(AD_STATUS, "") == "0"
            || !Utils.isInternetAvailable(context)
        ) {
            mAds.isVisible = false
            return
        }


        mAds.setOnAdsViewListener(object : OnAdsViewListener {
            override fun onAdsSuccess(adResource: String) {
                mAds.isVisible = true
            }

            override fun onAdsFailure(adResource: String, errorMsg: String) {
                Log.d("TAG_ERROR", "onAdsFailure: $errorMsg")
            }
        })

        mAds.setOnAdRevenueListener { model ->
            Log.d("TAG_IMPRESSION", "showSmallBannerAds: ${Gson().toJson(model)}")
            val logPaidImpression = LogPaidImpression()
            logPaidImpression.adsPlatform = "ad_manager" //Set Ads Platform value

            logPaidImpression.adsPlacement =
                model.adsPlacement //Set Ads Placement value like (banner, reward, native, interstitial or reward)

            logPaidImpression.adsSourceName =
                model.adsSourceName //Set Ads source name value from AdsPaidModel

            logPaidImpression.adsUnitId =
                model.adsUnitId // Set Ads unit id value from AdsPaidModel

            logPaidImpression.currencyCode =
                model.currencyCode //Set Currency code value from AdsPaidModel

            logPaidImpression.valueMicros =
                model.valueMicros //Set value micros from AdsPaidModel

            naimishtrivedi.`in`.googleadsmanager.AdsManager.logPaidImpression(
                context,
                logPaidImpression
            )
        }

        mAds.showAds(context.getSharedPrefs().getString(BANNER_AD_ID, ""))

    }


    fun loadInterstitialAds(activity: Activity, adsCallback: AdsDismissCallback) {
        if (activity.getSharedPrefs().getBoolean(PREF_KEY_PURCHASE_STATUS, false)
            || activity.getSharedPrefs().getString(AD_STATUS, "") == "0"
            ||!Utils.isInternetAvailable(activity)

        ) {
            adsCallback.onAdDismiss()
            return
        }
        val mInterstitialAds =
            InterstitialAds(activity.getSharedPrefs().getString(INTERSTITIAL_AD_ID, ""), activity)

        //add ads callback listener

        //add ads callback listener
        mInterstitialAds.setOnAdsListener(object : OnAdsListener {
            override fun onAdLoaded() {
                if (mInterstitialAds.isAdsReady) {
                    mInterstitialAds.showAd();
                } else {
                    adsCallback.onAdDismiss()
                }
            }

            override fun onAdLoadFailed(p0: String?, p1: String?) {
                adsCallback.onAdDismiss()
            }

            override fun onAdFailedToShow(p0: String?) {
                adsCallback.onAdDismiss()
            }

            override fun onAdShowed() {
            }

            override fun onAdClicked() {

            }

            override fun onAdDismiss() {
                adsCallback.onAdDismiss()
            }

        })

        mInterstitialAds.setOnAdRevenueListener { model ->
            val logPaidImpression = LogPaidImpression()
            logPaidImpression.adsPlatform = "ad_manager" //Set Ads Platform value

            logPaidImpression.adsPlacement =
                model.adsPlacement //Set Ads Placement value like (banner, reward, native, interstitial or reward)

            logPaidImpression.adsSourceName =
                model.adsSourceName //Set Ads source name value from AdsPaidModel

            logPaidImpression.adsUnitId =
                model.adsUnitId // Set Ads unit id value from AdsPaidModel

            logPaidImpression.currencyCode =
                model.currencyCode //Set Currency code value from AdsPaidModel

            logPaidImpression.valueMicros =
                model.valueMicros //Set value micros from AdsPaidModel

            Log.d("TAG_IMPRESSION", "loadInterstitialAds: ${Gson().toJson(logPaidImpression)}")
            naimishtrivedi.`in`.googleadsmanager.AdsManager.logPaidImpression(
                activity,
                logPaidImpression
            )

        }

        //load ads using below method

        //load ads using below method
        mInterstitialAds.loadAd()

    }

    fun showNativeBannerAds(mNativeAds: NativeBannerAds, context: Context) {
        if (context.getSharedPrefs().getBoolean(PREF_KEY_PURCHASE_STATUS, false)
            || context.getSharedPrefs().getString(AD_STATUS, "") == "0"
            ||!Utils.isInternetAvailable(context)
        ) {
            mNativeAds.isVisible = false
            return
        }
        mNativeAds.setOnAdsViewListener(object : OnAdsViewListener {
            override fun onAdsSuccess(adResource: String) {
                Log.e("TAG_NATIVE", "$adResource Success")
                mNativeAds.isVisible = true
            }

            override fun onAdsFailure(adResource: String, errorMsg: String) {
                Log.e("TAG_NATIVE", "$adResource Error msg $errorMsg")
            }
        })

        mNativeAds.setOnAdRevenueListener { model ->
            val logPaidImpression = LogPaidImpression()
            logPaidImpression.adsPlatform = "ad_manager" //Set Ads Platform value

            logPaidImpression.adsPlacement =
                model.adsPlacement //Set Ads Placement value like (banner, reward, native, interstitial or reward)

            logPaidImpression.adsSourceName =
                model.adsSourceName //Set Ads source name value from AdsPaidModel

            logPaidImpression.adsUnitId =
                model.adsUnitId // Set Ads unit id value from AdsPaidModel

            logPaidImpression.currencyCode =
                model.currencyCode //Set Currency code value from AdsPaidModel

            logPaidImpression.valueMicros =
                model.valueMicros //Set value micros from AdsPaidModel


            Log.d("TAG_IMPRESSION", "showNativeBannerAds: ${Gson().toJson(logPaidImpression)}")
            naimishtrivedi.`in`.googleadsmanager.AdsManager.logPaidImpression(
                context,
                logPaidImpression
            )

        }
        //show ads using below method

        //show ads using below method
        mNativeAds.showAds(context.getSharedPrefs().getString(NATIVE_AD_ID, ""))

    }


    fun showMediumRectangleBannerAds(
        mNativeContainer: FrameLayout,
        llNativeShimmer: LinearLayout,
        context: Context
    ) {
        if (context.getSharedPrefs().getBoolean(PREF_KEY_PURCHASE_STATUS, false)
            || context.getSharedPrefs().getString(AD_STATUS, "") == "0"
            ||!Utils.isInternetAvailable(context)
        ) {
            llNativeShimmer.isVisible = false
            mNativeContainer.isVisible = false
            return
        }
        val adView = AdView(context)
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        adView.adUnitId = context.getSharedPrefs().getString(BANNER_AD_ID, "").toString()

        // Create an ad request.
        val adRequest = AdRequest.Builder().build()
        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                mNativeContainer.addView(adView)
                llNativeShimmer.isVisible = false
                // Code to be executed when an ad finishes loading.
                adView.setOnPaidEventListener {
                    val logPaidImpression = LogPaidImpression()
                    logPaidImpression.adsPlatform = "ad_manager" //Set Ads Platform value

                    logPaidImpression.adsPlacement = "banner"

                    val responseInfo = adView.responseInfo?.loadedAdapterResponseInfo

                    if (responseInfo != null) {
                        logPaidImpression.adsSourceName = responseInfo.adSourceName
                    }

                    logPaidImpression.adsUnitId = adView.adUnitId

                    logPaidImpression.currencyCode = it.currencyCode

                    logPaidImpression.valueMicros = it.valueMicros

                    Log.d(
                        "TAG_IMPRESSION",
                        "showMediumRectangleBannerAds: ${Gson().toJson(logPaidImpression)}"
                    )
                    naimishtrivedi.`in`.googleadsmanager.AdsManager.logPaidImpression(
                        context,
                        logPaidImpression
                    )
                }
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }

        // Start loading the ad in the background.
        adView.loadAd(adRequest)

    }


}

