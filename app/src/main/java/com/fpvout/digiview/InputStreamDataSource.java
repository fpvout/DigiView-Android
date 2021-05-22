package com.fpvout.digiview;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamDataSource implements DataSource {
    private Context context;
    private DataSpec dataSpec;
    private InputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    public InputStreamDataSource(Context context, DataSpec dataSpec, InputStream inputStream) {
        this.context = context;
        this.dataSpec = dataSpec;
        this.inputStream = inputStream;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {

    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            long skipped = inputStream.skip(dataSpec.position);
            if (skipped < dataSpec.position)
                throw new EOFException();

            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesRemaining = dataSpec.length;
            } else {
                bytesRemaining = C.LENGTH_UNSET;
            }
        } catch (IOException e) {
            throw new IOException(e);
        }

        opened = true;
        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return inputStream.read(buffer, offset, readLength);
    }

    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            inputStream = null;
            if (opened) {
                opened = false;
            }
        }
    }

}