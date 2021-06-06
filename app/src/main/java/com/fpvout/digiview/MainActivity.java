package com.fpvout.digiview;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;

import static com.fpvout.digiview.UsbMaskConnection.ACTION_USB_PERMISSION;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final String TAG = "DIGIVIEW";
    private static final String ShowWatermark = "ShowWatermark";
    UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    UsbMaskConnection mUsbMaskConnection;
    VideoReaderExoplayer mVideoReader;
    boolean usbConnected = false;
    SurfaceView fpvView;
    private int shortAnimationDuration;
    private FloatingActionButton settingsButton;
    private View watermarkView;
    private OverlayView overlayView;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private SharedPreferences sharedPreferences;
    private final Runnable hideButtonsRunnable = new Runnable() {
        @Override
        public void run() {
            toggleView(settingsButton, false);
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
        setFullscreen();

        // Prevent screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // Register app for auto launch
        usbDeviceBroadcastReceiver = new UsbDeviceBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbDeviceBroadcastReceiver, filter);
        IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbDeviceBroadcastReceiver, filterDetached);

        watermarkView = findViewById(R.id.watermarkView);
        overlayView = findViewById(R.id.overlayView);
        fpvView = findViewById(R.id.fpvView);
        settingsButton = findViewById(R.id.settingsButton);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        // Enable resizing animations
        ((ViewGroup) findViewById(R.id.mainLayout)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        setupGestureDetectors();

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SettingsActivity.class);
            v.getContext().startActivity(intent);
        });

        mVideoReader = new VideoReaderExoplayer(fpvView, this);
        mVideoReader.setVideoPlayingEventListener(this::hideFullOverlay);
        mVideoReader.setVideoWaitingEventListener(() -> showOverlay(R.string.waiting_for_video, OverlayStatus.Connected));

        mUsbMaskConnection = new UsbMaskConnection();

        if (!usbConnected) {
            usbDevice = UsbMaskConnection.searchDevice(usbManager, getApplicationContext());
            if (usbDevice != null) {
                Log.i(TAG, "USB - usbDevice attached");
                showOverlay(R.string.usb_device_found, OverlayStatus.Connected);
                connect();
            } else {
                showOverlay(R.string.waiting_for_usb_device, OverlayStatus.Disconnected);
            }
        }
    }

    private void setFullscreen() {
        WindowInsetsControllerCompat insetsControllerCompat = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsControllerCompat.hide(WindowInsetsCompat.Type.statusBars()
                | WindowInsetsCompat.Type.navigationBars()
                | WindowInsetsCompat.Type.captionBar()
                | WindowInsetsCompat.Type.ime()
        );
        insetsControllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
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

    private void toggleFullOverlay() {
        if (overlayView.getAlpha() > 0.0f) return;

        toggleView(settingsButton, hideButtonsRunnable);
    }

    private void hideFullOverlay() {
        toggleView(watermarkView, sharedPreferences.getBoolean(ShowWatermark, true), 0.3f);

        toggleView(settingsButton, false);
        toggleView(overlayView, false);
    }

    private void showFullOverlay() {
        toggleView(watermarkView, false);

        toggleView(settingsButton, true);
    }

    private void showOverlay(int textId, OverlayStatus connected) {
        toggleView(overlayView, true);
        showFullOverlay();
        overlayView.show(textId, connected);
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

    private void connect() {
        usbConnected = true;
        mUsbMaskConnection.setUsbDevice(usbManager, usbDevice);
        mVideoReader.setUsbMaskConnection(mUsbMaskConnection);
        mVideoReader.start();
        showOverlay(R.string.waiting_for_video, OverlayStatus.Connected);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "APP - On Resume");

        setFullscreen();

        if (!usbConnected) {
            usbDevice = UsbMaskConnection.searchDevice(usbManager, getApplicationContext());
            if (usbDevice != null) {
                Log.d(TAG, "APP - On Resume usbDevice device found");
                showOverlay(R.string.usb_device_found, OverlayStatus.Connected);
                connect();
            } else {
                showOverlay(R.string.waiting_for_usb_device, OverlayStatus.Disconnected);
            }
        }
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

    private void checkDataCollectionAgreement() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean dataCollectionAccepted = preferences.getBoolean("dataCollectionAccepted", false);
        boolean dataCollectionReplied = preferences.getBoolean("dataCollectionReplied", false);
        if (!dataCollectionReplied) {
            Intent intent = new Intent(this, DataCollectionAgreementPopupActivity.class);
            ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (ActivityResultCallback<ActivityResult>) result -> {
                if (result.getResultCode() == RESULT_OK) {
                    SentryAndroid.init(getApplicationContext(), options -> options.setBeforeSend((event, hint) -> {
                        if (SentryLevel.DEBUG.equals(event.getLevel()))
                            return null;
                        else
                            return event;
                    }));
                }
                setFullscreen();
            });
            activityResultLauncher.launch(intent);


        } else if (dataCollectionAccepted) {
            SentryAndroid.init(this, options -> options.setBeforeSend((event, hint) -> {
                if (SentryLevel.DEBUG.equals(event.getLevel()))
                    return null;
                else
                    return event;
            }));
        }

    }

}