package com.adsdk.plugin;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.appopen.AppOpenAd;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Date;

public class AppOpenAdsManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AppOpenAdsManager";
    private AppOpenAd appOpenAd = null;
    private static boolean isShowingAd = false;
    private static AppOpenAdsManager self;
    private long lastLoadTime = 0;
    private WeakReference<Activity> currentActivity;
    private final Application myApplication;

    private static boolean allowAdsShow = true;
    public AppOpenAdsManager(Application application){
        myApplication = application;
        myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public static @NonNull AppOpenAdsManager get() {
        return self;
    }

    public static void init(Application application) {
        self = new AppOpenAdsManager(application);
    }

    public void loadAppOpen() {
        if (self == null) return;
        showAdIfAvailable(null);
    }
    @SuppressWarnings("deprecation")
    @OnLifecycleEvent(ON_START)
    public void onStart(){
        if (AdsUtils.adsConfig.isAllowIntervalAppOpen) {
            //app-open for time based interval
            long now = new Date().getTime();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(currentActivity.get());
            long lastAdTime = pref.getLong("last_ad_time_app_open", now);
            long allowedDelta = AdsUtils.adsConfig.adsIntervalTimeAppOpen * 1000L;
            long currentAdTimeDelta = now - lastAdTime;
            if (currentAdTimeDelta >= allowedDelta) {
                pref.edit().putLong("last_ad_time_app_open", now).apply();
                showAdIfAvailable(null);
            }
            return;
        }
        showAdIfAvailable(null);
        Log.e(TAG, "onStart");
    }

    public boolean isAdmobAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    public void fetchAppOpenAd() {
        if(!AdsUtils.adsConfig.adsStatus){
            return;
        }

        if(!allowAdsShow){
            return;
        }

        if (isAdmobAdAvailable() || TextUtils.isEmpty(AdsUtils.adsConfig.adMob.appOpenAdsId)) {
            return;
        }
        Log.e(TAG,"fetchAppOpenAd :: fetch app open ads");
        AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NotNull AppOpenAd ad) {
                AppOpenAdsManager.this.appOpenAd = ad;
                lastLoadTime = new Date().getTime();
                ad.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(@NonNull AdValue adValue) {
                        try {
                            AdapterResponseInfo responseInfo = ad.getResponseInfo().getLoadedAdapterResponseInfo();
                            AdsUtils.logPaidImpression(myApplication.getApplicationContext(), responseInfo, adValue, appOpenAd.getAdUnitId(), AdsFormat.AppOpenAdsFormat);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                // Handle the error.
                Log.e(TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
            }

        };

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
                currentActivity.get(),
                AdsUtils.adsConfig.adMob.appOpenAdsId,
                request,
                loadCallback
        );
    }
    private void showAdIfAvailable(BaseCallbackWithState callback) {
        if(!AdsUtils.adsConfig.adsStatus){
            if(callback!=null){
                callback.onComplete(false);
            }
            return;
        }

        if(!allowAdsShow){
            if(callback!=null){
                callback.onComplete(false);
            }
            return;
        }


        if (!isShowingAd && isAdmobAdAvailable()) {
            Log.e(TAG, "showAdIfAvailable :: App open ads show");
            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            AppOpenAdsManager.this.appOpenAd = null;
                            isShowingAd = false;
                            fetchAppOpenAd(); //closed

                            if (callback != null) {
                                callback.onComplete(true);
                            }
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NotNull AdError adError) {
                            isShowingAd = false;
                            if (callback != null) {
                                callback.onComplete(false);
                            }
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }

                    };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity.get());
        }else{
            Log.e(TAG, "showAdIfAvailable :: Ads can not show");
            fetchAppOpenAd(); //blocked or no id or already showing

            if (callback != null) {
                callback.onComplete(false);
            }
        }
    }

    public void requestAppOpenAdOnDemand(BaseCallbackWithState callback){
        if(!AdsUtils.adsConfig.adsStatus){
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }
        if(TextUtils.isEmpty(AdsUtils.adsConfig.adMob.appOpenAdsId)){
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }

        Log.e(TAG,"requestAppOpenAdOnDemand :: request app open ads");
        AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NotNull AppOpenAd ad) {
                FullScreenContentCallback fullScreenContentCallback =
                        new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                isShowingAd = false;
                                try {
                                    if (currentActivity.get() instanceof BaseActivity) {
                                        ((BaseActivity) currentActivity.get()).dismissDialog();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (callback != null) {
                                    callback.onComplete(true);
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NotNull AdError adError) {
                                isShowingAd = false;
                                try {
                                    if (currentActivity.get() instanceof BaseActivity) {
                                        ((BaseActivity) currentActivity.get()).dismissDialog();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (callback != null) {
                                    callback.onComplete(false);
                                }
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                isShowingAd = true;
                                try {
                                    if (currentActivity.get() instanceof BaseActivity) {
                                        ((BaseActivity) currentActivity.get()).dismissDialog();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        };

                ad.setFullScreenContentCallback(fullScreenContentCallback);
                ad.show(currentActivity.get());
                ad.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(@NonNull AdValue adValue) {
                        try {
                            AdapterResponseInfo responseInfo = ad.getResponseInfo().getLoadedAdapterResponseInfo();
                            AdsUtils.logPaidImpression(myApplication.getApplicationContext(), responseInfo, adValue, appOpenAd.getAdUnitId(), AdsFormat.AppOpenAdsFormat);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                // Handle the error.
                Log.e(TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
                try {
                    if (currentActivity.get() instanceof BaseActivity) {
                        ((BaseActivity) currentActivity.get()).dismissDialog();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.onComplete(false);
                }
            }

        };

        try {
            if (currentActivity.get() instanceof BaseActivity) {
                ((BaseActivity) currentActivity.get()).showDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
                currentActivity.get(),
                AdsUtils.adsConfig.adMob.appOpenAdsId,
                request,
                loadCallback
        );
    }
    public interface BaseCallbackWithState {
        void onComplete(boolean isSuccess);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    public static void overrideAppOpenShow(boolean override) {
        isShowingAd = override;
    }

    public static void allowAdsShowing(boolean status){
        allowAdsShow = status;
    }

    private boolean wasLoadTimeLessThanNHoursAgo(@SuppressWarnings("SameParameterValue") long numHours) {
        long dateDifference = (new Date()).getTime() - lastLoadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }
}
