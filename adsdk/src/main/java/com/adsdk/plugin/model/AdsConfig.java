package com.adsdk.plugin.model;

public class AdsConfig {
    public boolean adsStatus = false;
    public boolean isAllowIntervalInterstitial = false;
    public boolean isAllowIntervalAppOpen = false;
    public int adsIntervalTimeInterstitial = 0;
    public int adsIntervalTimeAppOpen = 0;
    public boolean preloadAdsInterstitial = false;
    public boolean preloadAdsNative = true;
    public boolean preloadAdsBanner = true;
    public boolean preloadAdsMediumRectBanner = true;
    public boolean preloadAdsLargeBanner = true;
    public String privacyPolicyUrl = "";
    public String feedbackEmailId = "";

    public FetchAdsModel adMob = new FetchAdsModel();

    public NativeConfig nativeConfig = new NativeConfig();

    public boolean isProUser = false;
}
