package com.example.ijdfpvviewer;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import java.io.IOException;
import java.io.InputStream;


import usb.AndroidUSBInputStream;
import usb.AndroidUSBOutputStream;

public class UsbMaskConnection extends InputStream {

    private byte[] magicPacket = "RMVT".getBytes();
    private UsbInterface mUsbInterface;
    private UsbEndpoint mInEndpoint;
    private UsbEndpoint mOutEndpoint;
    private UsbDeviceConnection usbConnection;
    AndroidUSBInputStream mInputStream;
    AndroidUSBOutputStream mOutputStream;

    public UsbMaskConnection(UsbDeviceConnection c, UsbDevice device) {
        usbConnection = c;
        mUsbInterface = device.getInterface(3);
        usbConnection.claimInterface(mUsbInterface,true);
        mInEndpoint = mUsbInterface.getEndpoint(1);
        mOutEndpoint = mUsbInterface.getEndpoint(0);
        AndroidUSBInputStream mInputStream = new AndroidUSBInputStream(mInEndpoint,usbConnection);
        AndroidUSBOutputStream mOutputStream = new AndroidUSBOutputStream(mOutEndpoint,usbConnection);

    }

    public void start(){
        mOutputStream.write(magicPacket);
        mInputStream.startReadThread();
    }

    @Override
    public int read() throws IOException {
        return mInputStream.read();
    }
}
