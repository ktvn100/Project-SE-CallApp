package com.hcmus.callapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;
import com.hcmus.callapp.services.SinchService;
import com.hcmus.callapp.utils.AudioPlayer;
import com.hcmus.callapp.utils.NoResponseHandler;
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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import timber.log.Timber;

public class CallingActivity extends BaseActivity implements SensorEventListener {

    private static final String APP_KEY = "112fc617-342d-488d-b838-57181b208d53";
    private static final String APP_SECRET = "kHo4NcMhJUihhTX/rYE6UQ==";
    private static final String ENVIRONMENT = "clientapi.sinch.com";

    //private Call call;
    private TextView callState;
    private SinchClient sinchClient;
    private Button button;
    private String callerId;
    private String recipientId;

    private static final String SHARED_PREFS_KEY = "shared_prefs";
    private static final String SINCH_ID_KEY = "sinch_id";
    private boolean mServiceConnected = false;
    private String mSinchId;
    private String mCallId;

    private static final String CALLERID_DATA_KEY = "callerId";

    private User user = null;
    private User curUser = null;
    private String mOriginalCaller;
    private String mOriginalReceiver;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDBRef;
    private static final String CALL_REQUEST_KEY = "call_request";

    private String _callId = null;
    private Chronometer chronometer;
    private SinchService.SinchServiceInterface mSinchServiceInterface;

    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;
    private long mTotalDuration;

    private SensorManager mSensorManager;
    private Sensor mProximity;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean mIsSpeakerPhone = false;
    private boolean mIsMicMuted = false;
    private AudioManager mAudioManager;
    private AudioPlayer mAudioPlayer;

    private class UpdateCallDurationTask extends TimerTask {

