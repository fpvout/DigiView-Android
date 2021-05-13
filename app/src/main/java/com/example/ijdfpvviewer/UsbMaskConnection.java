package com.example.ijdfpvviewer;

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

    public UsbMaskConnection() {
    }

    public void setUsbDevice(UsbDeviceConnection c, UsbDevice d) {
        usbConnection = c;
        device = d;
        usbInterface = device.getInterface(3);

        Log.d("GET_USB_INTERFACE","Interface #3 (" + usbInterface.getName() + ")");
        usbConnection.claimInterface(usbInterface,true);

        mOutputStream = new AndroidUSBOutputStream(usbInterface.getEndpoint(0), usbConnection);
        mInputStream = new AndroidUSBInputStream(usbInterface.getEndpoint(1), usbConnection);
    }

    public void start(){
        mOutputStream.write(magicPacket);
        mInputStream.startReadThread();
    }

    public void stop() {
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
}
