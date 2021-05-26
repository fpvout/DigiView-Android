package com.fpvout.digiview.helpers;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StreamDumper  {

    private FileOutputStream fos;
    private boolean bytesWritten = false;

    private final File dumpDir;
    private File streamDump;
    private File streamAmbient;
    private File outFile;
    private final Context context;

    public StreamDumper(Context context, String defaultPath){
        this.context = context;
        dumpDir = new File(defaultPath);

        dumpDir.mkdirs();
    }

    public void dump(byte[] buffer, int offset, int receivedBytes) {

        try {
            fos.write(buffer, offset, receivedBytes);
            bytesWritten = true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void init(String videoFileName, String ambientAudioFileName, String outFileFileName) {
        try {
            streamDump = new File(dumpDir, videoFileName);
            streamAmbient = new File(dumpDir, ambientAudioFileName);
            outFile = new File(dumpDir, outFileFileName);
            fos = new FileOutputStream(streamDump);
            bytesWritten = false;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void stop(Handler completeHandler) {
        try {
            if(fos != null){
                fos.flush();
                fos.close();

                if(bytesWritten) {
                    new Mp4Muxer(this.context, dumpDir, streamDump, streamAmbient,outFile, true).start();
                }
            }
            if(!bytesWritten){
                streamDump.delete();
            }
            completeHandler.sendEmptyMessage(0);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
