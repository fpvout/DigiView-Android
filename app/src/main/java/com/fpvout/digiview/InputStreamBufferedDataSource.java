package com.fpvout.digiview;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import usb.CircularByteBuffer;

public class InputStreamBufferedDataSource implements DataSource {
    private static final int READ_BUFFER_SIZE = 50 * 1024 * 1024;
    private static final String ERROR_THREAD_NOT_INITIALIZED = "Read thread not initialized, call first 'startReadThread()'";
    private static final long READ_TIMEOUT = 200;

    private final Context context;
    private final DataSpec dataSpec;
    private InputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    private CircularByteBuffer readBuffer;
    private Thread receiveThread;
    private boolean working;


    public InputStreamBufferedDataSource(Context context, DataSpec dataSpec, InputStream inputStream) {
        this.context = context;
        this.dataSpec = dataSpec;
        this.inputStream = inputStream;
        startReadThread();
    }

    @Override
    public void addTransferListener(@NonNull TransferListener transferListener) {

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
        if (readBuffer == null)
            throw new IOException(ERROR_THREAD_NOT_INITIALIZED);

        long deadLine = System.currentTimeMillis() + READ_TIMEOUT;
        int readBytes = 0;
        while (System.currentTimeMillis() < deadLine && readBytes <= 0)
            readBytes = readBuffer.read(buffer, offset, readLength);
        return readBytes;
    }

    public void startReadThread(){
        if (!working) {
            working = true;
            readBuffer = new CircularByteBuffer(READ_BUFFER_SIZE);
            receiveThread = new Thread() {
                @Override
                public void run() {
                    while (working) {
                        byte[] buffer = new byte[1024];
                        int receivedBytes = 0;
                        try {
                            receivedBytes = inputStream.read(buffer, 0, buffer.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (receivedBytes > 0) {
                            readBuffer.write(buffer, 0, receivedBytes);
                        }
                    }
                }
            };
            receiveThread.start();
        }
    }

    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        working = false;
        if (receiveThread != null){
            receiveThread.interrupt();
        }
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