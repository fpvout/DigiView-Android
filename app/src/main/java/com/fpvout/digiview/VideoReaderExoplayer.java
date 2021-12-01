package com.fpvout.digiview;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.video.VideoSize;

import usb.AndroidUSBInputStream;

public class VideoReaderExoplayer {
    private static final String TAG = "DIGIVIEW";
    private Handler videoReaderEventListener;
    private ExoPlayer mPlayer;
    static final String VideoPreset = "VideoPreset";
    private final SurfaceView surfaceView;
    private AndroidUSBInputStream inputStream;
    private UsbMaskConnection mUsbMaskConnection;
    private boolean zoomedIn;
    private final Context context;
    private PerformancePreset performancePreset = PerformancePreset.getPreset(PerformancePreset.PresetType.DEFAULT);
    static final String VideoZoomedIn = "VideoZoomedIn";
    private final SharedPreferences sharedPreferences;
    private boolean streaming = false;

    VideoReaderExoplayer(SurfaceView videoSurface, Context c) {
        surfaceView = videoSurface;
        context = c;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    }

    VideoReaderExoplayer(SurfaceView videoSurface, Context c, Handler v) {
        this(videoSurface, c);
        videoReaderEventListener = v;
    }

    public void setUsbMaskConnection(UsbMaskConnection connection) {
        mUsbMaskConnection = connection;
        inputStream = mUsbMaskConnection.mInputStream;
    }

    public void start() {
        zoomedIn = sharedPreferences.getBoolean(VideoZoomedIn, true);
        performancePreset = PerformancePreset.getPreset(sharedPreferences.getString(VideoPreset, "default"));

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(performancePreset.exoPlayerMinBufferMs, performancePreset.exoPlayerMaxBufferMs, performancePreset.exoPlayerBufferForPlaybackMs, performancePreset.exoPlayerBufferForPlaybackAfterRebufferMs).build();
        mPlayer = new ExoPlayer.Builder(context).setLoadControl(loadControl).build();
        mPlayer.setVideoSurfaceView(surfaceView);
        mPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        mPlayer.setWakeMode(C.WAKE_MODE_LOCAL);

        DataSpec dataSpec = new DataSpec(Uri.EMPTY, 0, C.LENGTH_UNSET);

        Log.d(TAG, "preset: " + performancePreset);

        DataSource.Factory dataSourceFactory = () -> {
            switch (performancePreset.dataSourceType) {
                case INPUT_STREAM:
                        return (DataSource) new InputStreamDataSource(context, dataSpec, inputStream);
                    case BUFFERED_INPUT_STREAM:
                    default:
                        return (DataSource) new InputStreamBufferedDataSource(context, dataSpec, inputStream);
                }
            };

            ExtractorsFactory extractorsFactory = () -> new Extractor[]{new H264Extractor(performancePreset.h264ReaderMaxSyncFrameSize, performancePreset.h264ReaderSampleTime)};
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(MediaItem.fromUri(Uri.EMPTY));
            mPlayer.setMediaSource(mediaSource);

            mPlayer.prepare();
            mPlayer.play();
            mPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayerErrorChanged(@Nullable PlaybackException error) {
                    if (error == null) {
                        Log.e(TAG, "PLAYER_SOURCE - TYPE_UNEXPECTED: no message");
                        return;
                    }
                    ExoPlaybackException e = (ExoPlaybackException) error;
                    Log.e(TAG, "onPlayerErrorChanged: " + e.type);
                    switch (e.type) {
                        case ExoPlaybackException.TYPE_SOURCE:
                            streaming = false;
                            Log.e(TAG, "PLAYER_SOURCE - TYPE_SOURCE: " + error.getMessage());
                            (new Handler(Looper.getMainLooper())).postDelayed(() -> restart(), 1000);
                            break;
                        case ExoPlaybackException.TYPE_REMOTE:
                            Log.e(TAG, "PLAYER_SOURCE - TYPE_REMOTE: " + error.getMessage());
                            break;
                        case ExoPlaybackException.TYPE_RENDERER:
                            Log.e(TAG, "PLAYER_SOURCE - TYPE_RENDERER: " + error.getMessage());
                            break;
                        case ExoPlaybackException.TYPE_UNEXPECTED:
                            Log.e(TAG, "PLAYER_SOURCE - TYPE_UNEXPECTED: " + error.getMessage());
                            break;
                    }
                }

                @Override
                public void onPlaybackStateChanged(@NonNull int state) {
                    switch (state) {
                        case Player.STATE_IDLE:
                        case Player.STATE_READY:
                        case Player.STATE_BUFFERING:
                            break;
                        case Player.STATE_ENDED:
                            Log.d(TAG, "PLAYER_STATE - ENDED");
                            sendEvent(VideoReaderEventMessageCode.WAITING_FOR_VIDEO); // let MainActivity know so it can hide watermark/show settings button
                            (new Handler(Looper.getMainLooper())).postDelayed(() -> restart(), 1000);
                            break;
                    }
                }
            });

            mPlayer.addListener(new Player.Listener() {
                @Override
                public void onRenderedFirstFrame() {
                    Log.d(TAG, "PLAYER_RENDER - FIRST FRAME");
                    sendEvent(VideoReaderEventMessageCode.VIDEO_PLAYING); // let MainActivity know so it can hide watermark/show settings button
                }

                @Override
                public void onVideoSizeChanged(@NonNull VideoSize videosize) {
                    if (!zoomedIn) {
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) surfaceView.getLayoutParams();
                        params.dimensionRatio = videosize.width + ":" + videosize.height;
                        surfaceView.setLayoutParams(params);
                    }
                }
            });
    }

    private void sendEvent(VideoReaderEventMessageCode eventCode) {
        if (videoReaderEventListener != null) { // let MainActivity know so it can hide watermark/show settings button
            Message videoReaderEventMessage = new Message();
            videoReaderEventMessage.obj = eventCode;
            videoReaderEventListener.sendMessage(videoReaderEventMessage);
        }
    }

    public void toggleZoom() {
        zoomedIn = !zoomedIn;

        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putBoolean(VideoZoomedIn, zoomedIn);
        preferencesEditor.apply();

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) surfaceView.getLayoutParams();

            if (zoomedIn) {
                params.dimensionRatio = "";
            } else {
                if (mPlayer == null) return;
                Format videoFormat = mPlayer.getVideoFormat();
                if (videoFormat == null) return;

                params.dimensionRatio = videoFormat.width + ":" + videoFormat.height;
            }

            surfaceView.setLayoutParams(params);
        }

        public void zoomIn() {
            if (!zoomedIn) {
                toggleZoom();
            }
        }

        public void zoomOut() {
            if (zoomedIn) {
                toggleZoom();
            }
        }

        public void restart() {
            mPlayer.release();

            if (mUsbMaskConnection.isReady()) {
                mUsbMaskConnection.start();
                start();
            }
        }

    public void stop() {
        if (mPlayer != null)
            mPlayer.release();
    }

    public enum VideoReaderEventMessageCode {WAITING_FOR_VIDEO, VIDEO_PLAYING}
}
