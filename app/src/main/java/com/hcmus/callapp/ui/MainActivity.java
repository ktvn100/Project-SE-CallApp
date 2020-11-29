package com.hcmus.callapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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

    @BindView(R.id.findButton)
    Button _btn_find;

    CallAction callAction;
    SettingButton settingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // check for theme mode
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean dark_mode = sharedPreferences.getBoolean("themeMode", false);
        if (dark_mode)
            setContentView(R.layout.activity_main_dark);
        else
            setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handlePermissions();
        }

        callAction = new CallAction(this);

        settingButton = new SettingButton(this, R.id.settingBtn);
    }

    private void handlePermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void openCallingActivity() {
        Intent intent = new Intent(this, CallingActivity.class);
        startActivity(intent);
    }
}