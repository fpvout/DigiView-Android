package com.example.ijdfpvviewer;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final String ACTION_USB_PERMISSION = "com.example.ijdfpvviewer.USB_PERMISSION";
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    PendingIntent permissionIntent;
    UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    Handler frameHandler;
    boolean usbConnected;
    SurfaceView fpvView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide top bar and status bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbDeviceBroadcastReceiver = new UsbDeviceBroadcastReceiver(this);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbDeviceBroadcastReceiver, filter);
        IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbDeviceBroadcastReceiver, filterDetached);

        SurfaceView fpvView = findViewById(R.id.fpvView);
        frameHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //Log.d("NEW_FRAME", "got a new frame !");
                super.handleMessage(msg);
                Bitmap b = (Bitmap) msg.obj;
                double frameRatio = 16.0 / 9.0;

                Rect r = new Rect(0,
                        0,
                        (int) (fpvView.getHeight() * frameRatio),
                        fpvView.getHeight());
                displayFrame(fpvView, b, r);
            }
        };

        if (searchDevice()) {
            connect();
        }
    }

    @Override
    public void usbDeviceApproved(UsbDevice device) {
        Log.d("USB", "usbDevice approved");
        usbDevice = device;
        connect();
    }

    @Override
    public void usbDeviceDetached() {
        Log.d("USB", "usbDevice detached");
        // todo : properly stop threads/listeners?
        usbConnected = false;
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
        UsbMaskConnection mUsbMaskConnection = new UsbMaskConnection(usbManager.openDevice(usbDevice), usbDevice);
        mUsbMaskConnection.start();
        VideoReaderExoplayer mVideoReader = new VideoReaderExoplayer(mUsbMaskConnection.mInputStream, fpvView, getApplicationContext());
        mVideoReader.start();
    }

    protected void displayFrame(SurfaceView v, Bitmap f, Rect zone){
        if(f!=null){
            Canvas canvas = v.getHolder().lockCanvas();
            Rect frameRect =  new Rect(0, 0, f.getWidth(), f.getHeight());
            canvas.drawBitmap(f, frameRect,zone,new Paint(Color.BLUE));
            v.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        Log.d("ON_RESUME", "intent: " + intent);
        String action = intent.getAction();


        if (!usbConnected ) {
            //check to see if USB is now connected
            Log.d("RESUME_USB_CONNECTED", "not connected");
        }
    }

}