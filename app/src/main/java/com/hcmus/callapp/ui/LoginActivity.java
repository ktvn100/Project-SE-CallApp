package com.hcmus.callapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;
import com.hcmus.callapp.utils.AESUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

import android.provider.Settings;
import android.provider.Settings.System;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.login)
    Button btnLogin;

    @BindView(R.id.username)
    EditText edtUsername;

    private FirebaseDatabase mUserDB;
    private DatabaseReference mDBRef;
    private FirebaseAuth mAuth;

    public int state = 1;
    public SettingButton settingButton;

    private static final String SHARED_PREFS_KEY = "shared_prefs";
    private static final String SINCH_ID_KEY = "sinch_id";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean dark_mode = sharedPreferences.getBoolean("themeMode", false);
        if (dark_mode)
            setContentView(R.layout.activity_login_dark);
        else
            setContentView(R.layout.activity_login);


        ButterKnife.bind(this);

        mUserDB = FirebaseDatabase.getInstance();
        mDBRef = mUserDB.getReference("users");
        mAuth = FirebaseAuth.getInstance();

        Button button = (Button) findViewById(R.id.login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
                openMainActivity();
            }
        });
        settingButton = new SettingButton(this, R.id.settingBtn);
    }

    private void registerUser() {
        String androidID = System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        String encrypted = "";
        try {
            encrypted = AESUtils.encrypt(androidID);
            Log.d("TEST", "encrypted:" + encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String _Username = edtUsername.getText().toString();
        //encrypted = "ABC";

        User user = new User("0",encrypted, _Username,"false");

        mDBRef.child(encrypted).setValue(user);

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        prefs.edit().putString(SINCH_ID_KEY, encrypted).apply();
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}