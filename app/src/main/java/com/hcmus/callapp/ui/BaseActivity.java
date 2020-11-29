package com.hcmus.callapp.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hcmus.callapp.services.SinchService;

public abstract class BaseActivity extends AppCompatActivity implements ServiceConnection {
    private SinchService.SinchServiceInterface _SinchServiceInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationContext()
                .bindService(
                        new Intent(this, SinchService.class),
                        this,BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (SinchService.class.getName().equals(componentName.getClassName())){
            _SinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
            onServiceConnected();
        }
    }

    protected void onServiceConnected() {

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (SinchService.class.getName().equals(componentName.getClassName())){
            _SinchServiceInterface = null;
            onServiceDisconnected();
        }
    }

    protected void onServiceDisconnected() {

    }

    protected SinchService.SinchServiceInterface getSinchServiceInterface(){
        return _SinchServiceInterface;
    }

}
