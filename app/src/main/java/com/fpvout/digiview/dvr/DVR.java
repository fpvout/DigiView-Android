package com.fpvout.digiview.dvr;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.fpvout.digiview.R;
import com.fpvout.digiview.UsbMaskConnection;
import com.fpvout.digiview.helpers.DataListener;
import com.fpvout.digiview.helpers.Mp4Muxer;
import com.fpvout.digiview.helpers.StreamDumper;
import com.fpvout.digiview.helpers.ThreadPerTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DVR {
    private final Activity activity;
    private final boolean recordAmbientAudio;
    private MediaRecorder recorder;
    private boolean recording = false;
    private static DVR instance;
    private static final String DVR_LOG_TAG = "DVR";
    private String defaultFolder = "";
    private final StreamDumper streamDumper;
    private String videoFile;
    private String dvrFile;
    private String fileName;
    private final UsbMaskConnection connection;
    private String ambientAudio;
    private static Handler updateAfterRecord;
    public static final String LATEST_THUMB_FILE = "latest.jpeg";

    DVR(Activity activity, boolean recordAmbientAudio, Handler updateAfterRecord, UsbMaskConnection connection) {
        this.activity = activity;
        this.connection = connection;
        this.recordAmbientAudio = recordAmbientAudio;
        DVR.updateAfterRecord = updateAfterRecord;
        defaultFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + this.activity.getApplicationInfo().loadLabel(this.activity.getPackageManager()).toString();
        streamDumper = new StreamDumper(activity, defaultFolder);
    }

    public static DVR getInstance(Activity context, boolean recordAmbientAudio, Handler updateAfterRecord, UsbMaskConnection connection){
        if (instance == null) {
            instance = new DVR(context, recordAmbientAudio, updateAfterRecord, connection);
        }
        return instance;
    }

    public void init() throws IOException {
        repairNotFinishedDVR();
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    private void  repairNotFinishedDVR(){
        File dvrFolder = new File(defaultFolder);
        if (dvrFolder.exists()) {
            File[] files = dvrFolder.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.getAbsolutePath().endsWith(".h264")) {
                    File ambientAudio = new File(file.getAbsolutePath().replace(".h264", ".aac"));
                    if (ambientAudio.exists()) {
                        File output = new File(file.getAbsolutePath().replace(".h264", ".mp4"));
                        new Mp4Muxer(activity, dvrFolder , file, ambientAudio,output, false).start();
                    }
                }
            }
        }
    }

    public void start() {
        if (connection.getInputStream() != null) {
            Toast.makeText(activity, activity.getText(R.string.recording_started), Toast.LENGTH_LONG).show();
            ((ImageButton) activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.stop);

            ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
            executor.execute(() -> {
                connection.getInputStream().setInputStreamListener(new DataListener() {
                    @Override
                    public void calllback(byte[] buffer, int offset, int length) {
                        if (streamDumper != null) {
                            if (isRecording()) {
                                if (buffer != null) {
                                    streamDumper.dump(buffer, offset, length);
                                }
                            }
                        }
                    }
                });

                fileName = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
                        .format(Calendar.getInstance().getTime());
                ambientAudio = "/DigiView_" + fileName + ".aac";
                videoFile = "/DigiView_" + fileName + ".h264";
                dvrFile = "/DigiView_" + fileName + ".mp4";

                Log.d(DVR_LOG_TAG, "creating folder for dvr saving ...");
                File objFolder = new File(defaultFolder);
                if (!objFolder.exists())
                    objFolder.mkdir();

                Log.d(DVR_LOG_TAG, "start recording ...");
                streamDumper.init(videoFile, ambientAudio, dvrFile);
                if (recordAmbientAudio) {
                    Log.d(DVR_LOG_TAG, "starting ambient recording ...");
                    recorder.setOutputFile(defaultFolder + ambientAudio);
                    try {
                        recorder.prepare();
                        recorder.start();   // Ambient Audio Recording is now started
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //start recording (input stream starts collecting data
                this.recording = true;
            });
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
        this.recording = false;
        Toast.makeText(activity, activity.getText(R.string.recording_stopped), Toast.LENGTH_LONG).show();
        ((ImageButton) activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.record);

        ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        executor.execute(() -> {
            connection.getInputStream().setInputStreamListener(null); //remove listener from raw
            streamDumper.stop(updateAfterRecord);
            if (recordAmbientAudio) {
                recorder.stop();
            }

            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public File getDefaultFolder() {
        return new File(defaultFolder);
    }
}