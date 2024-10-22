package com.adsdk.plugin;

public class PrintLog {

    public static void d(String PageTag, String ExceptionMsg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("::" + PageTag, "::" + ExceptionMsg);
        }
    }

    public static void e(String PageTag, String ExceptionMsg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e("::" + PageTag, "::" + ExceptionMsg);
        }
    }

    public static void v(String PageTag, String ExceptionMsg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v("::" + PageTag, "::" + ExceptionMsg);
        }
    }

    public static void i(String PageTag, String ExceptionMsg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i("::" + PageTag, "::" + ExceptionMsg);
        }
    }

    public static void w(String PageTag, String ExceptionMsg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w("::" + PageTag, "::" + ExceptionMsg);
        }
    }

}
