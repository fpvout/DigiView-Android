package com.fpvout.digiview.vr.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.fpvout.digiview.R;
import com.fpvout.digiview.VideoReaderExoplayer;
import com.fpvout.digiview.vr.gles.EglCore;
import com.fpvout.digiview.vr.gles.FullFrameRect;
import com.fpvout.digiview.vr.gles.Texture2dProgram;
import com.fpvout.digiview.vr.gles.WindowSurface;
import com.google.android.exoplayer2.SimpleExoPlayer;

public class VrView extends FrameLayout implements SurfaceTexture.OnFrameAvailableListener {
    private SurfaceView surfaceViewLeft;
    private SurfaceView surfaceViewRight;
    private SimpleExoPlayer mPlayer;
    private EglCore eglCore;
    private FullFrameRect fullFrameBlit;
    private int textureId = 0;
    private SurfaceTexture videoSurfaceTexture;
    private final float[] transformMatrix = new float[16];
    private WindowSurface mainDisplaySurface;
    private WindowSurface secondaryDisplaySurface;
    private Surface surface;

    public VrView(Context context) {
        super(context);
        init(context);
    }

    public VrView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VrView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.vr_view, this);
        surfaceViewLeft = findViewById(R.id.surfaceViewLeft);
        surfaceViewRight = findViewById(R.id.surfaceViewRight);
    }

    public void setAspect(int dividend, int divisor) {
        ConstraintLayout.LayoutParams leftParams = (ConstraintLayout.LayoutParams) surfaceViewLeft.getLayoutParams();
        ConstraintLayout.LayoutParams rightParams = (ConstraintLayout.LayoutParams) surfaceViewRight.getLayoutParams();
        String dimensionString = "H," + dividend + ":" + divisor;
        leftParams.dimensionRatio = dimensionString;
        rightParams.dimensionRatio = dimensionString;
    }

    public void start(SimpleExoPlayer mPlayer) {
        this.mPlayer = mPlayer;
        surfaceViewLeft.getHolder().addCallback(videoSurfaceCallbackLeft);
        surfaceViewRight.getHolder().addCallback(videoSurfaceCallbackRight);
        surfaceViewLeft.setVisibility(View.GONE);
        surfaceViewRight.setVisibility(View.GONE);
        surfaceViewLeft.post(new Runnable() {
            @Override
            public void run() {
                surfaceViewLeft.setVisibility(View.VISIBLE);
                surfaceViewRight.setVisibility(View.VISIBLE);
            }
        });
    }

    public void stop() {
        surfaceViewLeft.getHolder().removeCallback(videoSurfaceCallbackLeft);
        surfaceViewRight.getHolder().removeCallback(videoSurfaceCallbackRight);
        if (surface != null) {
            surface.release();
            surface = null;
        }

        if (videoSurfaceTexture != null) {
            videoSurfaceTexture.release();
            videoSurfaceTexture = null;
        }

        if (mainDisplaySurface != null) {
            mainDisplaySurface.release();
            mainDisplaySurface = null;
        }

        if (secondaryDisplaySurface != null) {
            secondaryDisplaySurface.release();
            secondaryDisplaySurface = null;
        }

        if (fullFrameBlit != null) {
            fullFrameBlit.release(false);
            fullFrameBlit = null;
        }

        if (eglCore != null) {
            eglCore.release();
            eglCore = null;
        }
    }

    private SurfaceHolder.Callback videoSurfaceCallbackLeft = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            eglCore = new EglCore();

            mainDisplaySurface = new WindowSurface(eglCore, holder.getSurface(), false);
            mainDisplaySurface.makeCurrent();

            fullFrameBlit = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            textureId = fullFrameBlit.createTextureObject();
            videoSurfaceTexture = new SurfaceTexture(textureId);
            videoSurfaceTexture.setOnFrameAvailableListener(VrView.this);
            surface = new Surface(videoSurfaceTexture);
            mPlayer.setVideoSurface(surface);
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

        }
    };

    private SurfaceHolder.Callback videoSurfaceCallbackRight = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            secondaryDisplaySurface = new WindowSurface(eglCore, holder.getSurface(), false);
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

        }
    };

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (eglCore == null) {
            return;
        }

        if (mainDisplaySurface != null) {
            drawFrame(mainDisplaySurface, surfaceViewLeft.getWidth(), surfaceViewLeft.getHeight());
        }

        if (secondaryDisplaySurface != null) {
            drawFrame(secondaryDisplaySurface, secondaryDisplaySurface.getWidth(), secondaryDisplaySurface.getHeight());
        }
    }

    private void drawFrame(WindowSurface windowSurface, int viewWidth, int viewHeight) {
        windowSurface.makeCurrent();
        videoSurfaceTexture.updateTexImage();
        videoSurfaceTexture.getTransformMatrix(transformMatrix);
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        fullFrameBlit.drawFrame(textureId, transformMatrix);
        windowSurface.swapBuffers();
    }
}