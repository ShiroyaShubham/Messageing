package messenger.messages.messaging.sms.chat.meet.ads;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
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

import java.util.Date;

import messenger.messages.messaging.sms.chat.meet.activity.BaseActivity;
import messenger.messages.messaging.sms.chat.meet.utils.ConstantsKt;

public class AppOpenManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {

    public static boolean isShowAppOpenManagerAd = true;
    static Dialog dialogCustom;
    private final Application myApplication;
    private AppOpenAd appOpenAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private Activity currentActivity;
    private long loadTime = 0;

    public static boolean isDoneLanguageChange = false;
    private int activityReferences = 0;

    /**
     * Constructor
     */

    public AppOpenManager(Application myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Request an ad
     */
    public void fetchAd() {
        // Have unused ad, no need to fetch another.
        try {
            Context mContext = (Context) currentActivity;
            if (mContext.getSharedPreferences(ConstantsKt.PREFS_KEY, Context.MODE_PRIVATE).getString(ConstantsKt.AD_STATUS, "").equals("0")) {
                if (isAdAvailable()) {
                    return;
                }

                loadCallback =
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        /**
                         * Called when an app open ad has loaded.
                         *
                         * @param ad the loaded app open ad.
                         */
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
                            AppOpenManager.this.appOpenAd = ad;
                            AppOpenManager.this.loadTime = (new Date()).getTime();

                            ad.setOnPaidEventListener(new OnPaidEventListener() {
                                @Override
                                public void onPaidEvent(@NonNull AdValue adValue) {
                                    try {
                                        AdapterResponseInfo responseInfo = ad.getResponseInfo().getLoadedAdapterResponseInfo();
//                                        AdsUtility.logPaidImpression(myApplication.getApplicationContext(), responseInfo, adValue, appOpenAd.getAdUnitId(), "app_open");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }

                        /**
                         * Called when an app open ad has failed to load.
                         *
                         * @param loadAdError the error.
                         */
                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            // Handle the error.
                        }

                    };
                AdRequest request = getAdRequest();
                AppOpenAd.load(
                    myApplication, mContext.getSharedPreferences(ConstantsKt.PREFS_KEY, Context.MODE_PRIVATE).getString(ConstantsKt.APP_OPEN_AD_ID, ""), request,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }


    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public boolean isAdAvailable() {
        return appOpenAd != null;
    }

    public void showAdIfAvailable() {
        if (isShowAppOpenManagerAd && isAdAvailable()) {

            /*showFullScreenAdLoading(currentActivity);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {



                }
            }, 1000);*/

            FullScreenContentCallback fullScreenContentCallback =
                new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        AppOpenManager.this.appOpenAd = null;
                        fetchAd();
//                                    hideFullScreenAdLoading();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        System.out.println(">>>>>>>>>><<<<<<<<< AdError : " + adError.getMessage());
//                                    hideFullScreenAdLoading();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        System.out.println(">>>>>>>>>><<<<<<<<< onAdShowedFullScreenContent : ");
//                                    hideFullScreenAdLoading();
                    }
                };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);

        } else {
            fetchAd();
        }
    }


    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        //isMainActivityBackground = true;
        Log.d("TAG_BACKGROUND", "onStart: ");
        fetchAd();
        //showAdIfAvailable();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        currentActivity = activity;
        // isMainActivityBackground = false;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
        // isMainActivityBackground = false;

        int cc = ++activityReferences;

        System.out.println(">>>>>>>>>>>>>><<<<<<< onActivityStarted : " + cc);

        if (cc == 1 && !activity.getLocalClassName().contains("DashboardActivity") && !isDoneLanguageChange) {
            // App enters foreground
            showAdIfAvailable();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

        System.out.println(">>>>>>>>>>>>>><<<<<<< onActivityResumed : " + activityReferences);

        currentActivity = activity;



        /*if (isMainActivityBackground) {
            isMainActivityBackground = false;
            if (activity.getLocalClassName().endsWith(".MainActivity") || activity.getLocalClassName().endsWith(".MainParentActivity")) {
                Log.d("TAG", "onActivityResumed: Success");
                showAdIfAvailable();
            }
        }*/
    }

    @Override
    public void onActivityStopped(Activity activity) {
        int cc = --activityReferences;
        //isMainActivityBackground = false;
        if (cc == 0) {
            // App enters background
        }
        System.out.println(">>>>>>>>>>>>>><<<<<<< onActivityStopped : " + cc);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // isMainActivityBackground = false;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        //isMainActivityBackground = false;
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
    }

    /*public static void showFullScreenAdLoading(Context context) {
        dialogCustom = new Dialog(context);
        dialogCustom.setContentView(R.layout.ad_loading_dialog);
        dialogCustom.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogCustom.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        dialogCustom.setCancelable(false);
        dialogCustom.getWindow().getAttributes().windowAnimations = R.style.animation;

        dialogCustom.show();
    }

    public static void hideFullScreenAdLoading() {
        if (dialogCustom != null && dialogCustom.isShowing()) {
            dialogCustom.dismiss();
        }
    }*/

}
