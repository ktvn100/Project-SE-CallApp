package com.hcmus.callapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;
import com.hcmus.callapp.receivers.NetworkChangeReceiver;
import com.hcmus.callapp.services.SinchService;
import com.sinch.android.rtc.SinchError;

import butterknife.BindView;

import android.provider.Settings;
import android.provider.Settings.System;

import java.util.ArrayList;

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

        _UserDB = FirebaseDatabase.getInstance();
        _DBRef = _UserDB.getReference("Users");


        Button button = (Button) findViewById(R.id.findButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall();
                openCallingActivity();
            }
        });
    }

    private void makeCall() {
        String ID = System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        _DBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int size = (int)snapshot.getChildrenCount();
                Log.d("Size: ", String.valueOf(size));
                if (size > 1){
                    for(DataSnapshot dss:snapshot.getChildren()){
                        String status = dss.child("status").getValue().toString();
                        String androidID = dss.child("androidID").getValue().toString();
                        String username = dss.child("username").getValue().toString();
                        Log.d("ID: ", String.valueOf(androidID));

                        if (androidID != ID && status == "0"){
                            status = "1";
                            User user = new User(status,androidID,username);
                            _DBRef.child(androidID).setValue(user);


                        }
                    }
                } else {

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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
        Toast.makeText(this, "An error has occured.Please restart the app", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStarted() {

    }
}