package com.hcmus.callapp.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.hcmus.callapp.ui.CallingActivity;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;

public class SinchService extends Service {

    //API Key
    private static final String APP_KEY = "112fc617-342d-488d-b838-57181b208d53";
    private static final String APP_SECRET = "kHo4NcMhJUihhTX/rYE6UQ==";
    private static final String ENVIRONMENT = "clientapi.sinch.com";

    public static final String CALL_ID = "call_id";
    static final String TAG = SinchService.class.getSimpleName();

    private final IBinder mSinchServiceInterface = new SinchServiceInterface();
    private SinchClient mSinchClient;
    private String mUserID;

    private String CALLER_SCREEN_KEY = "caller_screen";

    private StartFailedListened mListener;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (mSinchClient != null && mSinchClient.isStarted()){
            mSinchClient.terminate();
        }
        super.onDestroy();
    }

    private void start(String userName){
        if(mSinchClient == null){
            // create new client
            mUserID = userName;

            mSinchClient = Sinch.getSinchClientBuilder()
                    .context(getApplicationContext())
                    .userId(userName)
                    .applicationKey(APP_KEY)
                    .applicationSecret(APP_SECRET)
                    .environmentHost(ENVIRONMENT)
                    .build();

            mSinchClient.setSupportCalling(true);
            mSinchClient.startListeningOnActiveConnection();
            mSinchClient.addSinchClientListener(new MySinchClientListener());

            mSinchClient.getCallClient().setRespectNativeCalls(false);
            mSinchClient.getCallClient().addCallClientListener(new SinchCallClientList());
            mSinchClient.start();
        }
    }

    private void stop(){
        if (mSinchClient != null){
            mSinchClient.terminate();
            mSinchClient = null;
        }
    }

    private boolean isStarted(){
        return (mSinchClient != null && mSinchClient.isStarted());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mSinchServiceInterface;
    }

    public class SinchServiceInterface extends Binder {
        public Call callUser(String userID){
            if (mSinchClient == null){
                return null;
            }
            return mSinchClient.getCallClient().callUser(userID);
        }

        public String getUsername(){
            return mUserID;
        }

        public boolean isStarted(){
            return SinchService.this.isStarted();
        }

        public void startClient(String userName){
            SinchService.this.start(userName);
        }

        public void stopClient(){
            SinchService.this.stop();
        }

        public void setStartListener(StartFailedListened listened){
            mListener = listened;
        }

        public Call getCall(String callID){
            return mSinchClient.getCallClient().getCall(callID);
        }
    }

    public interface StartFailedListened {
        void onStartFailed(SinchError error);

        void onStarted();
    }


    private class MySinchClientListener implements SinchClientListener {
        @Override
        public void onClientStarted(SinchClient sinchClient) {
            Log.d(TAG,"SinchClient is started!");
            if (mListener != null){
                mListener.onStarted();
            }
        }

        @Override
        public void onClientStopped(SinchClient sinchClient) {
            Log.d(TAG,"SinchClient is stopped!");
        }

        @Override
        public void onClientFailed(SinchClient sinchClient, SinchError sinchError) {
            if (mListener != null){
                mListener.onStartFailed(sinchError);
            }
            mSinchClient.terminate();
            mSinchClient = null;
        }

        @Override
        public void onRegistrationCredentialsRequired(SinchClient sinchClient, ClientRegistration clientRegistration) {

        }

        @Override
        public void onLogMessage(int i, String s, String s1) {
            switch (i){
                case Log.DEBUG:
                    Log.d(s,s1);
                    break;
                case Log.ERROR:
                    Log.e(s,s1);
                    break;
                case Log.INFO:
                    Log.i(s,s1);
                    break;
                case Log.VERBOSE:
                    Log.v(s,s1);
                    break;
                case Log.WARN:
                    Log.w(s,s1);
                    break;
                default:
                    Log.wtf(s,s1);
                    break;
            }
        }
    }

    private class SinchCallClientList implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call call) {
            Log.d(TAG,"Incoming call!");
            Intent intent = new Intent(SinchService.this, CallingActivity.class);
            intent.putExtra(CALL_ID, call.getCallId());
            intent.putExtra(CALLER_SCREEN_KEY, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            SinchService.this.startActivity(intent);
        }
    }
}
