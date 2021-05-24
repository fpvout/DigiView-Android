package com.fpvout.digiview.dvr;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.fpvout.digiview.R;
import com.fpvout.digiview.VideoReaderExoplayer;
import com.fpvout.digiview.helpers.StreamDumper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DVR {
    private static final int WRITE_EXTERNAL_STORAGE = 0;
    private final Activity activity;
    private boolean recordAmbientAudio;
    private MediaRecorder recorder;
    private boolean recording = false;
    private static DVR instance;
    private static final String DVR_LOG_TAG = "DVR";
    private String defaultFolder = "";
    private String ambietAudio;
    private String videoFile;
    private String dvrFile;
    private VideoReaderExoplayer mPlayer;
    private File dvrTmpFile;
    private String fileName;
    private StreamDumper streamDumper;
    private static Handler updateAfterRecord;
    public static final String LATEST_THUMB_FILE = "latest.jpeg";

    DVR(Activity activity, boolean recordAmbientAudio, Handler updateAfterRecord){
        this.activity = activity;
        this.recordAmbientAudio = recordAmbientAudio;
        this.updateAfterRecord = updateAfterRecord;
        defaultFolder =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + this.activity.getApplicationInfo().loadLabel(this.activity.getPackageManager()).toString();
        streamDumper = new StreamDumper(activity, defaultFolder);
    }

    public static DVR getInstance(Activity context, boolean recordAmbientAudio, Handler updateAfterRecord){
        if (instance == null) {
            instance = new DVR(context, recordAmbientAudio, updateAfterRecord);
        }
        return instance;
    }

    public void init(VideoReaderExoplayer mPlayer) throws IOException {
        this.mPlayer = mPlayer;

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    public void recordVideoDVR(byte[] buffer, int offset, int readLength) {
        if (isRecording()) {
            streamDumper.dump( buffer,  offset, readLength);
        }
    }

    public void start() {
        if ( mPlayer.isStreaming()) {
            this.recording = true;
            fileName = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
                    .format(Calendar.getInstance().getTime());
            ambietAudio = "/DigiView_" + fileName + ".aac";
            videoFile ="/DigiView_"+fileName+".h264";
            dvrFile = "/DigiView_" + fileName + ".mp4";

            Log.d(DVR_LOG_TAG, "creating folder for dvr saving ...");
            File objFolder = new File(defaultFolder);
            if (!objFolder.exists())
                objFolder.mkdir();


            Log.d(DVR_LOG_TAG, "start recording ...");
            streamDumper.init(videoFile, ambietAudio, dvrFile);

            Toast.makeText(activity, activity.getText(R.string.recording_started), Toast.LENGTH_LONG).show();
            ((ImageButton) activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.stop);



            if (recordAmbientAudio) {
                Log.d(DVR_LOG_TAG, "starting ambient recording ...");
                recorder.setOutputFile(defaultFolder + ambietAudio);
                try {
                    recorder.prepare();
                    recorder.start();   // Ambient Audio Recording is now started
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(activity, "Stream not ready", Toast.LENGTH_LONG).show();
        }
    }

    public String getLatestThumbFile() {
        return defaultFolder + "/" + LATEST_THUMB_FILE;
    }

    public boolean isRecording(){
        return recording;
    }

    public void stop() {
        Log.d(DVR_LOG_TAG, "stop recording ...");
        Toast.makeText(activity, activity.getText(R.string.recording_stopped), Toast.LENGTH_LONG).show();
        ((ImageButton) activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.record);

        if (recordAmbientAudio) {
            recorder.stop();
            try {
                init(mPlayer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        streamDumper.stop(updateAfterRecord);
        this.recording = false;
    }

    public File getDefaultFolder() {
        return new File(defaultFolder);
    }
}