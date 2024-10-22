package com.adsdk.plugin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;

public class NetworkCallback {
    public static boolean isNetworkConnected = false;
    private static OnNetworkCallbackListener networkCallback = null;
    public static void registerNetworkCallback(Context context,OnNetworkCallbackListener networkCallback) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback);
            }else{
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                NetworkRequest networkRequest = builder.build();
                connectivityManager.registerNetworkCallback(networkRequest,callback);
            }
            isNetworkConnected = false;
            NetworkCallback.networkCallback = networkCallback;
        } catch (Exception e) {
            e.printStackTrace();
            isNetworkConnected = false;
        }
    }

    private static ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            isNetworkConnected = true;
            if(NetworkCallback.networkCallback!=null){
                NetworkCallback.networkCallback.onNetworkCallback(isNetworkConnected);
            }
        }

        @Override
        public void onLost(Network network) {
            isNetworkConnected = false;
            if(NetworkCallback.networkCallback!=null){
                NetworkCallback.networkCallback.onNetworkCallback(isNetworkConnected);
            }
        }
    };
}
