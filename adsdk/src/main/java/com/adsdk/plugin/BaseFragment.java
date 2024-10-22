package com.adsdk.plugin;

import static com.adsdk.plugin.AdsUtils.adsConfig;
import static com.adsdk.plugin.NetworkCallback.isNetworkConnected;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.Date;

public class BaseFragment extends Fragment implements OnNetworkCallbackListener {
//    private Dialog progressDialog;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkCallback.registerNetworkCallback(requireActivity(),this);
    }


//    public void dismissDialog() {
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
//    }
    public void showBannerAds(ViewGroup adsLayout){
        if(TextUtils.isEmpty(adsConfig.adMob.bannerAdsId)){
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestBannerAds((BaseActivity) requireActivity(),adsLayout);
    }

    public void showMediumRectBannerAds(ViewGroup adsLayout){
        if(TextUtils.isEmpty(adsConfig.adMob.bannerMediumRectAdsId)){
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestMediumRectangleBannerAds((BaseActivity) requireActivity(),adsLayout);
    }

    public void showLargeBannerAds(ViewGroup adsLayout){
        if(TextUtils.isEmpty(adsConfig.adMob.bannerLargeAdsId)){
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestLargeBannerAds((BaseActivity) requireActivity(),adsLayout);
    }

    public void showMediumNativeAds(ViewGroup adsLayout){
        if(TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)){
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestMediumNativeAds((BaseActivity) requireActivity(),adsLayout);
    }

    public void showLargeNativeAds(ViewGroup adsLayout){
        if(TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)){
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestLargeNativeAds((BaseActivity) requireActivity(),adsLayout);
    }

    public void showSmallNativeAds(ViewGroup adsLayout){
        if(TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)){
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestSmallNativeAds((BaseActivity) requireActivity(),adsLayout);
    }
    public void showInterstitialAds(FullScreenAdsCallback callback){
        if(!isNetworkConnected){
            callback.onCompleted();
            return;
        }
        if (adsConfig.isAllowIntervalInterstitial) {
            long now = new Date().getTime();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences((BaseActivity) requireActivity());
            long lastAdTime = pref.getLong("ads_last_time_interstitial", now);
            long allowedDelta = adsConfig.adsIntervalTimeInterstitial * 1000L;
            long currentAdTimeDelta = now - lastAdTime;
            if (currentAdTimeDelta >= allowedDelta) {
                pref.edit().putLong("ads_last_time_interstitial", now).apply();
                AdsUtils.showInterstitialAds((BaseActivity) requireActivity(), callback);
            } else {
                callback.onCompleted();
            }
            return;
        }
        AdsUtils.showInterstitialAds((BaseActivity) requireActivity(), callback);
    }

    protected void showRewardedVideoAd(RewardAdsListener callback){
        if(!isNetworkConnected){
            callback.onUserRewarded(null);
            return;
        }
        if(callback == null){
            return;
        }
        AdsUtils.showRewardedVideoAd((BaseActivity) requireActivity(),callback);
    }

    @Override
    public void onNetworkCallback(boolean isConnected) {
        if(isConnected && adsConfig.adsStatus){
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(AdsUtils.adsConfig.preloadAdsBanner){
                        AdsUtils.loadBannerPreload((BaseActivity) requireActivity());
                    }

//                    if(AdsUtils.adsConfig.preloadAdsMediumRectBanner){
//                        AdsUtils.loadMediumRectBannerPreload((BaseActivity) requireActivity());
//                    }
//
//                    if(AdsUtils.adsConfig.preloadAdsLargeBanner){
//                        AdsUtils.loadLargeBannerPreload((BaseActivity) requireActivity());
//                    }

                    if(AdsUtils.adsConfig.preloadAdsInterstitial){
                        AdsUtils.loadInterstitialAdsPreload((BaseActivity) requireActivity());
                    }

                    if(AdsUtils.adsConfig.preloadAdsNative){
                        AdsUtils.loadLargeNativeAdsPreload((BaseActivity) requireActivity());
                    }
                }
            });
        }
    }
}