        @Override
        public void run() {
            CallingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateCallDuration();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        Timber.d("CallingActivity launched");


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean dark_mode = sharedPreferences.getBoolean("themeMode", false);
        if (dark_mode)
            setContentView(R.layout.activity_calling_dark);
        else
            setContentView(R.layout.activity_calling);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        chronometer.setText("Waiting...");

        mAudioPlayer = new AudioPlayer(this);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        mSinchId = prefs.getString(SINCH_ID_KEY, null);

        initialiseAuthAndDatabaseReference();

        setupProximitySensor();

        handleCall();

        Button btnHangUp = (Button) findViewById(R.id.btn_hangup);
        btnHangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endCall();
            }
        });

        Button mSpeakerPhoneButton = (Button) findViewById(R.id.btn_speakerphone);
        mSpeakerPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioManager.setSpeakerphoneOn(!mIsSpeakerPhone);
                mIsSpeakerPhone = !mIsSpeakerPhone;
                String toastMessage;
                if (mIsSpeakerPhone) {
                    toastMessage = getString(R.string.speakerphone_on);
                    mSpeakerPhoneButton.setBackgroundResource(R.drawable.speakerphone_selected);
                } else {
                    toastMessage = getString(R.string.speakerphone_off);
                    mSpeakerPhoneButton.setBackgroundResource(R.drawable.ic_speakerphone);
                }
                Toast.makeText(CallingActivity.this, toastMessage, Toast.LENGTH_SHORT).show();

            }
        });
        Button mMicrophoneButton = (Button) findViewById(R.id.btn_microphone);
        mMicrophoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioManager.setMicrophoneMute(!mIsMicMuted);
                mIsMicMuted = !mIsMicMuted;
                String toastMessage;
                if (mIsMicMuted) {
                    toastMessage = getString(R.string.mic_is_muted);
                    mMicrophoneButton.setBackgroundResource(R.drawable.microphone_selected);
                } else {
                    toastMessage = getString(R.string.mic_is_unmuted);
                    mMicrophoneButton.setBackgroundResource(R.drawable.ic_mic_off);
                }
                Toast.makeText(CallingActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initialiseAuthAndDatabaseReference() {
        mDatabase = FirebaseDatabase.getInstance();
        mDBRef = mDatabase.getReference();
    }

    private void setupProximitySensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (mProximity != null) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    mPowerManager.isPowerSaveMode()) {
                Toast.makeText(this, "If you experience any problems in the call, turn off device power saving mode and try again", Toast.LENGTH_LONG).show();
            }
            int field = 0x00000020;
            try {
                // Yeah, this is hidden field.
                field = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            } catch (Throwable ignored) {
            }
            mWakeLock = mPowerManager.newWakeLock(field, getLocalClassName());
        }
    }

    private void handleCall() {
        if (getIntent().hasExtra(CALLERID_DATA_KEY)){
            mOriginalCaller = getIntent().getStringExtra(CALLERID_DATA_KEY);
            //createCall();
            createCallOrTooLate(mOriginalCaller);
        } else {
            listenCall();
        }
    }

    private void createCallOrTooLate(final String callerId) {
        Query query = mDBRef.child("users");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(callerId).child(CALL_REQUEST_KEY).getValue().equals("true")) {
                    mDBRef.child("users").child(callerId).child(CALL_REQUEST_KEY).setValue("false");
                    if (mServiceConnected) {
                        createCall();
                    }
                } else {
                    //TODO Replace with proper activity
                    Toast.makeText(CallingActivity.this, "Too late. Someone else has picked the call", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(CallingActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onServiceConnected() {
        mServiceConnected = true;
        if (getSinchServiceInterface() != null && !getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(mSinchId);
        }
        if (mCallId != null){
            Call call = getSinchServiceInterface().getCall(mCallId);
            if (call != null) {
                call.answer();
                call.addCallListener(new SinchCallListener());
                mOriginalReceiver = call.getRemoteUserId();
            }
        }
    }

    private void listenCall() {
        //sinchClient.startListeningOnActiveConnection();
        //NoResponseHandler.stopHandler();
        Intent intent = new Intent("finish_waitingcallactivity");
        sendBroadcast(intent);

        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
        Timber.d(mCallId);
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();

        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null){
            call.hangup();
        }

        /*curUser.status = "1";
        mDBRefs.child(callerId).setValue(curUser);*/
        chronometer.stop();
        //sinchClient.stopListeningOnActiveConnection();
        //sinchClient.terminate();
        if (mProximity != null) {
            mSensorManager.unregisterListener(this);
        }
        openMainActivity();
    }

    private void createCall() {
        //onServiceConnected();
        try {
            Call call = null;
            //mSinchServiceInterface = getSinchServiceInterface();

            if (getSinchServiceInterface() != null){
                call = getSinchServiceInterface().callUser(mOriginalCaller);
            }

            if (call == null) {
                // Service failed for some reason, show a Toast and abort
                Toast.makeText(this, "An error has occurred", Toast.LENGTH_SHORT).show();
                return;
            }

            mCallId = call.getCallId();
            call.addCallListener(new SinchCallListener());
            if (call.getState().toString().equals("INITIATING")) {

                chronometer.setText(R.string.connecting);
            } else {
                chronometer.setText(call.getState().toString());
            }
        } catch (MissingPermissionException e) {
            ActivityCompat.requestPermissions(this, new String[]{e.getRequiredPermission()}, 0);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    public void onPause() {
        super.onPause();


        if (mDurationTask != null) {
            mDurationTask.cancel();
            mTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProximity != null) {
            if (mWakeLock.isHeld()) {

                mWakeLock.release();
            }
        }
        mSensorManager.unregisterListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mProximity != null) {
            mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {

        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.values[0] < sensorEvent.sensor.getMaximumRange() /*face near phone*/) {

            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        } else {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private String formatTimespan(int totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void updateCallDuration() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {

        }
    }

    private void updateDatabaseCallDuration(final long duration) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }
                mTotalDuration += duration;
            }
        }).start();
    }

    AudioManager.OnAudioFocusChangeListener mFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            }
        }
    };

    private class SinchCallListener implements CallListener {
        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Timber.d("Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();

            // Abandons audio focus so that any interrupted app can gain audio focus
            mAudioManager.abandonAudioFocus(mFocusChangeListener);

            String endMsg = "Call ended";
            Toast.makeText(CallingActivity.this, endMsg, Toast.LENGTH_LONG).show();

            if (mProximity != null) {
                mSensorManager.unregisterListener(CallingActivity.this);
            }

            call = null;
            endCall();
        }

        @Override
        public void onCallEstablished(Call call) {
            //setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

            Timber.d("Call established");
            Toast.makeText(CallingActivity.this, "Call Connected", Toast.LENGTH_SHORT).show();
            mAudioManager.requestAudioFocus(mFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            mIsSpeakerPhone = false;
            mIsMicMuted = false;
            mAudioManager.setSpeakerphoneOn(mIsSpeakerPhone);
            mAudioManager.setMicrophoneMute(mIsMicMuted);
            mAudioPlayer.stopProgressTone();

            mDBRef.child("users").child(mSinchId).child(CALL_REQUEST_KEY).setValue("false");
            startClock();
        }

        @Override
        public void onCallProgressing(Call call) {
            Timber.d("Call progressing");
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. FCM
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