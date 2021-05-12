package com.example.ijdfpvviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    UsbManager mUsbManager;
    UsbDevice device;
    boolean usbConnected;
    Handler frameHandler;

    protected void initialize() {
        Log.d("INIT", "init !");
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        setContentView(R.layout.activity_main);

        run();
    }

    protected void run() {
        Log.d("RUN", "run !");

        SurfaceView fpvView = findViewById(R.id.fpvView);
        Button connectButton = findViewById(R.id.connectButton);
        frameHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d("NEW_FRAME", "got a new frame !");
                super.handleMessage(msg);
                Bitmap b = (Bitmap) msg.obj;
                double frameRatio = 16.0 / 9.0;

                Rect r = new Rect(0,
                        b.getHeight(),
                        fpvView.getWidth(),
                        (int) (fpvView.getWidth() / frameRatio));
                displayFrame(fpvView, b, r);
            }
        };

        connectButton.setOnClickListener(vw -> {
            if(device == null) {
                Log.d("DEVICE", "no device !");
                Toast.makeText(getApplicationContext(), "no device !", Toast.LENGTH_SHORT).show();
            }else{
                Log.d("DEVICE", device.getDeviceName() + " " +device.getManufacturerName()+ " " +device.getProductName());
                Toast.makeText(getApplicationContext(), device.getDeviceName() + " " +device.getManufacturerName()+ " " +device.getProductName(), Toast.LENGTH_SHORT).show();
            }
        });
        Log.d("INITIALIZED", "app running !");

    }

    private void connect(){
        UsbMaskConnection mUsbMaskConnection = new UsbMaskConnection(mUsbManager.openDevice(device), device);
        mUsbMaskConnection.start();
        VideoReader mVideoReader = new VideoReader(mUsbMaskConnection,frameHandler);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
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


    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // Permissions that need to be explicitly requested from end user.
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            /*
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
            */};


    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ON_USB_ACTION",action);

            Toast.makeText(context, action,Toast.LENGTH_SHORT).show();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                usbConnected=false;
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            connect();
                        }
                    }
                    else {
                        Log.d("USB_PERMISSION", "permission denied for device " + device);
                    }
                }
            }
        }
    };

}