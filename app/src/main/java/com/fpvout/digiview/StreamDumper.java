package com.fpvout.digiview;

import android.os.Environment;

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

    public boolean dumpStream = true;

    public StreamDumper(){
        dumpDir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES),
                "DigiView");
        dumpDir.mkdir();

        init();
    }

    public void dump(byte[] buffer, int offset, int receivedBytes) {

        if(fos == null) init();

        try {
            fos.write(buffer, offset, receivedBytes);
            bytesWritten = true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void init() {
        try {
            streamDump = new File(dumpDir, "DigiView-"+System.currentTimeMillis()+".h264");
            fos = new FileOutputStream(streamDump);
            bytesWritten = false;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void stop() {
        try {
            if(fos != null){
                fos.flush();
                fos.close();

                if(bytesWritten) {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
                            .format(Calendar.getInstance().getTime());
                    File out = new File(dumpDir, "DigiView "+timestamp+".mp4");
                    new Mp4Muxer(streamDump, out).start();
                }
            }
            if(!bytesWritten){
                streamDump.delete();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
