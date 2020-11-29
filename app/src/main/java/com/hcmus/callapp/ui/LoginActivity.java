package com.hcmus.callapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;

import butterknife.BindView;
import butterknife.ButterKnife;

import android.provider.Settings;
import android.provider.Settings.System;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.login)
    Button _btnLogin;

    @BindView(R.id.username)
    EditText _edUsername;

    private FirebaseDatabase _UserDB;
    private DatabaseReference _DBRef;
    private FirebaseAuth _Auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        _UserDB = FirebaseDatabase.getInstance();
        _DBRef = _UserDB.getReference("Users");
        _Auth = FirebaseAuth.getInstance();

        Button button = (Button) findViewById(R.id.login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
                openMainActivity();
            }

        });
    }

    private void registerUser() {
        String androidID = System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        final String _Username = _edUsername.getText().toString();
        User user = new User("1",androidID, _Username);

        _DBRef.child(androidID).setValue(user);
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}