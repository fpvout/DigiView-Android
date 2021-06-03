package com.fpvout.digiview;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.fpvout.digiview.streaming.StreamingService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.ossrs.rtmp.ConnectCheckerRtmp;

import java.util.HashMap;

import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "DIGIVIEW";
    private static final String ACTION_USB_PERMISSION = "com.fpvout.digiview.USB_PERMISSION";
    private static final int DATA_COLLECTION_AGREEMENT = 1;
    private static final int MEDIA_PROJECTION_PERMISSION = 2;
    private static final int RECORD_AUDIO_PERMISSION = 3;
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    private int shortAnimationDuration;
    private FloatingActionButton settingsButton;
    private FloatingActionButton liveButton;
    private FloatingActionButton muteButton;
    private TextView bitrateTextview;
    private View watermarkView;
    private OverlayView overlayView;
    PendingIntent permissionIntent;
    UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    UsbMaskConnection mUsbMaskConnection;
    VideoReaderExoplayer mVideoReader;
    boolean usbConnected = false;
    SurfaceView fpvView;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private SharedPreferences sharedPreferences;
    private static final String ShowWatermark = "ShowWatermark";
    private final Runnable hideButtonsRunnable = new Runnable() {
        @Override
        public void run() {
            toggleView(settingsButton, false);
            toggleView(liveButton, false);
            toggleView(muteButton, false);
            toggleView(bitrateTextview, false);
        }
    };
    private final ConnectCheckerRtmp connectChecker = new ConnectCheckerRtmp() {
        @Override
        public void onConnectionSuccessRtmp() {
            updateLiveButtonIcon();
        }

        @Override
        public void onConnectionFailedRtmp(String reason) {
            stopService(new Intent(getApplicationContext(), StreamingService.class));
            updateLiveButtonIcon();
            runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), getString(R.string.rtmp_connection_failed), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onNewBitrateRtmp(long bitrate) {
            runOnUiThread(() -> {
                bitrateTextview.setVisibility(View.VISIBLE);
                bitrateTextview.setText(String.format("%s kbps", bitrate / 1024));
            });
        }

        @Override
        public void onDisconnectRtmp() {
            updateLiveButtonIcon();
            runOnUiThread(() -> {
                bitrateTextview.setText("");
                bitrateTextview.setVisibility(View.GONE);
            });
        }

        @Override
        public void onAuthErrorRtmp() {
            updateLiveButtonIcon();
            runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), getString(R.string.rtmp_auth_error), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onAuthSuccessRtmp() {
            updateLiveButtonIcon();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "APP - On Create");
        setContentView(R.layout.activity_main);

        // check Data Collection agreement
        checkDataCollectionAgreement();

        // Hide top bar and status bar
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Prevent screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbDeviceBroadcastReceiver = new UsbDeviceBroadcastReceiver(this);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbDeviceBroadcastReceiver, filter);
        IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbDeviceBroadcastReceiver, filterDetached);

        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        watermarkView = findViewById(R.id.watermarkView);
        overlayView = findViewById(R.id.overlayView);
        fpvView = findViewById(R.id.fpvView);

        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SettingsActivity.class);
            v.getContext().startActivity(intent);
        });

        bitrateTextview = findViewById(R.id.bitrateText);
        StreamingService.init(this, connectChecker);

        muteButton = findViewById(R.id.muteButton);
        muteButton.setOnClickListener(v -> {
            StreamingService.toggleMute();
            updateLiveButtonIcon();
        });

        liveButton = findViewById(R.id.liveButton);
        liveButton.setOnClickListener(v -> {
            if (!StreamingService.isStreaming()) {
                if (sharedPreferences.getString("StreamRtmpUrl", "").isEmpty() || sharedPreferences.getString("StreamRtmpKey", "").isEmpty()) {
                    Toast.makeText(this, getString(R.string.rtmp_settings_empty), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(v.getContext(), SettingsActivity.class);
                    v.getContext().startActivity(intent);
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startActivityForResult(StreamingService.sendIntent(), MEDIA_PROJECTION_PERMISSION);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION);
                    }
                }
            } else {
                stopService(new Intent(this, StreamingService.class));
                this.updateLiveButtonIcon();
            }
        });

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Enable resizing animations
        ((ViewGroup) findViewById(R.id.mainLayout)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        setupGestureDetectors();

        mUsbMaskConnection = new UsbMaskConnection();
        Handler videoReaderEventListener = new Handler(this.getMainLooper(), msg -> onVideoReaderEvent((VideoReaderExoplayer.VideoReaderEventMessageCode) msg.obj));

        mVideoReader = new VideoReaderExoplayer(fpvView, this, videoReaderEventListener);

        if (!usbConnected) {
            if (searchDevice()) {
                connect();
            } else {
                showOverlay(R.string.waiting_for_usb_device, OverlayStatus.Disconnected);
            }
        }
    }

    private void toggleFullOverlay() {
        if (overlayView.getAlpha() > 0.0f) return;

        toggleView(settingsButton, hideButtonsRunnable);
        toggleView(liveButton, hideButtonsRunnable);
        if (StreamingService.isStreaming()) {
            toggleView(bitrateTextview, hideButtonsRunnable);
            toggleView(muteButton, hideButtonsRunnable);
        }
    }

    private void hideFullOverlay() {
        toggleView(watermarkView, sharedPreferences.getBoolean(ShowWatermark, true), 0.3f);

        toggleView(settingsButton, false);
        toggleView(liveButton, false);
        toggleView(muteButton, false);
        toggleView(bitrateTextview, false);
        toggleView(overlayView, false);
    }

    private void showFullOverlay() {
        toggleView(watermarkView, false);

        toggleView(settingsButton, true);
        toggleView(liveButton, true);
        if (StreamingService.isStreaming()) {
            toggleView(muteButton, true);
            toggleView(bitrateTextview, true);
        }
    }

    private void toggleView(View view, @Nullable Runnable runnable) {
        toggleView(view, view.getAlpha() == 0.0f, 1.0f, runnable);
    }

    private void toggleView(FloatingActionButton view, @Nullable Runnable runnable) {
        toggleView(view, view.getVisibility() != View.VISIBLE, runnable);
    }

    private void toggleView(View view, boolean visible) {
        toggleView(view, visible, 1.0f, null);
    }

    private void toggleView(FloatingActionButton view, boolean visible) {
        toggleView(view, visible, null);
    }

    private void toggleView(View view, boolean visible, float visibleAlpha) {
        toggleView(view, visible, visibleAlpha, null);
    }

    private void toggleView(View view, boolean visible, float visibleAlpha, @Nullable Runnable runnable) {
        if (!visible) {
            view.removeCallbacks(runnable);
            view.animate().cancel();
            view.animate()
                    .alpha(0)
                    .setDuration(shortAnimationDuration)
                    .setListener(null);
        } else {
            view.removeCallbacks(runnable);
            view.animate().cancel();
            view.animate()
                    .alpha(visibleAlpha)
                    .setDuration(shortAnimationDuration);
            view.postDelayed(runnable, 3000);
        }
    }

    private void toggleView(FloatingActionButton view, boolean visible, @Nullable Runnable runnable) {
        if (!visible) {
            view.hide();
        } else {
            view.removeCallbacks(runnable);
            view.show();
            view.postDelayed(runnable, 3000);
        }
    }

    private void setupGestureDetectors() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleFullOverlay();
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mVideoReader.toggleZoom();
                return super.onDoubleTap(e);
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                if (detector.getScaleFactor() < 1) {
                    mVideoReader.zoomOut();
                } else {
                    mVideoReader.zoomIn();
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public void usbDeviceApproved(UsbDevice device) {
        Log.i(TAG, "USB - usbDevice approved");
        usbDevice = device;
        showOverlay(R.string.usb_device_approved, OverlayStatus.Connected);
        connect();
    }

    @Override
    public void usbDeviceDetached() {
        Log.i(TAG, "USB - usbDevice detached");
        showOverlay(R.string.usb_device_detached_waiting, OverlayStatus.Disconnected);
        this.onStop();
    }

    private boolean searchDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.size() <= 0) {
            usbDevice = null;
            return false;
        }

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID) {
                if (usbManager.hasPermission(device)) {
                    Log.i(TAG, "USB - usbDevice attached");
                    showOverlay(R.string.usb_device_found, OverlayStatus.Connected);
                    usbDevice = device;
                    return true;
                }

                usbManager.requestPermission(device, permissionIntent);
            }
        }

        return false;
    }

    private void connect() {
        usbConnected = true;
        mUsbMaskConnection.setUsbDevice(usbManager.openDevice(usbDevice), usbDevice);
        mVideoReader.setUsbMaskConnection(mUsbMaskConnection);
        mVideoReader.start();
        showOverlay(R.string.waiting_for_video, OverlayStatus.Connected);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "APP - On Resume");

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        if (!usbConnected) {
            if (searchDevice()) {
                Log.d(TAG, "APP - On Resume usbDevice device found");
                connect();
            } else {
                showOverlay(R.string.waiting_for_usb_device, OverlayStatus.Connected);
            }
        }
    }

    private boolean onVideoReaderEvent(VideoReaderExoplayer.VideoReaderEventMessageCode m) {
        if (VideoReaderExoplayer.VideoReaderEventMessageCode.WAITING_FOR_VIDEO.equals(m)) {
            Log.d(TAG, "event: WAITING_FOR_VIDEO");
            showOverlay(R.string.waiting_for_video, OverlayStatus.Connected);
        } else if (VideoReaderExoplayer.VideoReaderEventMessageCode.VIDEO_PLAYING.equals(m)) {
            Log.d(TAG, "event: VIDEO_PLAYING");
            hideFullOverlay();
        }
        return false; // false to continue listening
    }

    private void showOverlay(int textId, OverlayStatus connected) {
        toggleView(overlayView, true);
        showFullOverlay();
        overlayView.show(textId, connected);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "APP - On Stop");

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "APP - On Pause");

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "APP - On Destroy");

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean dataCollectionAccepted = preferences.getBoolean("dataCollectionAccepted", false);

        if (requestCode == DATA_COLLECTION_AGREEMENT) { // Data Collection agreement Activity
            if (resultCode == RESULT_OK && dataCollectionAccepted) {
                SentryAndroid.init(this, options -> options.setBeforeSend((event, hint) -> {
                    if (SentryLevel.DEBUG.equals(event.getLevel()))
                        return null;
                    else
                        return event;
                }));
            }
        }

        if (data != null && (requestCode == MEDIA_PROJECTION_PERMISSION && resultCode == Activity.RESULT_OK)) {
            StreamingService.setMediaProjectionData(resultCode, data);
            Intent intent = new Intent(this, StreamingService.class);
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(StreamingService.sendIntent(), MEDIA_PROJECTION_PERMISSION);
        }
    }

    private void checkDataCollectionAgreement() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean dataCollectionAccepted = preferences.getBoolean("dataCollectionAccepted", false);
        boolean dataCollectionReplied = preferences.getBoolean("dataCollectionReplied", false);
        if (!dataCollectionReplied) {
            Intent intent = new Intent(this, DataCollectionAgreementPopupActivity.class);
            startActivityForResult(intent, DATA_COLLECTION_AGREEMENT);
        } else if (dataCollectionAccepted) {
            SentryAndroid.init(this, options -> options.setBeforeSend((event, hint) -> {
                if (SentryLevel.DEBUG.equals(event.getLevel()))
                    return null;
                else
                    return event;
            }));
        }
    }

    private void updateLiveButtonIcon() {
        runOnUiThread(() -> {
            if (StreamingService.isStreaming()) {
                if (StreamingService.isMuted()) {
                    muteButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_microphone_slash_solid, this.getTheme()));
                } else {
                    muteButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_microphone_solid, this.getTheme()));
                }

                toggleView(muteButton, true, overlayView.getAlpha() > 0.0f ? null : hideButtonsRunnable);
                liveButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.exo_icon_stop, this.getTheme()));
            } else {
                toggleView(muteButton, false);
                liveButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_live_icon, this.getTheme()));
            }
        });
    }
}