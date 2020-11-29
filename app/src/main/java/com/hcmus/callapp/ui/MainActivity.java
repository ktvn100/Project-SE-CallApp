package com.hcmus.callapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;

import butterknife.BindView;

import android.provider.Settings;
import android.provider.Settings.System;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase _UserDB;
    private DatabaseReference _DBRef;

    @BindView(R.id.findButton)
    Button _btn_find;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handlePermissions();
        }

        _UserDB = FirebaseDatabase.getInstance();
        _DBRef = _UserDB.getReference("Users");

        Button button = (Button) findViewById(R.id.findButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall();
                //openCallingActivity();
            }
        });
    }

    private void handlePermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void makeCall() {
        String ID = System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("ID: ", ID);

        _DBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int size = (int)snapshot.getChildrenCount();
                Log.d("Size: ", String.valueOf(size));

                _DBRef.child(ID).child("status").setValue("0");

                User user = null, curUser = null;

                for(DataSnapshot dss:snapshot.getChildren()){
                    String status = dss.child("status").getValue().toString();
                    String androidID = dss.child("androidID").getValue().toString();
                    String username = dss.child("username").getValue().toString();
                    //User tempUser = new User(status,androidID,username);


                    if (androidID.equals(ID)){
                        curUser = new User(status,androidID,username);
                        Log.d("ID: ", androidID);
                    } else {
                        if (status.equals("0")){
                            user = new User(status,androidID,username);
                            Log.d("ID: ", androidID);
                        }
                    }
                }
                /*if (!checkConnected) {
                    justWaiting();
                } else {
                    callUser(user,curUser);
                }*/
                callUser(user,curUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
            }
        });
    }

    private void callUser(User user, User curUser) {
        Intent intent = new Intent(this, CallingActivity.class);
        intent.putExtra("User",user);
        intent.putExtra("CurUser",curUser);

        startActivity(intent);
        finish();
    }

    private void openCallingActivity() {
        Intent intent = new Intent(this, CallingActivity.class);
        startActivity(intent);
        finish();
    }
}