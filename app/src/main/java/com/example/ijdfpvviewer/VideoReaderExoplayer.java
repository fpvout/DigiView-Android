package com.example.ijdfpvviewer;

import android.content.Context;
import android.net.Uri;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.C;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.InputStream;

public class VideoReaderExoplayer {

        private SimpleExoPlayer mPlayer;
        private Context appContext;

        VideoReaderExoplayer(InputStream input, SurfaceView videoSurface, Context c){
            appContext = c;
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(32*1024, 64*1024, 0, 0).build();
            mPlayer = new SimpleExoPlayer.Builder(c).setLoadControl(loadControl).build();

            mPlayer.setVideoSurfaceView(videoSurface);
            mPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mPlayer.setWakeMode(C.WAKE_MODE_LOCAL);

            DataSpec dataSpec = new DataSpec(Uri.parse(""),0,C.LENGTH_UNSET);

            DataSource.Factory  dataSourceFactory = () -> (DataSource) new InputStreamDataSource(appContext, dataSpec, input);

            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory,H264Extractor.FACTORY).createMediaSource(MediaItem.fromUri(Uri.parse("")));
            mPlayer.setMediaSource(mediaSource);
        }

        public void start() {
            mPlayer.prepare();
            mPlayer.play();
            mPlayer.addListener(new ExoPlayer.EventListener() {
                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    switch (error.type) {
                        case ExoPlaybackException.TYPE_SOURCE:
                            Log.e("PLAYER_SOURCE", "TYPE_SOURCE: " + error.getSourceException().getMessage());
                            Toast.makeText(appContext, "Video not ready", Toast.LENGTH_SHORT).show();
                            (new Handler(Looper.getMainLooper())).postDelayed(() -> {
                                start(); //retry in 10 sec
                            }, 10000);
                            break;
                    }
                }
            });
        }

}
