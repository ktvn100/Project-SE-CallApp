package com.hcmus.callapp.utils;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class NoResponseHandler {
    private static Handler _NoReponseHandler;

    public static Handler getHandler(Context context){
        if ( _NoReponseHandler == null){
            _NoReponseHandler = new Handler(Looper.getMainLooper());
        }
        initHandler(context);
        return _NoReponseHandler;
    }

    private static void initHandler(Context context) {
        _NoReponseHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 60000);
    }

    public static void stopHandler(){
        _NoReponseHandler.removeCallbacksAndMessages(null);
    }


}
