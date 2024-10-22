package messenger.messages.messaging.sms.chat.meet.utils;

import android.content.Context;
import android.content.SharedPreferences;

import messenger.messages.messaging.sms.chat.meet.MainAppClass;


public class PreferenceHelper {

    public static SharedPreferences WeatherForecastSharedPreference;
//    public static boolean isDebugApp = false;

    public static void putString(String key, String value) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext().getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = WeatherForecastSharedPreference.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(String key, String defaultValue) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext().getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
       /* if (isDebugApp) {
            return getTestAdIds(key);
        }*/
        return WeatherForecastSharedPreference.getString(key, defaultValue);
    }

    public static void putInt(String key, int value) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext()
                .getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = WeatherForecastSharedPreference.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static int getInt(String key, int defaultValue) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext().getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        return WeatherForecastSharedPreference.getInt(key, defaultValue);
    }

    public static void putBoolean(String key, boolean value) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext().getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = WeatherForecastSharedPreference.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext()
                .getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        boolean string = WeatherForecastSharedPreference.getBoolean(key, defaultValue);
        return string;
    }

    public static boolean contains(String key) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext()
                .getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        return WeatherForecastSharedPreference.contains(key);
    }

    public static void remove(String key) {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext()
                .getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = WeatherForecastSharedPreference.edit();
        editor.remove(key);
        editor.apply();
    }

    public static void clearPreference() {
        WeatherForecastSharedPreference = MainAppClass.getInstance().getSContext()
                .getSharedPreferences("WeatherForecast", Context.MODE_PRIVATE);
        WeatherForecastSharedPreference.edit().clear().apply();
    }
}
