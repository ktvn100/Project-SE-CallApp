package com.hcmus.callapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hcmus.callapp.R;
import com.hcmus.callapp.model.User;

import static android.content.ContentValues.TAG;

public class CallAction {
    private Button button;
    private ProgressBar progressBar;
    private Chronometer chronometer;
    private Activity activity;
    static int WAITING = 0;
    static int FREE = 1;
    private int state = FREE;
    private static final String CALLERID_DATA_KEY = "callerId";
    public CallAction(Activity activity)
    {
        Log.d(TAG, "CallAction: activity");
        this.activity = activity;
        Log.d(TAG, "CallAction: assign button");
        button = (Button) this.activity.findViewById(R.id.findButton);
        Log.d(TAG, "CallAction: assign progressbar");
        progressBar = (ProgressBar)this.activity.findViewById(R.id.progress_circular);
        Log.d(TAG, "CallAction: assign chrono");
        chronometer = (Chronometer) this.activity.findViewById(R.id.waitingChrono);
        chronometer.stop();
        Log.d(TAG, "CallAction: setclicklistener");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickingButton();
            }
        });
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
        String ID = Settings.System.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("ID: ", ID);

        DatabaseReference _DBRef = FirebaseDatabase.getInstance().getReference("Users");
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
                //callUser(user,curUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
            }
        });
    }

    private void callUser(User user, User curUser) {
        Intent intent = new Intent(activity, CallingActivity.class);
        intent.putExtra("User",user);
        intent.putExtra("CurUser",curUser);
        intent.putExtra(CALLERID_DATA_KEY, "a");
        //activity.finish();
        activity.startActivity(intent);
    }
}
