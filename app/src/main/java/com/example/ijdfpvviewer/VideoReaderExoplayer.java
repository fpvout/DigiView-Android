package com.example.ijdfpvviewer;

import android.content.Context;
import android.net.Uri;

import android.view.SurfaceView;

import com.google.android.exoplayer2.DefaultLoadControl;
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

        VideoReaderExoplayer(InputStream input, SurfaceView videoSurface, Context c){
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(32*1024, 64*1024, 0, 0).createDefaultLoadControl();
            mPlayer = new SimpleExoPlayer.Builder(c).setLoadControl(loadControl).build();

            mPlayer.setVideoSurfaceView(videoSurface);
            mPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

            DataSpec dataSpec = new DataSpec(Uri.parse("test"));

            DataSource.Factory  dataSourceFactory = () -> (DataSource) new InputStreamDataSource(c, dataSpec, input);

            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse("test")));

            mPlayer.setMediaSource(mediaSource);
        }

        public void start() {
            mPlayer.prepare();
            mPlayer.play();
        }

}
