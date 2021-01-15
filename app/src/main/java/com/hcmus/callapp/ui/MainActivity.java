package com.hcmus.callapp.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hcmus.callapp.R;
import com.hcmus.callapp.receivers.NetworkChangeReceiver;
import com.hcmus.callapp.services.SinchService;
import com.sinch.android.rtc.SinchError;

import butterknife.BindView;
import timber.log.Timber;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends BaseActivity implements SinchService.StartFailedListened {

    private String mSinchId = "b";
    private String mOriginalCaller;

    private NetworkChangeReceiver networkChangeReceiver;
    private BroadcastReceiver mDialogReceiver;
    private AlertDialog mInternetDialog;
    private BroadcastReceiver finishReceiver;
    private SinchService.SinchServiceInterface mSinchServiceInterface;

    @BindView(R.id.findButton)
    Button _btn_find;

    CallAction callAction;
    SettingButton settingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handlePermissions();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("ACTION_TRIGGER_RECEIVER");

        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, filter);

        Timber.plant(new Timber.DebugTree());

        // check for theme mode
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean dark_mode = sharedPreferences.getBoolean("themeMode", false);
        if (dark_mode)
            setContentView(R.layout.activity_main_dark);
        else
            setContentView(R.layout.activity_main);

        mSinchServiceInterface = getSinchServiceInterface();

        callAction = new CallAction(this);

        settingButton = new SettingButton(this, R.id.settingBtn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createDialogReceiver();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getBooleanExtra("EXIT", false)) {
            finish();
        }
    }

    private void createDialogReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_NO_INTERNET");
        filter.addAction("ACTION_INTERNET_AVAILABLE");

        mDialogReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("ACTION_NO_INTERNET")) {

                    if (mInternetDialog == null || !mInternetDialog.isShowing()) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.no_internet)
                                .setMessage("The app cannot function without an internet connection")
                                .setCancelable(false)
                                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Intent intent = new Intent("ACTION_TRIGGER_RECEIVER");
                                        sendBroadcast(intent);
                                    }
                                })
                                .setNegativeButton("Close App", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        intent.putExtra("EXIT", true);
                                        startActivity(intent);
                                    }
                                });
                        mInternetDialog = builder.create();
                        mInternetDialog.show();
                    }
                } else {
                    if (mInternetDialog != null && mInternetDialog.isShowing()) {
                        mInternetDialog.dismiss();
                        Toast.makeText(context, "Internet access restored", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        registerReceiver(mDialogReceiver, filter);
    }

    private void handlePermissions() {

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mDialogReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.uprootAll();
        unregisterReceiver(networkChangeReceiver);

    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, "An error has occured.Please restart the app", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStarted() {
    }

    @Override
    protected void onServiceConnected() {

        Timber.d("onServiceConnected");
        getSinchServiceInterface().setStartListener(this);

        //Register user
        if (getSinchServiceInterface() != null && !getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(mSinchId);
        }

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permission succesfully granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Cannot function without microphone access", Toast
                    .LENGTH_LONG).show();
            finish();
        }
    }

    private void openCallingActivity() {
        Intent intent = new Intent(this, CallingActivity.class);
        startActivity(intent);
    }
}