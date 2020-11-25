package com.sinch.android.rtc.sample.video.screenshot;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.os.Environment.DIRECTORY_DCIM;

public class SaveScreenshotTask extends AsyncTask<SaveScreenshotTask.ScreenShotParams, Void, String>{
    public static final String CHANNEL_ID = "Sinch_screenshot_notification_channel_id";
    private static final String TAG = "SaveScreenshotTask";
    Context context;

    public static class ScreenShotParams {
        public String callId;
        public YuvImage image;
        public int rotationDegreesCW;
        public Context context;
        public ScreenShotParams(String callId, YuvImage image, int rotationDegreesCW, Context context) {
            this.callId = callId;
            this.image = image;
            this.rotationDegreesCW = rotationDegreesCW;
            this.context = context;
        }
    }

    @Override
    protected String doInBackground(ScreenShotParams... params) {
        YuvImage image = params[0].image;
        String callId = params[0].callId;
        int rotationDegreesCW = params[0].rotationDegreesCW;
        context = params[0].context;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);

        if (rotationDegreesCW != 0) {
            BitmapFactory.Options opt;
            opt = new BitmapFactory.Options();
            Matrix matrix = new Matrix();
            byte[] compressedData = out.toByteArray();
            matrix.postRotate(rotationDegreesCW);
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length, opt);
            Bitmap rotatedBitmap  = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            out.reset();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        }

        StringBuilder pathString = new StringBuilder();
        pathString.append(Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath());
        pathString.append("/");
        pathString.append(callId);
        pathString.append("-screenshot.jpg");

        File path = new File(pathString.toString());
        FileOutputStream file;
        String result = null;
        try {
            if (!path.exists()) {
                path.createNewFile();
            }
            file = new FileOutputStream(path);
            out.writeTo(file);
            result = pathString.toString();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not find output file.");
        } catch (IOException e) {
            Log.e(TAG, "Exception saving the screenshot to a file " + e);
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null) {
            Toast.makeText(context, "Couldn't take screenshot of video!", Toast.LENGTH_SHORT).show();
        } else {
            createNotification(result);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Sinch";
            String description = "Incoming Sinch Push Notifications.";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createNotification(String pathToFile) {
        createNotificationChannel();

        Uri apkURI = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", new File(pathToFile));

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(apkURI, "image/*");

        Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Screenshot taken!")
                        .setLargeIcon(bitmap);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }
}
