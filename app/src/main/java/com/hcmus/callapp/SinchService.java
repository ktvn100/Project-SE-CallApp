package com.hcmus.callapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

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

    private SinchServiceInterface _SinchServiceInterface = new SinchServiceInterface();
    private SinchClient _SinchClient = null;
    private String _UserID = "anonymous";

    private String CALLER_SCREEN_KEY = "caller_screen";

    private StartFailedListened _Listener;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (_SinchClient != null && _SinchClient.isStarted()){
            _SinchClient.terminate();
        }
        super.onDestroy();
    }

    private void start(String userName){
        if(_SinchClient == null){
            // create new client
            _UserID = userName;

            _SinchClient = Sinch.getSinchClientBuilder()
                    .context(getApplicationContext())
                    .userId(userName)
                    .applicationKey(APP_KEY)
                    .applicationSecret(APP_SECRET)
                    .environmentHost(ENVIRONMENT)
                    .build();

            _SinchClient.setSupportCalling(true);
            _SinchClient.startListeningOnActiveConnection();
            _SinchClient.addSinchClientListener(new MySinchClientListener());

            _SinchClient.getCallClient().setRespectNativeCalls(false);
            _SinchClient.getCallClient().addCallClientListener(new SinchCallClientList());
            _SinchClient.start();
        }
    }

    private void stop(){
        if (_SinchClient != null){
            _SinchClient.terminate();
            _SinchClient = null;
        }
    }

    private boolean isStarted(){
        return (_SinchClient != null && _SinchClient.isStarted());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return _SinchServiceInterface;
    }

    private class SinchServiceInterface extends Binder {
        public Call callUser(String userID){
            if (_SinchClient == null){
                return null;
            }
            return _SinchClient.getCallClient().callUser(userID);
        }

        public String getUsername(){
            return _UserID;
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

        public void setStartListened(StartFailedListened listened){
            _Listener = listened;
        }

        public Call getCall(String callID){
            return _SinchClient.getCallClient().getCall(callID);
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
            if (_Listener != null){
                _Listener.onStarted();
            }
        }

        @Override
        public void onClientStopped(SinchClient sinchClient) {
            Log.d(TAG,"SinchClient is stopped!");
        }

        @Override
        public void onClientFailed(SinchClient sinchClient, SinchError sinchError) {
            if (_Listener != null){
                _Listener.onStartFailed(sinchError);
            }
            _SinchClient.terminate();
            _SinchClient = null;
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
            Intent intent = new Intent(SinchService.this,CallScreenActivity.class);
            intent.putExtra(CALL_ID, call.getCallId());
            intent.putExtra(CALLER_SCREEN_KEY, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            SinchService.this.startActivity(intent);
        }
    }
}
