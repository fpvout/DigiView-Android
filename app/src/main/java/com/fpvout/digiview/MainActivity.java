package com.fpvout.digiview;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static AppCompatActivity instance;
    private static final String ACTION_USB_PERMISSION = "com.fpvout.digiview.USB_PERMISSION";
    private static final String TAG = "DIGIVIEW";
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    private int shortAnimationDuration;
    private boolean watermarkAnimationInProgress = false;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Log.d(TAG, "APP - On Create");
        setContentView(R.layout.activity_main);

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

        // Enable resizing animations
        ((ViewGroup) findViewById(R.id.mainLayout)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (watermarkView.getVisibility() == View.VISIBLE) {
                    toggleWatermark();
                }
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

        watermarkView.setVisibility(View.GONE);

        mUsbMaskConnection = new UsbMaskConnection();
        mVideoReader = new VideoReaderExoplayer(fpvView, overlayView, this);

        if(checkStoragePermission()){
            // permission already granted
            finishStartup();
        }
    }

    private void finishStartup() {

        if (!usbConnected) {
            if (searchDevice()) {
                connect();
            } else {
                overlayView.showOpaque(R.string.waiting_for_usb_device, OverlayStatus.Disconnected);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                finishStartup();
            }
            else {
                overlayView.showOpaque("Storage access is required.", OverlayStatus.Error);
            }
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;

            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    private void toggleWatermark() {
        if (watermarkAnimationInProgress) {
            return;
        }
        watermarkAnimationInProgress = true;

        float targetAlpha = 0;
        if (watermarkView.getAlpha() == 0) {
            targetAlpha = 0.3F;
        }

        watermarkView.animate()
                .alpha(targetAlpha)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        watermarkAnimationInProgress = false;
                    }
                });
    }

    @Override
    public void usbDeviceApproved(UsbDevice device) {
        Log.i(TAG, "USB - usbDevice approved");
        usbDevice = device;
        overlayView.showOpaque(R.string.usb_device_approved, OverlayStatus.Connected);
        connect();
    }

    @Override
    public void usbDeviceDetached() {
        Log.i(TAG, "USB - usbDevice detached");
        overlayView.showOpaque(R.string.usb_device_detached_waiting, OverlayStatus.Disconnected);
        this.onStop();
    }

    private boolean searchDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.size() <= 0) {
            usbDevice = null;
            return false;
        }

        for(UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID) {
                if (usbManager.hasPermission(device)) {
                    Log.i(TAG, "USB - usbDevice attached");
                    overlayView.showOpaque(R.string.usb_device_found, OverlayStatus.Connected);
                    usbDevice = device;
                    return true;
                }

                usbManager.requestPermission(device, permissionIntent);
            }
        }

        return false;
    }

    private void connect(){
        usbConnected = true;
        mUsbMaskConnection.setUsbDevice(usbManager.openDevice(usbDevice), usbDevice);
        mVideoReader.setUsbMaskConnection(mUsbMaskConnection);
        watermarkView.setVisibility(View.VISIBLE);
        overlayView.hide();
        mVideoReader.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "APP - On Resume");

        if (!usbConnected) {
            if (searchDevice()) {
                Log.d(TAG, "APP - On Resume usbDevice device found");
                connect();
            } else {
                overlayView.showOpaque(R.string.waiting_for_usb_device, OverlayStatus.Disconnected);
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

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}