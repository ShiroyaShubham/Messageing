package com.adsdk.plugin;

import static com.adsdk.plugin.AdsUtils.adsConfig;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Date;

public class BaseActivity extends AppCompatActivity implements OnNetworkCallbackListener {
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkCallback.registerNetworkCallback(this, this);
    }

    public void showDialog() {
        if (progressDialog != null && !progressDialog.isShowing() && !isFinishing()) {
            progressDialog.show();
        } else {
            progressDialog = new Dialog(this);
            progressDialog.setContentView(R.layout.ad_loading);
            progressDialog.setCancelable(false);
            if (progressDialog.getWindow() != null)
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            progressDialog.show();
        }
    }

    public void dismissDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void showBannerAds(ViewGroup adsLayout) {
        if (!adsConfig.adsStatus) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.bannerAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestBannerAds(this, adsLayout);
    }

    public void showMediumRectBannerAds(ViewGroup adsLayout) {
        if (!adsConfig.adsStatus) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.bannerMediumRectAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestMediumRectangleBannerAds(this, adsLayout);
    }

    public void showLargeBannerAds(ViewGroup adsLayout) {
        if (!adsConfig.adsStatus) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.bannerLargeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestLargeBannerAds(this, adsLayout);
    }

    public void showMediumNativeAds(ViewGroup adsLayout) {
        if (!adsConfig.adsStatus) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestMediumNativeAds(this, adsLayout);
    }

    public void showLargeNativeAds(ViewGroup adsLayout) {
        if (!adsConfig.adsStatus) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestLargeNativeAds(this, adsLayout);
    }

    public void showSmallNativeAds(ViewGroup adsLayout) {
        if (!adsConfig.adsStatus) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }

        AdsUtils.requestSmallNativeAds(this, adsLayout);
    }

    public void showInterstitialAds(FullScreenAdsCallback callback) {
        if (callback == null) {
            return;
        }
        if (!adsConfig.adsStatus) {
            callback.onCompleted();
            return;
        }

        if (!NetworkCallback.isNetworkConnected) {
            callback.onCompleted();
            return;
        }
        if (adsConfig.isAllowIntervalInterstitial) {
            long now = new Date().getTime();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            long lastAdTime = pref.getLong("ads_last_time_interstitial", now);
            long allowedDelta = adsConfig.adsIntervalTimeInterstitial * 1000L;
            long currentAdTimeDelta = now - lastAdTime;
            if (currentAdTimeDelta >= allowedDelta || currentAdTimeDelta == 0) {
                pref.edit().putLong("ads_last_time_interstitial", now).apply();
                AdsUtils.showInterstitialAds(this, callback);
            } else {
                callback.onCompleted();
            }
            return;
        }

        AdsUtils.showInterstitialAds(this, callback);
    }

    protected void showRewardedVideoAd(RewardAdsListener callback) {
        if (callback == null) {
            return;
        }

        if (!adsConfig.adsStatus) {
            callback.onUserRewarded(null);
            return;
        }
        if (!NetworkCallback.isNetworkConnected) {
            callback.onUserRewarded(null);
            return;
        }
        AdsUtils.showRewardedVideoAd(this, callback);
    }

    @Override
    public void onNetworkCallback(boolean isConnected) {
        if (isConnected && adsConfig.adsStatus) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (AdsUtils.adsConfig.preloadAdsBanner) {
                        AdsUtils.loadBannerPreload(BaseActivity.this);
                    }

                    if (AdsUtils.adsConfig.preloadAdsMediumRectBanner) {
                        AdsUtils.loadMediumRectBannerPreload(BaseActivity.this);
                    }

                    if (AdsUtils.adsConfig.preloadAdsLargeBanner) {
                        AdsUtils.loadLargeBannerPreload(BaseActivity.this);
                    }

                    if (AdsUtils.adsConfig.preloadAdsInterstitial) {
                        AdsUtils.loadInterstitialAdsPreload(BaseActivity.this);
                    }

                    if (AdsUtils.adsConfig.preloadAdsNative) {
                        AdsUtils.loadLargeNativeAdsPreload(BaseActivity.this);
                    }
                }
            });
        }
    }
}
