package com.sinch.android.rtc.sample.push;

import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;
import com.sinch.android.rtc.UserRegistrationCallback;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;

import static com.sinch.android.rtc.sample.push.SinchService.APP_KEY;
import static com.sinch.android.rtc.sample.push.SinchService.APP_SECRET;
import static com.sinch.android.rtc.sample.push.SinchService.ENVIRONMENT;

public class LoginActivity extends BaseActivity implements SinchService.StartFailedListener, PushTokenRegistrationCallback, UserRegistrationCallback {

    private Button mLoginButton;
    private EditText mLoginName;
    private ProgressDialog mSpinner;

    private String mUserId;
    private long mSigningSequence = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mLoginName = (EditText) findViewById(R.id.loginName);

        mLoginButton = (Button) findViewById(R.id.loginButton);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginClicked();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onServiceConnected() {
        if (getSinchServiceInterface().isStarted()) {
            openPlaceCallActivity();
        } else {
            getSinchServiceInterface().setStartListener(this);
        }
    }

    @Override
    protected void onPause() {
        dismissSpinner();
        super.onPause();
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
        dismissSpinner();
    }

    @Override
    public void onStarted() {
        openPlaceCallActivity();
    }

    private void startClientAndOpenPlaceCallActivity() {
        // start Sinch Client, it'll result onStarted() callback from where the place call activity will be started
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient();
            showSpinner();
        }
    }

    private void loginClicked() {
        String username = mLoginName.getText().toString();
        getSinchServiceInterface().setUsername(username);

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show();
            return;
        }

        if (!username.equals(getSinchServiceInterface().getUsername())) {
            getSinchServiceInterface().stopClient();
        }

        mUserId = username;
        UserController uc = Sinch.getUserControllerBuilder()
                .context(getApplicationContext())
                .applicationKey(APP_KEY)
                .userId(mUserId)
                .environmentHost(ENVIRONMENT)
                .build();
        uc.registerUser(this, this);
    }

    private void openPlaceCallActivity() {
        Intent mainActivity = new Intent(this, PlaceCallActivity.class);
        startActivity(mainActivity);
    }

    private void showSpinner() {
        mSpinner = new ProgressDialog(this);
        mSpinner.setTitle("Logging in");
        mSpinner.setMessage("Please wait...");
        mSpinner.show();
    }

    private void dismissSpinner() {
        if (mSpinner != null) {
            mSpinner.dismiss();
            mSpinner = null;
        }
    }

    @Override
    public void tokenRegistered() {
        dismissSpinner();
        startClientAndOpenPlaceCallActivity();
    }

    @Override
    public void tokenRegistrationFailed(SinchError sinchError) {
        dismissSpinner();
        Toast.makeText(this, "Push token registration failed - incoming calls can't be received!", Toast.LENGTH_LONG).show();
    }

    // The most secure way is to obtain the signature from the backend,
    // since storing APP_SECRET in the app is not secure.
    // Following code demonstrates how the signature is obtained provided
    // the UserId and the APP_KEY and APP_SECRET.
    @Override
    public void onCredentialsRequired(ClientRegistration clientRegistration) {
        String toSign = mUserId + APP_KEY + mSigningSequence + APP_SECRET;
        String signature;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] hash = messageDigest.digest(toSign.getBytes("UTF-8"));
            signature = Base64.encodeToString(hash, Base64.DEFAULT).trim();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }

        clientRegistration.register(signature, mSigningSequence++);
    }

    @Override
    public void onUserRegistered() {
        // Instance is registered, but we'll wait for another callback, assuring that the push token is
        // registered as well, meaning we can receive incoming calls.
    }

    @Override
    public void onUserRegistrationFailed(SinchError sinchError) {
        dismissSpinner();
        Toast.makeText(this, "Registration failed!", Toast.LENGTH_LONG).show();
    }
}
