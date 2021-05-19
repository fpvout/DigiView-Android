package com.fpvout.digiview.dvr;

import android.Manifest;
import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.fpvout.digiview.InputStreamDataSource;
import com.fpvout.digiview.R;
import com.fpvout.digiview.VideoReaderExoplayer;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import androidx.core.app.ActivityCompat;
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFtask;

public class DVR {
    private static final int WRITE_EXTERNAL_STORAGE = 0;
    private final Activity _activity;
    private boolean _recordAmbientAudio;
    private MediaRecorder _recorder;
    private boolean recording = false;
    private static DVR instance;
    private static final String DVR_LOG_TAG = "DVR";
    private String defaultFolder = "";
    private FFmpeg _ffmpeg;
    private FFtask _ffTask;
    private String _ambietAudio;
    private String _videoFile;
    private String _dvrFile;
    private VideoReaderExoplayer _mPlayer;

    DVR(Activity activity,VideoReaderExoplayer mPlayer,  boolean recordAmbientAudio){
        _activity = activity;
        _mPlayer = mPlayer;
        _recordAmbientAudio = recordAmbientAudio;
        defaultFolder = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" +_activity.getApplicationInfo().loadLabel(_activity.getPackageManager()).toString();
        ActivityCompat.requestPermissions(_activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA }, WRITE_EXTERNAL_STORAGE);

    }

    public static DVR getInstance(Activity context,VideoReaderExoplayer mPlayer, boolean recordAmbientAudio){
        if (instance == null) {
            instance = new DVR(context,mPlayer, recordAmbientAudio);
        }
        return instance;
    }

    public void init() throws IOException {
        _ffmpeg = FFmpeg.getInstance(_activity);

        _recorder = new MediaRecorder();
        _recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        _recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        _recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    public void start() throws IOException {
        Log.d(DVR_LOG_TAG, "start recording ...");
        Toast.makeText(_activity, _activity.getText(R.string.recording_started), Toast.LENGTH_LONG).show();
        ((ImageButton)_activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.stop);
        this.recording = true;
        String fileName = String.valueOf(new Date().getTime());
        _ambietAudio = defaultFolder + "tmp_" + fileName + ".mp3";
        _videoFile = defaultFolder + "/tmp_" + fileName + ".mp4";
        _dvrFile = defaultFolder + "/DVR_" + fileName + ".mp4";

        File objFolder = new File(defaultFolder);
        if(!objFolder.exists()){
            objFolder.mkdir();
        }

        if (_recordAmbientAudio) {
            _recorder.setOutputFile(_ambietAudio);
            _recorder.prepare();
            _recorder.start();   // Ambient Audio Recording is now started
        }
        InputStreamDataSource inputStreamDataSource =  _mPlayer.getInputDataStream();
        if (inputStreamDataSource != null) {

        }

        if (_ffmpeg.isSupported()) {


            String RTSP_URL = "rtsp://<your_rtsp_url>";
            String[] ffmpegCommand = new String[]{  "-i", RTSP_URL, "-acodec", "copy", "-vcodec", "copy", _videoFile};
            _ffTask = _ffmpeg.execute( ffmpegCommand, new FFcommandExecuteResponseHandler() {
                @Override
                public void onStart() {}

                @Override
                public void onProgress(String message) {}

                @Override
                public void onFailure(String message) {}

                @Override
                public void onSuccess(String message) {}

                @Override
                public void onFinish() {
                    if (new File(_videoFile).exists() && new File(_ambietAudio).exists()) {
                        Toast.makeText(_activity, _activity.getString(R.string.dvr_merge_audio_video), Toast.LENGTH_LONG).show();
                        _ffmpeg.execute(new String[]{"-i", _videoFile, "-i", _ambietAudio, " -shortest", _dvrFile}, new FFcommandExecuteResponseHandler() {
                            @Override
                            public void onStart() {
                            }

                            @Override
                            public void onProgress(String message) {
                            }

                            @Override
                            public void onFailure(String message) {
                            }

                            @Override
                            public void onSuccess(String message) {
                            }

                            @Override
                            public void onFinish() {
                                Toast.makeText(_activity, _activity.getString(R.string.saved_dvr), Toast.LENGTH_LONG).show();
                            }

                        });
                    }
                }

            } );
        }
    }

    public boolean isRecording(){
        return recording;
    }

    public void stop() {
        Log.d(DVR_LOG_TAG, "stop recording ...");
        Toast.makeText(_activity, _activity.getText(R.string.recording_stopped), Toast.LENGTH_LONG).show();
        ((ImageButton)_activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.record);
        if (_recordAmbientAudio) {
            _recorder.stop();
        }
        _ffTask.sendQuitSignal();

        this.recording = false;
    }
}