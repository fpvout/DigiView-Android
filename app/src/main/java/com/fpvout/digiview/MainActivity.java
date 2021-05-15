package com.fpvout.digiview;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final String ACTION_USB_PERMISSION = "com.fpvout.digiview.USB_PERMISSION";
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    PendingIntent permissionIntent;
    UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    UsbMaskConnection mUsbMaskConnection;
    VideoReaderExoplayer mVideoReader;
    boolean usbConnected = false;
    SurfaceView fpvView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        actionBar.hide();

        // Prevent screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbDeviceBroadcastReceiver = new UsbDeviceBroadcastReceiver(this);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbDeviceBroadcastReceiver, filter);
        IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbDeviceBroadcastReceiver, filterDetached);

        fpvView = findViewById(R.id.fpvView);

        mUsbMaskConnection = new UsbMaskConnection();
        mVideoReader = new VideoReaderExoplayer(fpvView, this);

        Toast.makeText(getApplicationContext(), "waiting for usb connection...", Toast.LENGTH_SHORT).show();

        if (searchDevice() && !usbConnected) {
            connect();
        }
    }

    @Override
    public void usbDeviceApproved(UsbDevice device) {
        Log.d("USB", "usbDevice approved");
        usbDevice = device;
        Toast.makeText(getApplicationContext(), "usb attached", Toast.LENGTH_SHORT).show();
        connect();
    }

    @Override
    public void usbDeviceDetached() {
        Log.d("USB", "usbDevice detached");
        Toast.makeText(getApplicationContext(), "usb detached", Toast.LENGTH_SHORT).show();
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
        mVideoReader.start();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (searchDevice() && !usbConnected) {
            Log.d("RESUME_USB_CONNECTED", "not connected");
            connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }
}