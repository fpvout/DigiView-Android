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
import android.util.DisplayMetrics;
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
    private static Context appContext;
    private static ConnectCheckerRtmp connectChecker;
    private static Intent mediaProjectionData;
    private static int mediaProjectionResultCode;
    private static DisplayBase rtmpDisplayBase;
    private static int dpi;
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
        rtmpDisplayBase = new RtmpDisplay(appContext, true, connectChecker);
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
                    dpi
            )) {
                Log.i(TAG, String.valueOf(Integer.parseInt(sharedPreferences.getString("OutputFramerate", "60"))));
                boolean audioInitialized;
                if (sharedPreferences.getString("AudioSource", "0").equals("internal") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    audioInitialized = rtmpDisplayBase.prepareInternalAudio(64 * 1024, 32000, true, false, false);
                } else {
                    audioInitialized = rtmpDisplayBase.prepareAudio(Integer.parseInt(sharedPreferences.getString("AudioSource", "0")), 64 * 1024, 32000, false, false, false);
                }

                if (audioInitialized) {
                    if (!sharedPreferences.getBoolean("RecordAudio", true)) {
                        rtmpDisplayBase.disableAudio();
                    } else {
                        rtmpDisplayBase.enableAudio();
                    }

                    rtmpDisplayBase.startStream(endpoint);
                }
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

    public static boolean isMuted() {
        return rtmpDisplayBase != null && rtmpDisplayBase.isAudioMuted();
    }

    public static void toggleMute() {
        if (isStreaming()) {
            if (rtmpDisplayBase.isAudioMuted()) {
                rtmpDisplayBase.enableAudio();
            } else {
                rtmpDisplayBase.disableAudio();
            }
        }
    }

    public static void init(Context context, ConnectCheckerRtmp connectCheckerRtmp) {
        appContext = context;
        connectChecker = connectCheckerRtmp;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        dpi = dm.densityDpi;

        if (rtmpDisplayBase == null) {
            rtmpDisplayBase = new RtmpDisplay(appContext, true, connectChecker);
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
