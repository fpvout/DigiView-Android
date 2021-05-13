package com.example.ijdfpvviewer;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

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
        Log.d("GET_USB_INTERFACE","Interface #3 (" + mUsbInterface.getName() + ")");
        usbConnection.claimInterface(mUsbInterface,true);
        getEndPoints(mUsbInterface);
        mInputStream = new AndroidUSBInputStream(mInEndpoint,usbConnection);
        mOutputStream = new AndroidUSBOutputStream(mOutEndpoint,usbConnection);

    }

    private void getEndPoints(UsbInterface u) {
        Log.d("GET_USB_ENDPOINTS","Endpoint Count " + u.getEndpointCount());
        mOutEndpoint = u.getEndpoint(0);
        mInEndpoint = u.getEndpoint(1);
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
