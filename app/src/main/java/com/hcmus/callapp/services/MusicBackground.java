package com.hcmus.callapp.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.hcmus.callapp.R;

import java.io.IOException;

public class MusicBackground extends Service {
    private MediaPlayer player;
    private final int maxVolume = 50;
    private int currVolume = 10;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        player = new MediaPlayer();
        AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.stardew);
        try {
            player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setLooping(true);
        // set volumne
        float log1=(float)(Math.log(maxVolume-currVolume)/Math.log(maxVolume));
        player.setVolume(log1,log1);
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        player.stop();
    }
}
