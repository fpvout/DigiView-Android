package com.fpvout.digiview;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.app.ActivityCompat;
import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;

import static com.fpvout.digiview.UsbMaskConnection.ACTION_USB_PERMISSION;
import com.fpvout.digiview.dvr.DVR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;


import static com.fpvout.digiview.VideoReaderExoplayer.VideoZoomedIn;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final String TAG = "DIGIVIEW";
    private int shortAnimationDuration;
    private float toolbarAlpha = 0.7f;
    private View settingsButton;
    private View recordButton;
    private ImageButton thumbnail;
    private RelativeLayout toolbar;
    private View watermarkView;
    private OverlayView overlayView;
    UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    UsbMaskConnection mUsbMaskConnection;
    VideoReaderExoplayer mVideoReader;
    boolean usbConnected = false;
    SurfaceView fpvView;
    DVR dvr;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private SharedPreferences sharedPreferences;
    private static final String ShowWatermark = "ShowWatermark";
    private boolean overlayIsShown = false;

    private void setupGestureDetectors() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleButton();
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

    private void updateWatermark() {
        if (overlayView.getVisibility() == View.VISIBLE) {
            watermarkView.setAlpha(0);
            return;
        }

        if (sharedPreferences.getBoolean(ShowWatermark, true)) {
            watermarkView.setAlpha(0.3F);
        } else {
            watermarkView.setAlpha(0F);
        }
    }

    private void updateVideoZoom() {
        if (sharedPreferences.getBoolean(VideoZoomedIn, true)) {
            mVideoReader.zoomIn();
        } else {
            mVideoReader.zoomOut();
        }
    }

    private void cancelToolbarAnimation() {
        Handler handler = toolbar.getHandler();
        if (handler != null) {
            toolbar.getHandler().removeCallbacksAndMessages(null);
        }
    }

    private void toggleButton() {
        if (overlayView.getVisibility() == View.VISIBLE) return;
        // cancel any pending delayed animations first
         cancelToolbarAnimation();

        int translation = 0;
        if (toolbarAlpha == 0.7f) {
            toolbarAlpha = 0;
            translation = 60;
        } else {
            toolbarAlpha = 0.7f;
        }
        updateDVRThumb();
        toolbar.animate()
                .alpha(toolbarAlpha)
                .translationX(translation)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        autoHideToolbar();
                        updateDVRThumb();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "APP - On Create");
        setContentView(R.layout.activity_main);

        // check Data Collection agreement
        checkDataCollectionAgreement();

        // Hide top bar and status bar
        setFullscreen();

        thumbnail = findViewById(R.id.thumbnail);
        thumbnail.setOnClickListener(view    -> {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            if (dvr != null) {
                intent.setDataAndType(Uri.withAppendedPath(Uri.fromFile(dvr.getDefaultFolder()), ""), "video/*");
            } else {
                intent.setType("image/*");
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        toolbar = findViewById(R.id.toolbar);

        recordButton = findViewById(R.id.recordbt);
        recordButton.setOnClickListener(view -> {
            if (dvr != null) {
                updateDVRThumb();
                if (dvr.isRecording()) {
                    dvr.stop();
                } else {
                    dvr.start();
                }
            } else {
                Toast.makeText(this, this.getText(R.string.no_dvr_video), Toast.LENGTH_LONG).show();
            }
        });

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
        mVideoReader.setVideoPlayingEventListener(this::hideOverlay);
        mVideoReader.setVideoWaitingEventListener(() -> showOverlay(R.string.waiting_for_video, OverlayStatus.Connected));

        mUsbMaskConnection = new UsbMaskConnection();

        dvr = DVR.getInstance(this, true, new Handler(message -> {
            updateDVRThumb();
            return true;
        }), mUsbMaskConnection);
        updateDVRThumb();

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

    private void updateDVRThumb() {
        if (dvr != null) {
            File file = new File(dvr.getLatestThumbFile());
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(dvr.getLatestThumbFile());
                thumbnail.setImageBitmap(bmp);
            } else {
                thumbnail.setImageBitmap(null);
            }
        }
    }


    private void connect() {
        usbConnected = true;
        mUsbMaskConnection.setUsbDevice(usbManager, usbDevice, dvr);
        mVideoReader.setUsbMaskConnection(mUsbMaskConnection);
        overlayView.hide();
        mVideoReader.start();
        updateDVRThumb();
        updateWatermark();
        autoHideToolbar();
        showOverlay(R.string.waiting_for_video, OverlayStatus.Connected);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "APP - On Resume");

        setFullscreen();

        autoHideToolbar();
        updateWatermark();
        updateVideoZoom();

        if(checkStoragePermission()) {
            finishStartup();
        }
    }

    private void finishStartup(){
        // Init DVR recorder
        try {
            dvr.init();
        } catch (IOException e) {
            Log.i(TAG, "DVR - init failed");
        }
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

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                return true;

            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA }, 1);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finishStartup();
            }
            else {
                overlayView.show( R.string.storage_rights_required, OverlayStatus.Error);
            }
        }
    }

    private void showOverlay(int textId, OverlayStatus connected) {
        overlayView.show(textId, connected);
        overlayIsShown = true;
        toolbar.setTranslationX(0);
        toolbar.setAlpha(1);
        updateWatermark();
        autoHideToolbar();
        updateVideoZoom();

    }

    private void hideOverlay() {
        overlayView.hide();
        overlayIsShown = false;
        updateWatermark();
        autoHideToolbar();
        updateVideoZoom();
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

    private void autoHideToolbar() {
        if (overlayView.getVisibility() == View.VISIBLE) return;
        if (overlayIsShown) return;

        toolbar.postDelayed(() -> {
            toolbarAlpha = 0;
            toolbar.animate()
                    .alpha(0)
                    .translationX(60)
                    .setDuration(shortAnimationDuration);
        }, 3000);
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