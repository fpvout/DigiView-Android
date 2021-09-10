package com.fpvout.digiview;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import usb.CircularByteBuffer;

public class VideoStreamServiceDataSource implements DataSource {
    private static final int READ_BUFFER_SIZE = 50 * 1024 * 1024;
    private static final long READ_TIMEOUT = 200;

    private final DataSpec dataSpec;
    private final CircularByteBuffer readBuffer;
    private boolean opened;

    public VideoStreamServiceDataSource(DataSpec dataSpec) {
        this.dataSpec = dataSpec;
        readBuffer = new CircularByteBuffer(READ_BUFFER_SIZE);
    }

    public VideoStreamServiceDataSource(DataSpec dataSpec, VideoStreamService v) {
        this.dataSpec = dataSpec;
        readBuffer = new CircularByteBuffer(READ_BUFFER_SIZE);
        v.addVideoStreamListener(this::onVideoStreamData);
    }

    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {

    }

    @Override
    public long open(DataSpec dataSpec) {
        long bytesRemaining;

        if (dataSpec.length != C.LENGTH_UNSET) {
            bytesRemaining = dataSpec.length;
        } else {
            bytesRemaining = C.LENGTH_UNSET;
        }

        opened = true;
        return bytesRemaining;
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int readLength) {
        long deadLine = System.currentTimeMillis() + READ_TIMEOUT;
        int readBytes = 0;
        while (System.currentTimeMillis() < deadLine && readBytes <= 0)
            readBytes = readBuffer.read(buffer, offset, readLength);
        return readBytes;
    }

    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void close() {
        if (opened) {
            opened = false;
        }

    }

    public void onVideoStreamData(byte[] buffer, int receivedBytes) {
        if (receivedBytes > 0) {
            readBuffer.write(buffer, 0, receivedBytes);
            Log.d("onVideoStream", "onVideoStreamData: " + receivedBytes);
        }
    }
}