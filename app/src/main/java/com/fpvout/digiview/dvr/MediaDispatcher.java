package com.fpvout.digiview.dvr;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;

import androidx.annotation.Nullable;

public class MediaDispatcher implements DataSource {
    public static final String LOG_TAG = "MediaDispatcher";
    private DataSpec mDataSpec;
    private String mBuffer;

    @Override
    public void addTransferListener(TransferListener transferListener) {
        Log.d(LOG_TAG, "addTransferListener");
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Log.d(LOG_TAG, "open");

        mDataSpec = dataSpec;
        return Long.MAX_VALUE;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        Log.d(LOG_TAG, "read, readLength=" + readLength);

        if (0 == readLength) return 0;

        if (readLength > mBuffer.length()) buffer = mBuffer.getBytes();

        return mBuffer.length();
    }

    public void write(String buffer) throws InterruptedException {
        //My service calls this
        mBuffer = buffer;
    }

    @Nullable
    @Override
    public Uri getUri() {
        Log.d(LOG_TAG, "getUri");
        return Uri.EMPTY;
    }

    @Override
    public void close() throws IOException {
        Log.d(LOG_TAG, "close");
    }
}