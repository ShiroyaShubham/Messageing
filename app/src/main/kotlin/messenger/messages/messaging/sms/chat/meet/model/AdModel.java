package messenger.messages.messaging.sms.chat.meet.model;

import com.google.gson.annotations.SerializedName;

public class AdModel {

    @SerializedName("data")
    public Data data;
    @SerializedName("message")
    public String message;
    @SerializedName("success")
    public boolean success;
    @SerializedName("status")
    public int status;

    public Data getData() {
        if (data != null)
            return data;
        else return new Data();
    }

    public String getMessage() {
        if (message != null)
            return message;
        else return "";
    }

    public static class Data {
        @SerializedName("ads")
        public Ads ads;

        public Ads getAds() {
            if (ads != null)
                return ads;
            else return new Ads();
        }
    }

    public static class Ads {

        @SerializedName("facebook")
        public Facebook facebook;
        @SerializedName("admob")
        public Admob admob;
        @SerializedName("setting")
        public Setting setting;


        public Facebook getFacebook() {
            if (facebook != null)
                return facebook;
            else return new Facebook();
        }

        public Admob getAdmob() {
            if (admob != null)
                return admob;
            else return new Admob();
        }

        public Setting getSetting() {
            if (setting != null)
                return setting;
            else return new Setting();
        }
    }

    public static class Facebook {
        @SerializedName("after_click_count")
        public String after_click_count;
        @SerializedName("after_day")
        public String after_day;
        @SerializedName("rewarded_video")
        public String rewarded_video;
        @SerializedName("interstitial")
        public String interstitial;
        @SerializedName("native_banner")
        public String native_banner;
        @SerializedName("native")
        public String nativeBanner;
        @SerializedName("banner")
        public String banner;

        public String getAfter_click_count() {
            if (after_click_count != null)
                return after_click_count;
            else return "";
        }

        public String getAfter_day() {
            if (after_day != null)
                return after_day;
            else return "";
        }

        public String getRewarded_video() {
            if (rewarded_video != null)
                return rewarded_video;
            else return "";
        }

        public String getInterstitial() {
            if (interstitial != null)
                return interstitial;
            else return "";
        }

        public String getNative_banner() {
            if (native_banner != null)
                return native_banner;
            else return "";
        }

        public String getNativeBanner() {
            if (nativeBanner != null)
                return nativeBanner;
            else return "";
        }

        public String getBanner() {
            if (banner != null)
                return banner;
            else return "";
        }
    }

    public static class Admob {
        @SerializedName("after_click_count")
        public String after_click_count;
        @SerializedName("after_day")
        public String after_day;
        @SerializedName("app_open")
        public String app_open;
        @SerializedName("rewarded_video")
        public String rewarded_video;
        @SerializedName("interstitial")
        public String interstitial;
        @SerializedName("native")
        public String nativeBanner;
        @SerializedName("banner")
        public String banner;

        public String getAfter_click_count() {
            if (after_click_count != null)
                return after_click_count;
            else return "";
        }

        public String getAfter_day() {
            if (after_day != null)
                return after_day;
            else return "";
        }

        public String getApp_open() {
            if (app_open != null)
                return app_open;
            else return "";
        }

        public String getRewarded_video() {
            if (rewarded_video != null)
                return rewarded_video;
            else return "";
        }

        public String getInterstitial() {
            if (interstitial != null)
                return interstitial;
            else return "";
        }

        public String getNativeBanner() {
            if (nativeBanner != null)
                return nativeBanner;
            else return "";
        }

        public String getBanner() {
            if (banner != null)
                return banner;
            else return "";
        }
    }

    public static class Setting {
        @SerializedName("ad_show_status")
        public String ad_show_status;
        @SerializedName("default_app_ad")
        public String default_app_ad;
        @SerializedName("privacy_policy_link")
        public String privacy_policy_link;
        @SerializedName("terms_condition_link")
        public String terms_condition_link;

        public String getPrivacy_policy_link() {
            return privacy_policy_link;
        }

        public String getTerms_condition_link() {
            return terms_condition_link;
        }

        public String getAd_show_status() {
            if (ad_show_status != null)
                return ad_show_status;
            else return "";
        }

        public String getDefault_app_ad() {
            if (default_app_ad != null)
                return default_app_ad;
            else return "";
        }
    }
}
