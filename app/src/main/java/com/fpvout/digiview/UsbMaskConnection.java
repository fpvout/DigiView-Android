package com.fpvout.digiview;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.fpvout.digiview.dvr.DVR;

import java.io.IOException;
import java.util.HashMap;

import usb.AndroidUSBInputStream;
import usb.AndroidUSBOutputStream;

public class UsbMaskConnection {

    public static final String ACTION_USB_PERMISSION = "com.fpvout.digiview.USB_PERMISSION";
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    private final byte[] magicPacket = "RMVT".getBytes();
    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    AndroidUSBInputStream mInputStream;
    AndroidUSBOutputStream mOutputStream;
    private boolean ready = false;

    public UsbMaskConnection() {
    }

    public static UsbDevice searchDevice(UsbManager usbManager, Context c) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(c, 0, new Intent(ACTION_USB_PERMISSION), 0);

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.size() <= 0) {
            return null;
        }

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID) {
                if (usbManager.hasPermission(device)) {
                    return device;
                }
                usbManager.requestPermission(device, permissionIntent);
            }
        }
        return null;
    }

    public void start(){
        mOutputStream.write(magicPacket);
    }

    public void stop() {
        ready = false;
        try {
            if (mInputStream != null)
                mInputStream.close();

            if (mOutputStream != null)
                mOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (usbConnection != null) {
            usbConnection.releaseInterface(usbInterface);
            usbConnection.close();
        }
    }

    public boolean isReady() {
        return ready;
    }

    public void setUsbDevice(UsbManager usbManager, UsbDevice d) {
        usbConnection = usbManager.openDevice(d);
        usbInterface = d.getInterface(3);

        usbConnection.claimInterface(usbInterface, true);

        mOutputStream = new AndroidUSBOutputStream(usbInterface.getEndpoint(0), usbConnection);
        mInputStream = new AndroidUSBInputStream(usbInterface.getEndpoint(1), usbInterface.getEndpoint(0), usbConnection);
        ready = true;
    }
}
