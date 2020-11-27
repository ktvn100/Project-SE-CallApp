package com.hcmus.callapp.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hcmus.callapp.R;
import com.hcmus.callapp.receivers.NetworkChangeReceiver;
import com.hcmus.callapp.services.SinchService;
import com.sinch.android.rtc.SinchError;

import butterknife.BindView;

public class MainActivity extends BaseActivity implements SinchService.StartFailedListened {

    private static final String SHARED_PREFS_KEY = "shared_prefs";
    private static final String FCM_TOKEN_KEY = "fcm_token";
    private static final String CALLER_ID_DATA_KEY = "callerId";
    private static final String SINCH_ID_KEY = "sinch_id";

    private FirebaseDatabase _UserDB;
    private DatabaseReference _DBRef;
    private String _fcmToken;
    private String _SinchID;
    private String _OriginalCaller;
    private NetworkChangeReceiver networkChangeReceiver;
    private AlertDialog _InternetDialog;

    @BindView(R.id.findButton)
    Button _btn_find;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.findButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCallingActivity();
            }
        });
    }

    @Override
    protected void onServiceConnected() {

    }

    @Override
    protected void onServiceDisconnected() {

    }

    private void openCallingActivity() {
        Intent intent = new Intent(this, CallingActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStartFailed(SinchError error) {

    }

    @Override
    public void onStarted() {

    }
}