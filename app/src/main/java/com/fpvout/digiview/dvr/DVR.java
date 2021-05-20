package com.fpvout.digiview.dvr;

import android.Manifest;
import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.fpvout.digiview.InputStreamDataSource;
import com.fpvout.digiview.R;
import com.fpvout.digiview.VideoReaderExoplayer;
import com.fpvout.digiview.helpers.Mp4Muxer;
import com.fpvout.digiview.helpers.ThreadPerTaskExecutor;

import org.mp4parser.Container;
import org.mp4parser.muxer.DataSource;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import androidx.core.app.ActivityCompat;

public class DVR {
    private static final int WRITE_EXTERNAL_STORAGE = 0;
    private final Activity _activity;
    private boolean _recordAmbientAudio;
    private MediaRecorder _recorder;
    private boolean recording = false;
    private static DVR instance;
    private static final String DVR_LOG_TAG = "DVR";
    private String defaultFolder = "";
    private String _ambietAudio;
    private String _videoFile;
    private String _dvrFile;
    private VideoReaderExoplayer _mPlayer;
    private File dvrTmpFile;
    private String fileName;

    DVR(Activity activity,  boolean recordAmbientAudio){
        _activity = activity;
        _recordAmbientAudio = recordAmbientAudio;
        defaultFolder = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" +_activity.getApplicationInfo().loadLabel(_activity.getPackageManager()).toString();
        ActivityCompat.requestPermissions(_activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA }, WRITE_EXTERNAL_STORAGE);

    }

    public static DVR getInstance(Activity context, boolean recordAmbientAudio){
        if (instance == null) {
            instance = new DVR(context, recordAmbientAudio);
        }
        return instance;
    }

    public void init(VideoReaderExoplayer mPlayer) throws IOException {
        this._mPlayer = mPlayer;

        _recorder = new MediaRecorder();
        _recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        _recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        _recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    public void recordVideoDVR(byte[] buffer, int offset, int readLength) {
        if (isRecording()) {
            try {
                if (dvrTmpFile != null) {
                    FileOutputStream dvrOutputStream = new FileOutputStream(dvrTmpFile);
                    dvrOutputStream.write(buffer, offset, readLength);
                    dvrOutputStream.flush();
                    dvrOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if ( _mPlayer.isStreaming()) {
            this.recording = true;
            fileName = String.valueOf(new Date().getTime());
            _ambietAudio = defaultFolder + "/tmp_" + fileName + ".aac";
            _videoFile = defaultFolder + "/tmp_" + fileName + ".h264";
            _dvrFile = defaultFolder + "/DVR_" + fileName + ".mp4";

            Log.d(DVR_LOG_TAG, "start recording ...");
            Toast.makeText(_activity, _activity.getText(R.string.recording_started), Toast.LENGTH_LONG).show();
            ((ImageButton)_activity.findViewById(R.id.recordbt)).setImageResource(R.drawable.stop);

            ThreadPerTaskExecutor threadPerTaskExecutor = new ThreadPerTaskExecutor();
            threadPerTaskExecutor.execute(() -> {
                Log.d(DVR_LOG_TAG, "creating folder for dvr saving ...");
                File objFolder = new File(defaultFolder);
                if(!objFolder.exists()){
                    objFolder.mkdir();
                }

                dvrTmpFile = new File(_videoFile);

                if (_recordAmbientAudio) {
                    Log.d(DVR_LOG_TAG, "starting abient recording ...");
                    _recorder.setOutputFile(_ambietAudio);
                    try {
                        _recorder.prepare();
                        _recorder.start();   // Ambient Audio Recording is now started
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Toast.makeText(_activity, "Stream not ready", Toast.LENGTH_LONG).show();
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

            Toast.makeText(_activity, _activity.getString(R.string.dvr_merge_audio_video), Toast.LENGTH_LONG).show();
            Mp4Muxer muxer = new Mp4Muxer(new File(_videoFile), _ambietAudio, _dvrFile);
            muxer.start();
        } else {
            new File(_videoFile).renameTo( new File(_dvrFile)); // No Ambient recording just dvr
        }

        this.recording = false;
    }
}