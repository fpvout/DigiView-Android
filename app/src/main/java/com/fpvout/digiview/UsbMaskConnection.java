package com.fpvout.digiview;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;

import usb.AndroidUSBInputStream;
import usb.AndroidUSBOutputStream;

public class UsbMaskConnection {

    private byte[] magicPacket = "RMVT".getBytes();
    private UsbDeviceConnection usbConnection;
    private UsbDevice device;
    private UsbInterface usbInterface;
    AndroidUSBInputStream mInputStream;
    AndroidUSBOutputStream mOutputStream;
    private boolean ready = false;

    public UsbMaskConnection() {
    }

    public void setUsbDevice(UsbDeviceConnection c, UsbDevice d) {
        usbConnection = c;
        device = d;
        usbInterface = device.getInterface(3);

        usbConnection.claimInterface(usbInterface,true);

        mOutputStream = new AndroidUSBOutputStream(usbInterface.getEndpoint(0), usbConnection);
        mInputStream = new AndroidUSBInputStream(usbInterface.getEndpoint(1), usbConnection);
        ready = true;
    }

    public void start(){
        mOutputStream.write(magicPacket);
        mInputStream.startReadThread();
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
}
