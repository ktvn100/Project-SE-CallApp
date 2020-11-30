package com.hcmus.callapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hcmus.callapp.R;

import butterknife.BindView;

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