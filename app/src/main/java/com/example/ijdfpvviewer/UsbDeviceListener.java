package com.example.ijdfpvviewer;

import android.hardware.usb.UsbDevice;

public interface UsbDeviceListener {
    void usbDeviceApproved(UsbDevice device);
    void usbDeviceDetached();
}
