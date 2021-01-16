package com.hcmus.callapp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import timber.log.Timber;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static android.content.ContentValues.TAG;

public class MainActivity extends BaseActivity implements SinchService.StartFailedListened {

    private String mSinchId;
    private String mOriginalCaller;

    private NetworkChangeReceiver networkChangeReceiver;
    private BroadcastReceiver mDialogReceiver;
    private AlertDialog mInternetDialog;
    private BroadcastReceiver finishReceiver;
    private SinchService.SinchServiceInterface mSinchServiceInterface;
    private static final String SHARED_PREFS_KEY = "shared_prefs";
    private static final String SINCH_ID_KEY = "sinch_id";

    @BindView(R.id.findButton)
    Button _btn_find;

    CallAction callAction;
    SettingButton settingButton;

    private Button button;
    private ProgressBar progressBar;
    private Chronometer chronometer;
    private Activity activity;
    private static final String CALLERID_DATA_KEY = "callerId";
    static int WAITING = 0;
    static int FREE = 1;
    private int state = FREE;

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

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        mSinchId = prefs.getString(SINCH_ID_KEY, null);

        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, filter);

        finishReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("finish_waitingcallactivity")) {
                    finish();
                }
            }
        };
        registerReceiver(finishReceiver, new IntentFilter("finish_waitingcallactivity"));

        Timber.plant(new Timber.DebugTree());

        // check for theme mode
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean dark_mode = sharedPreferences.getBoolean("themeMode", false);
        if (dark_mode)
            setContentView(R.layout.activity_main_dark);
        else
            setContentView(R.layout.activity_main);

        //sda
        Log.d(TAG, "CallAction: assign button");
        button = (Button) findViewById(R.id.findButton);
        Log.d(TAG, "CallAction: assign progressbar");
        progressBar = (ProgressBar)findViewById(R.id.progress_circular);
        Log.d(TAG, "CallAction: assign chrono");
        chronometer = (Chronometer)findViewById(R.id.waitingChrono);
        chronometer.stop();
        Log.d(TAG, "CallAction: setclicklistener");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickingButton();
            }
        });
        //áda

        settingButton = new SettingButton(this, R.id.settingBtn);

    }

    public void ClickingButton(){
        if (state == FREE)
            GoActive();
        else GoUnActive();
    }

    private void GoUnActive() {
        state = FREE;
        button.setText("GO");
        progressBar.setVisibility(View.INVISIBLE);
        chronometer.setVisibility(View.INVISIBLE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
    }

    private void GoActive() {
        state = WAITING;
        button.setText("WAIT");
        progressBar.setVisibility(View.VISIBLE);
        chronometer.setVisibility(View.VISIBLE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        state = WAITING;
        // Bat dau tim kiem
        //findingMate();
        makeCall();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 36000);
        //GoUnActive();
    }

    private void findingMate() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // Actions to do after 5 seconds
                if (state == FREE) return;
                Intent intent = new Intent(activity, CallingActivity.class);
                GoUnActive();
                activity.startActivity(intent);
            }
        }, 2000);
    }

    private void makeCall() {
        String ID = mSinchId;
        Log.d("ID: ", ID);

        DatabaseReference _DBRef = FirebaseDatabase.getInstance().getReference("users");
        _DBRef.child(ID).child("status").setValue("1");
        _DBRef.child(ID).child("call_request").setValue("true");

        _DBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int size = (int)snapshot.getChildrenCount();
                Log.d("Size: ", String.valueOf(size));

                User user = null, curUser = null;

                for(DataSnapshot dss:snapshot.getChildren()){
                    String status = dss.child("status").getValue().toString();
                    String androidID = dss.child("androidID").getValue().toString();
                    String username = dss.child("username").getValue().toString();
                    String call_request = dss.child("call_request").getValue().toString();
                    //User tempUser = new User(status,androidID,username);


                    if (androidID.equals(ID)){
                        curUser = new User(status,androidID,username,call_request);
                        Log.d("ID: ", androidID);
                    } else {
                        if (status.equals("1") && call_request.equals("true")){
                            user = new User(status,androidID,username,call_request);
                            Log.d("ID: ", androidID);
                        }
                    }
                }
                /*if (!checkConnected) {
                    justWaiting();
                } else {
                    callUser(user,curUser);
                }*/
                if (user != null){
                    callUser(user,curUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
            }
        });
    }

    private void callUser(User user, User curUser) {
        Intent intent = new Intent(this, CallingActivity.class);
        //intent.putExtra("User",user);
        //intent.putExtra("CurUser",curUser);
        intent.putExtra(CALLERID_DATA_KEY, user.androidID);
        finish();
        startActivity(intent);
    }

    private void getCallAction() {
        callAction = new CallAction(this);
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