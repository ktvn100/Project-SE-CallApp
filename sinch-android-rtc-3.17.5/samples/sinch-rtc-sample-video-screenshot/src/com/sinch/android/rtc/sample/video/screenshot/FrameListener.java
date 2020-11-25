package com.sinch.android.rtc.sample.video.screenshot;


import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.util.Log;

import com.sinch.android.rtc.video.VideoFrame;
import com.sinch.android.rtc.video.VideoUtils;
import com.sinch.android.rtc.video.RemoteVideoFrameListener;

public class FrameListener implements RemoteVideoFrameListener {
    private static final String TAG = RemoteVideoFrameListener.class.getSimpleName();
    private String mCallId;
    private boolean triggerTakeScreenshot = false;
    private Context context;

    public FrameListener(Context context) {
        this.context = context;
    }

    @Override
    public synchronized void onFrame(String callId, VideoFrame videoFrame) {
        mCallId = callId;
        if (triggerTakeScreenshot == true) {
            triggerTakeScreenshot = false;
            VideoFrame nv21Frame = VideoUtils.I420toNV21Frame(videoFrame);
            YuvImage image = new YuvImage(nv21Frame.yuvPlanes()[0].array(),
                    ImageFormat.NV21,
                    nv21Frame.width(),
                    nv21Frame.height(),
                    nv21Frame.yuvStrides());
            Log.i(TAG, "Saving a screenshot.");
            new SaveScreenshotTask().execute(new SaveScreenshotTask.ScreenShotParams(mCallId, image, videoFrame.getRotation(), context));
        }
    }

    public synchronized void takeScreenshot() {
        triggerTakeScreenshot = true;
    }
}
