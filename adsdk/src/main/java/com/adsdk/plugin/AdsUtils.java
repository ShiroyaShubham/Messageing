package com.adsdk.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.adsdk.plugin.model.AdsConfig;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.jetbrains.annotations.NotNull;

public class AdsUtils {

    private static String TAG = AdsUtils.class.getSimpleName();

    public static AdsConfig adsConfig = new AdsConfig();
    private static AdManagerAdView mediumRectBannerAdView = null;
    private static AdManagerAdView bannerAdView = null;
    private static AdManagerAdView largeBannerAdView = null;
    private static NativeAd nativeAd = null;
    private static InterstitialAd interstitialAd = null;
    private static RewardItem mRewardItem = null;

    protected static void requestMediumRectangleBannerAds(Context context, ViewGroup adsLayout) {
        if (adsConfig.preloadAdsMediumRectBanner) {
            if (mediumRectBannerAdView != null) {
                requestMediumRectBannerPreload(context, adsLayout);
                return;
            }
        }

        requestMediumRectBannerOnDemand(context, adsLayout);
    }

    private static void requestMediumRectBannerPreload(Context context, ViewGroup adsLayout) {
        PrintLog.e(TAG, "requestMediumRectangleBannerAds :: PreLoad");
        try {
            if (mediumRectBannerAdView.getParent() != null) {
                ((ViewGroup) mediumRectBannerAdView.getParent()).removeView(mediumRectBannerAdView);
            }
        } catch (Exception e) {
            PrintLog.e(TAG, "requestMediumRectBannerPreload >>> " + e.getMessage());
        }

        adsLayout.removeAllViews();
        adsLayout.addView(mediumRectBannerAdView);

        mediumRectBannerAdView = null;
        loadMediumRectBannerPreload(context);
    }

