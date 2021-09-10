package com.fpvout.digiview;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Consume an inputStream and make it available to all the listeners
 */
public class VideoStreamService {

    private static final int READ_BUFFER_SIZE = 131072;
    private static final int MAX_VIDEO_STREAM_LISTENER = 10;
    private static final String TAG = "VideoStreamService";
    private final ArrayList<VideoStreamListener> videoStreamListeners;
    InputStream videoStream;
    private final byte[] magicPacket = "RMVT".getBytes();
    private boolean isRunning = false;
    private Thread streamServiceThread;
    OutputStream outStream;

    public VideoStreamService(InputStream inputStream, OutputStream outputStream) {
        videoStream = inputStream;
        outStream = outputStream;
        videoStreamListeners = new ArrayList<>();
    }

    public void start() {
        Log.d(TAG, "streamServiceThread started");
        if (!isRunning) {
            isRunning = true;
            streamServiceThread = new Thread(() -> {
                while (isRunning) {
                    try {
                        outStream.write(magicPacket);
                        byte[] buffer = new byte[READ_BUFFER_SIZE];
                        int bytesReceived = videoStream.read(buffer, 0, READ_BUFFER_SIZE);
                        if (bytesReceived >= 0) {
                            Log.d(TAG, "bytesReceived : " + bytesReceived);
                            for (VideoStreamListener v : videoStreamListeners) {
                                v.onVideoStreamData(buffer, bytesReceived);
                            }
                        } else {
                            outStream.write(magicPacket);
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
        if (videoStreamListeners.size() < MAX_VIDEO_STREAM_LISTENER) {
            Log.d(TAG, "addVideoStreamListener");
            videoStreamListeners.add(listener);
        } else {
            Log.d(TAG, "addVideoStreamListener: Limit reached !");
        }
    }

    public void removeVideoStreamListener(VideoStreamListener listener) {
        Log.d(TAG, "removeVideoStreamListener");
        videoStreamListeners.remove(listener);
    }

    public interface VideoStreamListener {
        void onVideoStreamData(byte[] buffer, int bytesReceived);
    }
}
