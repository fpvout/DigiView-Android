package com.fpvout.digiview;

import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer2.SimpleExoPlayer;

import net.ossrs.rtmp.ConnectCheckerRtmp;

public class RTMPService implements ConnectCheckerRtmp {
    private static final String TAG = "RTMP-Service";
    private String endpoint;
    private ExoPlayerRTMP exoPlayerRTMP;

    public RTMPService(Context context, SimpleExoPlayer exoPlayer, String endpoint) {
        this.endpoint = endpoint;
        this.exoPlayerRTMP = new ExoPlayerRTMP(context, exoPlayer, true, this);
    }

    @Override
    public void onConnectionSuccessRtmp() {
        Log.d(TAG, "onConnectionSuccessRtmp");
    }

    @Override
    public void onConnectionFailedRtmp(String reason) {
        Log.d(TAG, "onConnectionFailedRtmp : " + reason);
    }

    @Override
    public void onNewBitrateRtmp(long bitrate) {
        Log.d(TAG, "onNewBitrateRtmp : " + String.valueOf(bitrate));
    }

    @Override
    public void onDisconnectRtmp() {
        Log.d(TAG, "onDisconnectRtmp");
    }

    @Override
    public void onAuthErrorRtmp() {
        Log.d(TAG, "onAuthErrorRtmp");
    }

    @Override
    public void onAuthSuccessRtmp() {
        Log.d(TAG, "onAuthSuccessRtmp");
    }

    public void stop() {
        if (exoPlayerRTMP != null) {
            if (exoPlayerRTMP.isStreaming()) exoPlayerRTMP.stopStream();
        }
    }

    public void start() {
        if (!exoPlayerRTMP.isStreaming()) {
            if (exoPlayerRTMP.prepareVideo(1280, 720, 60, 2500 * 1024, 0) && exoPlayerRTMP.prepareAudio()) {
                exoPlayerRTMP.startStream(endpoint);
            }
        } else {
            Log.e(TAG, "Already streaming");
        }
    }
}