    public static void loadMediumRectBannerPreload(Context context) {
        if (TextUtils.isEmpty(adsConfig.adMob.bannerMediumRectAdsId)) {
            return;
        }

        if (mediumRectBannerAdView != null) {
            return;
        }

        mediumRectBannerAdView = new AdManagerAdView(context);
        mediumRectBannerAdView.setAdUnitId(adsConfig.adMob.bannerMediumRectAdsId);
        mediumRectBannerAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        AdRequest.Builder builder = new AdRequest.Builder();
        mediumRectBannerAdView.loadAd(builder.build());
        mediumRectBannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                mediumRectBannerAdView = null;
                PrintLog.e(TAG, "loadMediumRectBannerPreload :: Error >>>> " + loadAdError.getMessage());
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setBannerLogPaidImpression(context, mediumRectBannerAdView, AdsFormat.MediumRectangleBannerAdsFormat);
            }
        });
    }

    private static void requestMediumRectBannerOnDemand(Context context, ViewGroup adsLayout) {
        if (TextUtils.isEmpty(adsConfig.adMob.bannerMediumRectAdsId)) {
            return;
        }
        PrintLog.e(TAG, "requestMediumRectBannerOnDemand :: On Demand");
        AdManagerAdView adView = new AdManagerAdView(context);
        adView.setAdUnitId(adsConfig.adMob.bannerMediumRectAdsId);
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        AdRequest.Builder builder = new AdRequest.Builder();
        adView.loadAd(builder.build());
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                adsLayout.setVisibility(View.GONE);
                PrintLog.e(TAG, "requestMediumRectBannerOnDemand :: Error >>>> " + loadAdError.getMessage());
                //If ads failed then again send Preload request
               /* if(mediumRectBannerAdView == null && adsConfig.preloadAdsMediumRectBanner){
                    loadMediumRectBannerPreload(context);
                }*/
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                try {
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                adsLayout.removeAllViews();
                adsLayout.addView(adView);

                setBannerLogPaidImpression(context, adView, AdsFormat.MediumRectangleBannerAdsFormat);

                //If ads failed then again send Preload request
                /*if(mediumRectBannerAdView == null && adsConfig.preloadAdsMediumRectBanner){
                    loadMediumRectBannerPreload(context);
                }*/
            }
        });
    }

    protected static void requestBannerAds(BaseActivity activity, ViewGroup adsLayout) {
        if (adsConfig.preloadAdsBanner) {
            if (bannerAdView != null) {
                requestBannerPreload(activity, adsLayout);
                return;
            }
        }
        requestBannerOnDemand(activity, adsLayout);
    }

    private static void requestBannerPreload(BaseActivity activity, ViewGroup adsLayout) {
        PrintLog.e(TAG, "requestBannerAds :: PreLoad");
        try {
            if (bannerAdView.getParent() != null) {
                ((ViewGroup) bannerAdView.getParent()).removeView(bannerAdView);
            }
        } catch (Exception e) {
            PrintLog.e(TAG, "requestBannerPreload >>> " + e.getMessage());
        }

        adsLayout.removeAllViews();
        adsLayout.addView(bannerAdView);

        bannerAdView = null;
        loadBannerPreload(activity);
    }

    public static void loadBannerPreload(BaseActivity activity) {
        if (TextUtils.isEmpty(adsConfig.adMob.bannerAdsId)) {
            return;
        }

        if (bannerAdView != null) {
            return;
        }

        bannerAdView = new AdManagerAdView(activity);
        bannerAdView.setAdUnitId(adsConfig.adMob.bannerAdsId);
        bannerAdView.setAdSize(getAdSize(activity));
        Bundle extras = new Bundle();
        //extras.putString("collapsible", "bottom");
        AdRequest.Builder builder = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras);
        bannerAdView.loadAd(builder.build());
        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                bannerAdView = null;
                PrintLog.e(TAG, "loadBannerPreload :: Error >>>> " + loadAdError.getMessage());
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setBannerLogPaidImpression(activity, bannerAdView, AdsFormat.BannerAdsFormat);
            }
        });
    }

    private static void requestBannerOnDemand(BaseActivity activity, ViewGroup adsLayout) {
        if (TextUtils.isEmpty(adsConfig.adMob.bannerAdsId)) {
            return;
        }
        PrintLog.e(TAG, "requestBannerOnDemand :: On Demand");
        AdManagerAdView adView = new AdManagerAdView(activity);
        adView.setAdUnitId(adsConfig.adMob.bannerAdsId);
        adView.setAdSize(getAdSize(activity));
        Bundle extras = new Bundle();
        //extras.putString("collapsible", "bottom");
        AdRequest.Builder builder = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras);
        adView.loadAd(builder.build());
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                adsLayout.setVisibility(View.GONE);
                PrintLog.e(TAG, "requestBannerOnDemand :: Error >>>> " + loadAdError.getMessage());
                //If ads failed then again send Preload request
               /* if(bannerAdView == null && adsConfig.preloadAdsBanner){
                    loadBannerPreload(activity);
                }*/
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                try {
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                adsLayout.removeAllViews();
                adsLayout.addView(adView);

                setBannerLogPaidImpression(activity, adView, AdsFormat.BannerAdsFormat);
                //If ads failed then again send Preload request
                /* if(bannerAdView == null && adsConfig.preloadAdsBanner){
                    loadBannerPreload(activity);
                }*/
            }
        });
    }

    protected static void requestLargeBannerAds(Context context, ViewGroup adsLayout) {
        if (adsConfig.preloadAdsLargeBanner) {
            if (largeBannerAdView != null) {
                requestLargeBannerPreload(context, adsLayout);
                return;
            }
        }

        requestLargeBannerOnDemand(context, adsLayout);
    }

    private static void requestLargeBannerPreload(Context context, ViewGroup adsLayout) {
        PrintLog.e(TAG, "requestLargeBannerAds :: PreLoad");
        try {
            if (largeBannerAdView.getParent() != null) {
                ((ViewGroup) largeBannerAdView.getParent()).removeView(largeBannerAdView);
            }
        } catch (Exception e) {
            PrintLog.e(TAG, "requestLargeBannerPreload >>> " + e.getMessage());
        }

        adsLayout.removeAllViews();
        adsLayout.addView(largeBannerAdView);

        largeBannerAdView = null;
        loadLargeBannerPreload(context);
    }

    public static void loadLargeBannerPreload(Context context) {
        if (TextUtils.isEmpty(adsConfig.adMob.bannerLargeAdsId)) {
            return;
        }

        if (largeBannerAdView != null) {
            return;
        }

        largeBannerAdView = new AdManagerAdView(context);
        largeBannerAdView.setAdUnitId(adsConfig.adMob.bannerLargeAdsId);
        largeBannerAdView.setAdSize(AdSize.LARGE_BANNER);
        AdRequest.Builder builder = new AdRequest.Builder();
        largeBannerAdView.loadAd(builder.build());
        largeBannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                largeBannerAdView = null;
                PrintLog.e(TAG, "loadLargeBannerPreload :: Error >>>> " + loadAdError.getMessage());
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setBannerLogPaidImpression(context, largeBannerAdView, AdsFormat.LargeBannerAdsFormat);
            }
        });
    }

    private static void requestLargeBannerOnDemand(Context context, ViewGroup adsLayout) {
        if (TextUtils.isEmpty(adsConfig.adMob.bannerLargeAdsId)) {
            return;
        }
        PrintLog.e(TAG, "requestLargeBannerOnDemand :: On Demand");
        AdManagerAdView adView = new AdManagerAdView(context);
        adView.setAdUnitId(adsConfig.adMob.bannerLargeAdsId);
        adView.setAdSize(AdSize.LARGE_BANNER);
        AdRequest.Builder builder = new AdRequest.Builder();
        adView.loadAd(builder.build());
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                adsLayout.setVisibility(View.GONE);
                PrintLog.e(TAG, "requestLargeBannerOnDemand :: Error >>>> " + loadAdError.getMessage());
                //If ads failed then again send Preload request
               /* if(largeBannerAdView == null && adsConfig.preloadAdsLargeBanner){
                    loadLargeBannerPreload(context);
                }*/
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                try {
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                adsLayout.removeAllViews();
                adsLayout.addView(adView);

                setBannerLogPaidImpression(context, adView, AdsFormat.LargeBannerAdsFormat);
                //If ads failed then again send Preload request
                /*if(largeBannerAdView == null && adsConfig.preloadAdsLargeBanner){
                    loadLargeBannerPreload(context);
                }*/
            }
        });
    }

    private static void setBannerLogPaidImpression(Context context, AdManagerAdView adManagerAdView, String adFormat) {
        if (adManagerAdView == null) {
            return;
        }

        adManagerAdView.setOnPaidEventListener(new OnPaidEventListener() {
            @Override
            public void onPaidEvent(@NonNull AdValue adValue) {
                try {
                    AdapterResponseInfo responseInfo = adManagerAdView.getResponseInfo().getLoadedAdapterResponseInfo();
                    logPaidImpression(context, responseInfo, adValue, adManagerAdView.getAdUnitId(), adFormat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static AdSize getAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public static void requestMediumNativeAds(BaseActivity activity, ViewGroup adsLayout) {
        if (adsConfig.preloadAdsNative) {
            if (nativeAd != null) {
                requestMediumNativeAdsPreload(activity, adsLayout);
                return;
            }
        }
        requestMediumNativeAdsOnDemand(activity, adsLayout);
    }

    private static void requestMediumNativeAdsPreload(BaseActivity activity, ViewGroup adsLayout) {
        PrintLog.e(TAG, "requestMediumNativeAdsPreload: PreLoad");
        NativeAdView nativeAdView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.ads_native_medium_layout, null);

        inflateAdMobNative(nativeAdView, nativeAd);
        adsLayout.removeAllViews();
        adsLayout.addView(nativeAdView);

        nativeAd = null;
        loadMediumNativeAdsPreload(activity);    //Preload medium native
    }

    public static void loadMediumNativeAdsPreload(BaseActivity activity) {
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) return;
        if (nativeAd != null) return;

        PrintLog.e(TAG, "loadMediumNativeAdsPreload: preload-native request");
        AdLoader adLoader = new AdLoader.Builder(activity, adsConfig.adMob.nativeAdsId)
                .forNativeAd(unifiedNativeAd -> nativeAd = unifiedNativeAd).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        PrintLog.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        nativeAd = null;
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        if (nativeAd != null) {
                            nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                                @Override
                                public void onPaidEvent(@NonNull AdValue adValue) {
                                    try {
                                        AdapterResponseInfo responseInfo = nativeAd.getResponseInfo().getLoadedAdapterResponseInfo();
                                        logPaidImpression(activity, responseInfo, adValue, adsConfig.adMob.nativeAdsId, AdsFormat.NativeAdsFormat);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void requestMediumNativeAdsOnDemand(BaseActivity activity, ViewGroup adsLayout) {
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        PrintLog.e(TAG, "requestMediumNativeAdsOnDemand: OnDemand");
        AdLoader adLoader = new AdLoader.Builder(activity, adsConfig.adMob.nativeAdsId)
                .forNativeAd(unifiedNativeAd -> {
                    NativeAdView nativeAdView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.ads_native_medium_layout, null);
                    inflateAdMobNative(nativeAdView, unifiedNativeAd);
                    adsLayout.removeAllViews();
                    adsLayout.addView(nativeAdView);
                    unifiedNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            try {
                                AdapterResponseInfo responseInfo = unifiedNativeAd.getResponseInfo().getLoadedAdapterResponseInfo();
                                logPaidImpression(activity, responseInfo, adValue, adsConfig.adMob.nativeAdsId, AdsFormat.NativeAdsFormat);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        PrintLog.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        adsLayout.setVisibility(View.GONE);
                        //If ads failed then again send Preload request
                        /*if (nativeAd == null && adsConfig.preloadAdsNative) {
                            loadMediumNativeAdsPreload(activity);
                        }*/
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        //If ads failed then again send Preload request
                        /*if (nativeAd == null && adsConfig.preloadAdsNative) {
                            loadMediumNativeAdsPreload(activity);
                        }*/
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void requestLargeNativeAds(BaseActivity activity, ViewGroup adsLayout) {
        if (adsConfig.preloadAdsNative) {
            if (nativeAd != null) {
                requestLargeNativeAdsPreload(activity, adsLayout);
                return;
            }
        }
        requestLargeNativeAdsOnDemand(activity, adsLayout);
    }

    private static void requestLargeNativeAdsPreload(BaseActivity activity, ViewGroup adsLayout) {
        PrintLog.e(TAG, "requestLargeNativeAdsPreload: PreLoad");
        NativeAdView nativeAdView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.ads_native_large_layout, null);

        inflateAdMobNative(nativeAdView, nativeAd);
        adsLayout.removeAllViews();
        adsLayout.addView(nativeAdView);

        nativeAd = null;
        loadLargeNativeAdsPreload(activity);    //Preload large native
    }

    public static void loadLargeNativeAdsPreload(BaseActivity activity) {
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) return;
        if (nativeAd != null) return;

        PrintLog.e(TAG, "loadLargeNativeAdsPreload: preload-native request");
        AdLoader adLoader = new AdLoader.Builder(activity, adsConfig.adMob.nativeAdsId)
                .forNativeAd(unifiedNativeAd -> nativeAd = unifiedNativeAd).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        PrintLog.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        nativeAd = null;
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        if (nativeAd != null) {
                            nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                                @Override
                                public void onPaidEvent(@NonNull AdValue adValue) {
                                    try {
                                        AdapterResponseInfo responseInfo = nativeAd.getResponseInfo().getLoadedAdapterResponseInfo();
                                        logPaidImpression(activity, responseInfo, adValue, adsConfig.adMob.nativeAdsId, AdsFormat.NativeAdsFormat);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void requestLargeNativeAdsOnDemand(BaseActivity activity, ViewGroup adsLayout) {
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        PrintLog.e(TAG, "requestLargeNativeAdsOnDemand: OnDemand");
        AdLoader adLoader = new AdLoader.Builder(activity, adsConfig.adMob.nativeAdsId)
                .forNativeAd(unifiedNativeAd -> {
                    NativeAdView nativeAdView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.ads_native_large_layout, null);
                    inflateAdMobNative(nativeAdView, unifiedNativeAd);
                    adsLayout.removeAllViews();
                    adsLayout.addView(nativeAdView);
                    unifiedNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            try {
                                AdapterResponseInfo responseInfo = unifiedNativeAd.getResponseInfo().getLoadedAdapterResponseInfo();
                                logPaidImpression(activity, responseInfo, adValue, adsConfig.adMob.nativeAdsId, AdsFormat.NativeAdsFormat);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        PrintLog.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        adsLayout.setVisibility(View.GONE);
                        //If ads failed then again send Preload request
                        /*if (nativeAd == null && adsConfig.preloadAdsNative) {
                            loadLargeNativeAdsPreload(activity);
                        }*/
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        //If ads failed then again send Preload request
                        /*if (nativeAd == null && adsConfig.preloadAdsNative) {
                            loadLargeNativeAdsPreload(activity);
                        }*/
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void requestSmallNativeAds(BaseActivity activity, ViewGroup adsLayout) {
        if (adsConfig.preloadAdsNative) {
            if (nativeAd != null) {
                requestSmallNativeAdsPreload(activity, adsLayout);
                return;
            }
        }
        requestSmallNativeAdsOnDemand(activity, adsLayout);
    }

    private static void requestSmallNativeAdsPreload(BaseActivity activity, ViewGroup adsLayout) {
        PrintLog.e(TAG, "requestSmallNativeAdsPreload: PreLoad");
        NativeAdView nativeAdView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.ads_native_small_layout, null);

        inflateAdMobNative(nativeAdView, nativeAd);
        adsLayout.removeAllViews();
        adsLayout.addView(nativeAdView);

        nativeAd = null;
        loadSmallNativeAdsPreload(activity);    //Preload small native
    }

    public static void loadSmallNativeAdsPreload(BaseActivity activity) {
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) return;
        if (nativeAd != null) return;

        PrintLog.e(TAG, "loadSmallNativeAdsPreload: preload-native request");
        AdLoader adLoader = new AdLoader.Builder(activity, adsConfig.adMob.nativeAdsId)
                .forNativeAd(unifiedNativeAd -> nativeAd = unifiedNativeAd).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        PrintLog.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        nativeAd = null;
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        if (nativeAd != null) {
                            nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                                @Override
                                public void onPaidEvent(@NonNull AdValue adValue) {
                                    try {
                                        AdapterResponseInfo responseInfo = nativeAd.getResponseInfo().getLoadedAdapterResponseInfo();
                                        logPaidImpression(activity, responseInfo, adValue, adsConfig.adMob.nativeAdsId, AdsFormat.NativeAdsFormat);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void requestSmallNativeAdsOnDemand(BaseActivity activity, ViewGroup adsLayout) {
        if (TextUtils.isEmpty(adsConfig.adMob.nativeAdsId)) {
            adsLayout.setVisibility(View.GONE);
            return;
        }
        PrintLog.e(TAG, "requestSmallNativeAdsOnDemand: OnDemand");
        AdLoader adLoader = new AdLoader.Builder(activity, adsConfig.adMob.nativeAdsId)
                .forNativeAd(unifiedNativeAd -> {
                    NativeAdView nativeAdView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.ads_native_small_layout, null);
                    inflateAdMobNative(nativeAdView, unifiedNativeAd);
                    adsLayout.removeAllViews();
                    adsLayout.addView(nativeAdView);
                    unifiedNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            try {
                                AdapterResponseInfo responseInfo = unifiedNativeAd.getResponseInfo().getLoadedAdapterResponseInfo();
                                logPaidImpression(activity, responseInfo, adValue, adsConfig.adMob.nativeAdsId, AdsFormat.NativeAdsFormat);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        PrintLog.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        adsLayout.setVisibility(View.GONE);
                        //If ads failed then again send Preload request
                        /*if (nativeAd == null && adsConfig.preloadAdsNative) {
                            loadSmallNativeAdsPreload(activity);
                        }*/
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        //If ads failed then again send Preload request
                        /*if (nativeAd == null && adsConfig.preloadAdsNative) {
                            loadSmallNativeAdsPreload(activity);
                        }*/
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void inflateAdMobNative(NativeAdView adView, NativeAd nativeAd) {
        try {
            try {
                if (!TextUtils.isEmpty(adsConfig.nativeConfig.bgColor)) {
                    adView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(adsConfig.nativeConfig.bgColor)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            TextView ad_headline = adView.findViewById(R.id.ad_headline);
            if (ad_headline != null) {
                try {
                    if (!TextUtils.isEmpty(adsConfig.nativeConfig.textHeadlineColor)) {
                        ad_headline.setTextColor(Color.parseColor(adsConfig.nativeConfig.textHeadlineColor));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (nativeAd.getHeadline() != null && !nativeAd.getHeadline().isEmpty()) {
                    ad_headline.setVisibility(View.VISIBLE);
                    ad_headline.setText(nativeAd.getHeadline());
                    adView.setHeadlineView(ad_headline);
                } else {
                    ad_headline.setVisibility(View.GONE);
                }
            }

            TextView ad_body = adView.findViewById(R.id.ad_body);
            if (ad_body != null) {
                try {
                    if (!TextUtils.isEmpty(adsConfig.nativeConfig.textBodyColor)) {
                        ad_body.setTextColor(Color.parseColor(adsConfig.nativeConfig.textBodyColor));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (nativeAd.getBody() != null && !nativeAd.getBody().isEmpty()) {
                    ad_body.setVisibility(View.VISIBLE);
                    ad_body.setText(nativeAd.getBody());
                    adView.setBodyView(ad_body);
                } else {
                    ad_body.setVisibility(View.GONE);
                }
            }

            Button ad_call_to_action = adView.findViewById(R.id.ad_call_to_action);
            if (ad_call_to_action != null) {
                try {
                    if (!TextUtils.isEmpty(adsConfig.nativeConfig.buttonColor)) {
                        ad_call_to_action.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(adsConfig.nativeConfig.buttonColor)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (nativeAd.getCallToAction() != null && !nativeAd.getCallToAction().isEmpty()) {
                    ad_call_to_action.setVisibility(View.VISIBLE);
                    ad_call_to_action.setText(nativeAd.getCallToAction());
                    adView.setCallToActionView(ad_call_to_action);
                } else {
                    ad_call_to_action.setVisibility(View.GONE);
                }
            }

            ImageView ad_app_icon = adView.findViewById(R.id.ad_app_icon);
            CardView cvIcon = adView.findViewById(R.id.cvIcon);
            if (ad_app_icon != null) {
                if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                    ad_app_icon.setVisibility(View.VISIBLE);
                    cvIcon.setVisibility(View.VISIBLE);
                    ad_app_icon.setImageDrawable(nativeAd.getIcon().getDrawable());
                    adView.setIconView(ad_app_icon);
                } else {
                    ad_app_icon.setVisibility(View.GONE);
                    cvIcon.setVisibility(View.GONE);
                }
            }

            RatingBar ad_stars = adView.findViewById(R.id.ad_stars);
            if (ad_stars != null) {
                if (nativeAd.getStarRating() != null) {
                    ad_stars.setVisibility(View.VISIBLE);
                    ad_stars.setRating(nativeAd.getStarRating().floatValue());
                    adView.setStarRatingView(ad_stars);
                } else {
                    ad_stars.setVisibility(View.GONE);
                }
            }

            MediaView mediaView = adView.findViewById(R.id.ad_media);
            if (mediaView != null) {
                if (nativeAd.getMediaContent() != null /*&& nativeAd.getMediaContent().hasVideoContent()*/) {
                    mediaView.setVisibility(View.VISIBLE);
                    mediaView.setMediaContent(nativeAd.getMediaContent());
                    adView.setMediaView(mediaView);
                } else {
                    mediaView.setVisibility(View.GONE);
                }
            }

            adView.setNativeAd(nativeAd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void showInterstitialAds(BaseActivity activity, FullScreenAdsCallback callback) {
        if (adsConfig.preloadAdsInterstitial) {
            showInterstitialAdsPreload(activity, callback);
            return;
        }

        showInterstitialAdsOnDemand(activity, callback);
    }

    private static void showInterstitialAdsPreload(BaseActivity activity, FullScreenAdsCallback callback) {
        PrintLog.e(TAG, "showInterstitialAdsPreload: preload request - " + interstitialAd);
        if (TextUtils.isEmpty(adsConfig.adMob.interstitialAdsId)) {
            callback.onCompleted();
            interstitialAd = null;
            return;
        }
        if (interstitialAd != null) {
            AppOpenAdsManager.overrideAppOpenShow(true);
            interstitialAd.show(activity);
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    AppOpenAdsManager.overrideAppOpenShow(false);
                    interstitialAd = null;
                    callback.onCompleted();
                    loadInterstitialAdsPreload(activity.getApplicationContext()); //Ads dismissed
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    AppOpenAdsManager.overrideAppOpenShow(false);
                    interstitialAd = null;
                    showInterstitialAdsOnDemand(activity, callback); //OnDemand Interstitial Ads
                    loadInterstitialAdsPreload(activity.getApplicationContext()); //failed to show
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();
                }
            });
        } else {
            //Call OnDemand Interstitial Ads
            showInterstitialAdsOnDemand(activity, callback);
        }
    }

    public static void loadInterstitialAdsPreload(Context context) {
        if (TextUtils.isEmpty(adsConfig.adMob.interstitialAdsId)) return;
        if (interstitialAd != null) return;

        PrintLog.e(TAG, "loadInterstitialAdsPreload: preload request");
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, adsConfig.adMob.interstitialAdsId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                AdsUtils.interstitialAd = interstitialAd;
                setInterstitialLogPaidImpression(context, interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                PrintLog.e(TAG, "loadInterstitialAdsPreload Error >>>> " + loadAdError.getMessage());
                interstitialAd = null;
            }
        });
    }

    private static void showInterstitialAdsOnDemand(BaseActivity activity, FullScreenAdsCallback callback) {
        PrintLog.e(TAG, "showInterstitialAdsOnDemand: On demand request");
        if (TextUtils.isEmpty(adsConfig.adMob.interstitialAdsId)) {
            callback.onCompleted();
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
//        activity.showDialog();
        InterstitialAd.load(activity, adsConfig.adMob.interstitialAdsId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                try {
                    activity.dismissDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                interstitialAd.show(activity);
                AppOpenAdsManager.overrideAppOpenShow(true);
                interstitialAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                AppOpenAdsManager.overrideAppOpenShow(false);
                                try {
                                    activity.dismissDialog();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                callback.onCompleted();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NotNull com.google.android.gms.ads.AdError adError) {
                                AppOpenAdsManager.overrideAppOpenShow(false);
                                try {
                                    activity.dismissDialog();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                callback.onCompleted();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                                try {
                                    activity.dismissDialog();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                            }
                        });
                setInterstitialLogPaidImpression(activity, interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                PrintLog.e(TAG, "showInterstitialAdsOnDemand Error >>>> " + loadAdError.getMessage());
                try {
                    activity.dismissDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                callback.onCompleted();
            }
        });
    }

    private static void setInterstitialLogPaidImpression(Context context, InterstitialAd interstitialAd) {
        if (interstitialAd == null) {
            return;
        }

        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
            @Override
            public void onPaidEvent(@NonNull AdValue adValue) {
                try {
                    AdapterResponseInfo responseInfo = interstitialAd.getResponseInfo().getLoadedAdapterResponseInfo();
                    logPaidImpression(context, responseInfo, adValue, interstitialAd.getAdUnitId(), AdsFormat.InterstitialAdsFormat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void showRewardedVideoAd(BaseActivity activity, RewardAdsListener callback) {
        if (callback == null) {
            return;
        }
        if (TextUtils.isEmpty(adsConfig.adMob.rewardAdsId)) {
            callback.onUserRewarded(null);
            return;
        }

//        activity.showDialog();
        final FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                /*callback.onUserRewarded(null);
                mRewardItem = null;*/
                if (mRewardItem != null) {
                    RewardEarned rewardEarned = new RewardEarned();
                    rewardEarned.setAmount(mRewardItem.getAmount());
                    rewardEarned.setType(mRewardItem.getType());
                    callback.onUserRewarded(rewardEarned);
                } else {
                    callback.onUserRewarded(null);
                }
                mRewardItem = null;
            }
        };

        OnUserEarnedRewardListener rewardCallback = new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                mRewardItem = rewardItem;
               /* RewardEarned rewardEarned = new RewardEarned();
                rewardEarned.setAmount(mRewardItem.getAmount());
                rewardEarned.setType(mRewardItem.getType());
                callback.onUserRewarded(rewardEarned);*/
            }
        };

        RewardedAd.load(
                activity,
                adsConfig.adMob.rewardAdsId,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mRewardItem = null;
                        activity.dismissDialog();
                        Toast.makeText(activity, "Failed to fetch reward!", Toast.LENGTH_SHORT).show();
                        callback.onUserRewarded(null);
                        PrintLog.e(TAG, "onAdFailedToLoad :: Reward " + loadAdError.getMessage());
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        rewardedAd.setFullScreenContentCallback(fullScreenContentCallback);
                        rewardedAd.show(activity, rewardCallback);
                        activity.dismissDialog();
                    }
                });
    }

    public static void logPaidImpression(Context context, AdapterResponseInfo responseInfo, AdValue adValue, String adUnitId, String adFormat) {
        try {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "ad_manager");
            params.putString(FirebaseAnalytics.Param.AD_SOURCE, responseInfo.getAdSourceName());
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
