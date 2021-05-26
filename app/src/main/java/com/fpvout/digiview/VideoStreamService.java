package com.fpvout.digiview;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Consume an inputStream and make it available to all the listeners
 */
public class VideoStreamService {

    private static final int READ_BUFFER_SIZE = 131072;
    private static final String TAG = "VideoStreamService";
    private final ArrayList<VideoStreamListener> videoStreamListeners;
    InputStream videoStream;
    private boolean isRunning = false;
    private Thread streamServiceThread;


    public VideoStreamService(InputStream inputStream) {
        videoStream = inputStream;
        videoStreamListeners = new ArrayList<>();
    }

    public void start() {
        Log.d(TAG, "streamServiceThread started");
        if (!isRunning) {
            isRunning = true;
            streamServiceThread = new Thread(() -> {
                while (isRunning) {
                    try {
                        byte[] buffer = new byte[READ_BUFFER_SIZE];
                        int bytesReceived = videoStream.read(buffer, 0, READ_BUFFER_SIZE);
                        if (bytesReceived >= 0) {
                            Log.d(TAG, "bytesReceived : " + bytesReceived);
                            for (VideoStreamListener v : videoStreamListeners) {
                                v.onVideoStreamData(buffer, bytesReceived);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            streamServiceThread.start();
        }

    }

    public void stop() throws InterruptedException {
        Log.d(TAG, "streamServiceThread stopped");
        isRunning = false;
        streamServiceThread.join();
    }

    public void addVideoStreamListener(VideoStreamListener listener) {
        Log.d(TAG, "addVideoStreamListener");
        videoStreamListeners.add(listener);
    }

    public void removeVideoStreamListener(VideoStreamListener listener) {
        Log.d(TAG, "removeVideoStreamListener");
        videoStreamListeners.remove(listener);
    }

    public interface VideoStreamListener {
        void onVideoStreamData(byte[] buffer, int bytesReceived);
    }
}
