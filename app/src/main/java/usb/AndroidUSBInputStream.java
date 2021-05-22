/*
 * Copyright 2019, Digi International Inc.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES 
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR 
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES 
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN 
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF 
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package usb;

import java.io.IOException;
import java.io.InputStream;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import com.fpvout.digiview.dvr.DVR;

/**
 * This class acts as a wrapper to read data from the USB Interface in Android
 * behaving like an {@code InputputStream} class.
 */
public class AndroidUSBInputStream extends InputStream {

	private final String TAG = "USBInputStream";
	// Constants.
	private static final int OFFSET = 0;
	private static final int READ_TIMEOUT = 100;

	// Variables.
	private UsbDeviceConnection usbConnection;

	private UsbEndpoint receiveEndPoint;
	private final UsbEndpoint sendEndPoint;

	private boolean working = false;


	private DVR dvr;

	/**
	 * Class constructor. Instantiates a new {@code AndroidUSBInputStream}
	 * object with the given parameters.
	 *
	 * @param readEndpoint The USB end point to use to read data from.
	 * @param connection The USB connection to use to read data from.
	 *
	 * @see UsbDeviceConnection
	 * @see UsbEndpoint
	 */
	public AndroidUSBInputStream( UsbEndpoint readEndpoint, UsbEndpoint sendEndpoint, UsbDeviceConnection connection, DVR dvr) {
		this.usbConnection = connection;
		this.receiveEndPoint = readEndpoint;
		this.dvr = dvr;
		this.sendEndPoint = sendEndpoint;
	}

	@Override
	public int read() throws IOException {
		byte[] buffer = new byte[131072];
		return read(buffer, 0,  buffer.length);
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int receivedBytes = usbConnection.bulkTransfer(receiveEndPoint, buffer, buffer.length, READ_TIMEOUT);
		if (receivedBytes <= 0) {
			// send magic packet again; Would be great to handle this in UsbMaskConnection directly...
			Log.d(TAG, "received buffer empty, sending magic packet again...");
			usbConnection.bulkTransfer(sendEndPoint, "RMVT".getBytes(), "RMVT".getBytes().length, 2000);
			receivedBytes = usbConnection.bulkTransfer(receiveEndPoint, buffer, buffer.length, READ_TIMEOUT);
		} else {
			this.dvr.recordVideoDVR(buffer,offset , buffer.length);
		}

		return receivedBytes;
	}


	@Override
	public void close() throws IOException {}

}
