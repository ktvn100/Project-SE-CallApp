package com.hcmus.callapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import timber.log.Timber;

public class NetworkUtil {
    public static int TYPE_NOT_CONNECTED = 0;
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;

    public static final int NETWORK_STATUS_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_WIFI = 1;
    public static final int NETWORK_STATUS_MOBILE = 2;

    public static int getConnectivityStatus(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null){
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                return TYPE_WIFI;
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
                return TYPE_MOBILE;
            }
        }
        return TYPE_NOT_CONNECTED;
    }

    public static int getConnectivityStatusString(Context context){
        int pre_status = NetworkUtil.getConnectivityStatus(context);

        int status = 0;

        if (pre_status == NetworkUtil.TYPE_NOT_CONNECTED){
            status = NETWORK_STATUS_NOT_CONNECTED;
            Timber.d("Status is "+status);
        } else if (pre_status == NetworkUtil.TYPE_WIFI){
            status = NETWORK_STATUS_WIFI;
            Timber.d("Status is "+status);
        } else if (pre_status == NetworkUtil.TYPE_MOBILE){
            status = NETWORK_STATUS_MOBILE;
            Timber.d("Status is "+status);
        }

        return status;
    }

}
