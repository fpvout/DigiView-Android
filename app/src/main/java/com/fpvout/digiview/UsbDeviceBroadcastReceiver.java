package com.fpvout.digiview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbDeviceBroadcastReceiver extends BroadcastReceiver {
    private static final String ACTION_USB_PERMISSION = "com.example.ijdfpvviewer.USB_PERMISSION";
    private final UsbDeviceListener listener;

    public UsbDeviceBroadcastReceiver(UsbDeviceListener listener ){
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                if(device != null){
                    Log.d("UsbDeviceBroadcastReceiver", "Usb device approved");
                    listener.usbDeviceApproved(device);
                }
            }
        }

        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            listener.usbDeviceDetached();
        }
    }
}
