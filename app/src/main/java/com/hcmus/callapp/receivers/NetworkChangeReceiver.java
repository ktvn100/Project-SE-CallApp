package com.hcmus.callapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hcmus.callapp.utils.NetworkUtil;

import timber.log.Timber;

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = NetworkUtil.getConnectivityStatusString(context);

        if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
            Timber.d("No network");
            Intent noInternetIntent = new Intent("ACTION_NO_INTERNET");
            context.sendBroadcast(noInternetIntent);
        } else {
            Timber.d("Network available");
            Intent internetAvailableIntent = new Intent("ACTION_INTERNET_AVAILABLE");
            context.sendBroadcast(internetAvailableIntent);
        }
    }
}
