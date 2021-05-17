package com.fpvout.digiview;

import android.content.Context;
import android.net.Uri;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.C;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.video.VideoListener;

import usb.AndroidUSBInputStream;

public class VideoReaderExoplayer {
        private static final String TAG = "DIGIVIEW";
        private SimpleExoPlayer mPlayer;
        private SurfaceView surfaceView;
        private Context context;
        private AndroidUSBInputStream inputStream;
        private UsbMaskConnection mUsbMaskConnection;
        private boolean zoomedIn = true;

        VideoReaderExoplayer(SurfaceView videoSurface, Context c) {
            surfaceView = videoSurface;
            context = c;
        }

        public void setUsbMaskConnection(UsbMaskConnection connection) {
            mUsbMaskConnection = connection;
            inputStream = mUsbMaskConnection.mInputStream;
        }

        public void start() {
            inputStream.startReadThread();
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(32 * 1024, 64 * 1024, 0, 0).build();
            mPlayer = new SimpleExoPlayer.Builder(context).setLoadControl(loadControl).build();
            mPlayer.setVideoSurfaceView(surfaceView);
            mPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mPlayer.setWakeMode(C.WAKE_MODE_LOCAL);

            DataSpec dataSpec = new DataSpec(Uri.EMPTY, 0, C.LENGTH_UNSET);

            DataSource.Factory dataSourceFactory = () -> (DataSource) new InputStreamDataSource(context, dataSpec, inputStream);

            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, H264Extractor.FACTORY).createMediaSource(MediaItem.fromUri(Uri.EMPTY));
            mPlayer.setMediaSource(mediaSource);

            mPlayer.prepare();
            mPlayer.play();
            mPlayer.addListener(new ExoPlayer.EventListener() {
                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    switch (error.type) {
                        case ExoPlaybackException.TYPE_SOURCE:
                            Log.e(TAG, "PLAYER_SOURCE - TYPE_SOURCE: " + error.getSourceException().getMessage());
                            Toast.makeText(context, "Waiting for video...", Toast.LENGTH_SHORT).show();
                            (new Handler(Looper.getMainLooper())).postDelayed(() -> {
                                restart();
                            }, 1000);
                            break;
                    }
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        Log.d(TAG, "PLAYER_STATE - ENDED");
                        Toast.makeText(context, "Waiting for video...", Toast.LENGTH_SHORT).show();
                        (new Handler(Looper.getMainLooper())).postDelayed(() -> {
                            restart();
                        }, 1000);
                    }
                }
            });

            mPlayer.addVideoListener(new VideoListener() {
                @Override
                public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                    if (!zoomedIn) {
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) surfaceView.getLayoutParams();
                        params.dimensionRatio = width + ":" + height;
                        surfaceView.setLayoutParams(params);
                    }
                }
            });
        }

        public void toggleZoom() {
            zoomedIn = !zoomedIn;

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
}
