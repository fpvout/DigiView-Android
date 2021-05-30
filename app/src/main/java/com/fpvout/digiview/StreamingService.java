package com.fpvout.digiview;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.pedro.rtplibrary.base.DisplayBase;
import com.pedro.rtplibrary.rtmp.RtmpDisplay;

import net.ossrs.rtmp.ConnectCheckerRtmp;

public class StreamingService extends Service {
    private static final String TAG = "Streaming-Service";
    private static NotificationManager notificationManager;
    private static SharedPreferences sharedPreferences;
    private static final String channelId = "streamingServiceNotification";
    private static int notificationId = 12345;
    private static Context appContext;
    private static Intent mediaProjectionData;
    private static int mediaProjectionResultCode;
    private static DisplayBase rtmpDisplayBase;
    private String endpoint;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "Create");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        keepAliveTrick();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Start");
        endpoint = String.format("%s/%s", sharedPreferences.getString("RtmpUrl", null), sharedPreferences.getString("RtmpKey", null));

        prepareStreaming();
        startStreaming();

        return START_STICKY;
    }

    private void prepareStreaming() {
        stopStreaming();
        rtmpDisplayBase = new RtmpDisplay(appContext, true, (ConnectCheckerRtmp) appContext);
        rtmpDisplayBase.setIntentResult(mediaProjectionResultCode, mediaProjectionData);
    }

    private void startStreaming() {
        if (!rtmpDisplayBase.isStreaming()) {
            if (rtmpDisplayBase.prepareVideo(
                    Integer.parseInt(sharedPreferences.getString("OutputWidth", "1280")),
                    Integer.parseInt(sharedPreferences.getString("OutputHeight", "720")),
                    Integer.parseInt(sharedPreferences.getString("OutputFramerate", "60")),
                    Integer.parseInt(sharedPreferences.getString("OutputBitrate", "1200")) * 1024,
                    0,
                    320
            ) && rtmpDisplayBase.prepareAudio()) {
                rtmpDisplayBase.startStream(endpoint);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopStreaming();
    }

    private void keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setOngoing(true)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
        } else {
            startForeground(1, new Notification());
        }
    }

    public static boolean isStreaming() {
        return rtmpDisplayBase != null && rtmpDisplayBase.isStreaming();
    }

    public static void init(Context context) {
        appContext = context;
        if (rtmpDisplayBase == null) {
            rtmpDisplayBase = new RtmpDisplay(context, true, (ConnectCheckerRtmp) context);
        }
    }

    public static void setMediaProjectionData(int resultCode, Intent data) {
        mediaProjectionResultCode = resultCode;
        mediaProjectionData = data;
    }

    public static Intent sendIntent() {
        if (rtmpDisplayBase != null) {
            return rtmpDisplayBase.sendIntent();
        }

        return null;
    }

    public static void stopStreaming() {
        if (StreamingService.isStreaming()) {
            rtmpDisplayBase.stopStream();
        }
    }
}
