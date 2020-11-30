package com.hcmus.callapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;
import com.hcmus.callapp.services.SinchService;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.List;

import butterknife.ButterKnife;
import timber.log.Timber;

public class CallingActivity extends AppCompatActivity {

    private static final String APP_KEY = "112fc617-342d-488d-b838-57181b208d53";
    private static final String APP_SECRET = "kHo4NcMhJUihhTX/rYE6UQ==";
    private static final String ENVIRONMENT = "clientapi.sinch.com";

    private Call call;
    private TextView callState;
    private SinchClient sinchClient;
    private Button button;
    private String callerId;
    private String recipientId;

    private User user = null;
    private User curUser = null;

    private FirebaseDatabase _Database;
    private DatabaseReference _DBRefs;

    private String _callId = null;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        Timber.d("CallingActivity launched");

        _Database = FirebaseDatabase.getInstance();
        _DBRefs = _Database.getReference("Users");

        user = (User) getIntent().getSerializableExtra("User");
        curUser = (User) getIntent().getSerializableExtra("CurUser");

        if (curUser != null) callerId = curUser.androidID;
        if (user != null) recipientId = user.androidID;

        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(callerId)
                .applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET)
                .environmentHost(ENVIRONMENT)
                .build();

        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();

        //sinchClient.addSinchClientListener(new MySinchClientListener());
        sinchClient.getCallClient().setRespectNativeCalls(false);
        sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());

        sinchClient.start();

        handleCall();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean dark_mode = sharedPreferences.getBoolean("themeMode", false);
        if (dark_mode)
            setContentView(R.layout.activity_calling_dark);
        else
            setContentView(R.layout.activity_calling);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        chronometer.setText("Waiting...");

        Button btnHangUp = (Button) findViewById(R.id.btn_hangup);
        btnHangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endCall();
            }
        });
    }

    private void handleCall() {
        if (user != null){
            createCall();
        } else {
            listenCall();
        }
    }

    private void listenCall() {
        sinchClient.startListeningOnActiveConnection();
    }

    private void endCall() {
        if (call != null){
            call.hangup();
        }

        curUser.status = "1";
        _DBRefs.child(callerId).setValue(curUser);
        chronometer.stop();
        sinchClient.stopListeningOnActiveConnection();
        sinchClient.terminate();
        openMainActivity();
    }

    private void createCall() {
        call = sinchClient.getCallClient().callUser(recipientId);
        call.addCallListener(new SinchCallListener());
    }

    private class SinchCallListener implements CallListener {
        @Override
        public void onCallEnded(Call call) {
            call = null;
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            endCall();
        }

        @Override
        public void onCallEstablished(Call call) {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            startClock();
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

    private class SinchCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            call = incomingCall;
            Toast.makeText(CallingActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
            call.answer();
            call.addCallListener(new SinchCallListener());
            startClock();
        }
    }

    private void startClock(){
        long base = SystemClock.elapsedRealtime();
        chronometer.setBase(base);
        chronometer.start();
    }

    private void openMainActivity() {
        Intent intent = new Intent(CallingActivity.this, MainActivity.class);
        finish();
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        endCall();
        super.onBackPressed();
    }
}