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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fpvout.digiview.dvr.DVR;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;

import static com.fpvout.digiview.VideoReaderExoplayer.VideoZoomedIn;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final String ACTION_USB_PERMISSION = "com.fpvout.digiview.USB_PERMISSION";
    private static final String TAG = "DIGIVIEW";
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    private int shortAnimationDuration;
    private float buttonAlpha = 1;
    private View settingsButton;
    private View recordButton;
    private ImageButton thumbnail;
    private RelativeLayout toolbar;
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
    DVR dvr;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private SharedPreferences sharedPreferences;
    private static final String ShowWatermark = "ShowWatermark";
    private boolean overlayIsShown = false;

    ActivityResultLauncher<Intent> launchDataCollectionActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean dataCollectionAccepted = preferences.getBoolean("dataCollectionAccepted", false);

                if (result.getResultCode() == Activity.RESULT_OK && dataCollectionAccepted) {
                    SentryAndroid.init(getApplicationContext(), options -> options.setBeforeSend((event, hint) -> {
                        if (SentryLevel.DEBUG.equals(event.getLevel()))
                            return null;
                        else
                            return event;
                    }));
                }
            });

    private void setupGestureDetectors() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleToolbar();
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

    private void cancelButtonAnimation() {
        Handler handler = toolbar.getHandler();
        if (handler != null) {
            toolbar.getHandler().removeCallbacksAndMessages(null);
        }
    }

    private void showToolbar() {
        cancelButtonAnimation();

        if (overlayView.getVisibility() == View.VISIBLE) {
            buttonAlpha = 1;
            toolbar.setAlpha(1);
        }
    }

    private void toggleToolbar() {
        if (buttonAlpha == 1 && overlayView.getVisibility() == View.VISIBLE) return;

        // cancel any pending delayed animations first
        cancelButtonAnimation();

        int translation = 0;
        if (buttonAlpha == 1) {
            buttonAlpha = 0;
            translation = 60;
        } else {
            buttonAlpha = 1;
        }
        updateDVRThumb();
        toolbar.animate()
                .alpha(buttonAlpha)
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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Enable resizing animations
        ((ViewGroup) findViewById(R.id.mainLayout)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        setupGestureDetectors();

        mUsbMaskConnection = new UsbMaskConnection();
        Handler videoReaderEventListener = new Handler(this.getMainLooper(), msg -> onVideoReaderEvent((VideoReaderExoplayer.VideoReaderEventMessageCode) msg.obj));

        mVideoReader = new VideoReaderExoplayer(fpvView, this, videoReaderEventListener);

        dvr = DVR.getInstance(this, true, new Handler(message -> {
            updateDVRThumb();
            return true;
        }), mUsbMaskConnection);
        updateDVRThumb();

        if (!usbConnected) {
            if (searchDevice()) {
                connect();
            } else {
                showOverlay(R.string.waiting_for_usb_device, OverlayStatus.Disconnected);
            }
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
        disconnect();
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


        toolbar.setAlpha(1);
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

    private boolean onVideoReaderEvent(VideoReaderExoplayer.VideoReaderEventMessageCode m) {
        if (VideoReaderExoplayer.VideoReaderEventMessageCode.WAITING_FOR_VIDEO.equals(m)) {
            Log.d(TAG, "event: WAITING_FOR_VIDEO");
            showOverlay(R.string.waiting_for_video, OverlayStatus.Connected);
        } else if (VideoReaderExoplayer.VideoReaderEventMessageCode.VIDEO_PLAYING.equals(m)) {
            Log.d(TAG, "event: VIDEO_PLAYING");
            hideOverlay();
        }
        return false; // false to continue listening
    }

    private void showOverlay(int textId, OverlayStatus connected) {
        overlayView.show(textId, connected);
        overlayIsShown = true;
        toolbar.setTranslationX(0);
        toolbar.setAlpha(1);
        updateWatermark();
        showToolbar();
    }

    private void hideOverlay() {
        overlayView.hide();
        overlayIsShown = false;
        updateWatermark();
        showToolbar();
        autoHideToolbar();
    }

    private void disconnect() {
        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "APP - On Stop");
        disconnect();
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
        if (buttonAlpha == 0) return;

        toolbar.postDelayed(() -> {
            buttonAlpha = 0;
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
            launchDataCollectionActivity.launch(intent);
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