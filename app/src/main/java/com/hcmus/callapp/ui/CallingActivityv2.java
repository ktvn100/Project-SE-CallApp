package com.hcmus.callapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;
import com.hcmus.callapp.services.SinchService;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.List;

import butterknife.ButterKnife;
import timber.log.Timber;

public class CallingActivityv2 extends BaseActivity {

    private boolean _ServiceConnected = false;

    private User user = null;
    private User curUser = null;

    private FirebaseDatabase _Database;
    private DatabaseReference _DBRefs;

    private String _callId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        ButterKnife.bind(this);
        Timber.d("CallingActivity launched");

        user = new User((User) getIntent().getSerializableExtra("User"));
        curUser = new User((User) getIntent().getSerializableExtra("CurUser"));

        _Database = FirebaseDatabase.getInstance();
        _DBRefs = _Database.getReference("Users");

        _callId = new String("call_id");

        handleCall();

        Chronometer chronometer = (Chronometer) findViewById(R.id.chronometer);
        chronometer.start();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        _ServiceConnected = true;
        if (getSinchServiceInterface() != null && !getSinchServiceInterface().isStarted()){
            getSinchServiceInterface().startClient(curUser.androidID);
        }
        Log.d("CallID:",_callId);
        Call call = getSinchServiceInterface().getCall(_callId);
        if (call != null){
            call.answer();
            call.addCallListener(new SinchCallListener());
        }
    }



    private void handleCall() {
        if (user.androidID != null){
            createCall();
        } else {
            //NoResponseHandler.stopHandler();
            Intent intent = new Intent("finish_waitingcallactivity");
            sendBroadcast(intent);

            //_callId = getIntent().getStringExtra(SinchService.CALL_ID);
        }
    }

    private void createCall() {
        try{
            Log.d("DesUser",user.androidID);
            Call call = getSinchServiceInterface().callUser("4ef18ae2906f3762");

            if (call == null) {
                // Service failed for some reason, show a Toast and abort
                Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show();
                return;
            }

            _callId = call.getCallId();
            call.addCallListener(new SinchCallListener());

        } catch (MissingPermissionException e) {
            ActivityCompat.requestPermissions(this, new String[]{e.getRequiredPermission()}, 0);
        }
    }

    private class SinchCallListener implements CallListener {
        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Timber.d("Call ended. Reason: " + cause.toString());
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        }

        @Override
        public void onCallEstablished(Call call) {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        @Override
        public void onCallProgressing(Call call) {
            Timber.d("Call progressing");
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. FCM
        }
    }
}