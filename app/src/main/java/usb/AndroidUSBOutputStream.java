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
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

/**
 * This class acts as a wrapper to write data to the USB Interface in Android
 * behaving like an {@code OutputStream} class.
 */
public class AndroidUSBOutputStream extends OutputStream {

	// Constants.
	private static final int WRITE_TIMEOUT = 2000;
	
	// Variables.
	private UsbDeviceConnection usbConnection;

	private UsbEndpoint sendEndPoint;

	private LinkedBlockingQueue<byte[]> writeQueue;

	private boolean streamOpen = true;

	/**
	 * Class constructor. Instantiates a new {@code AndroidUSBOutputStream}
	 * object with the given parameters.
	 * 
	 * @param writeEndpoint The USB end point to use to write data to.
	 * @param connection The USB connection to use to write data to.
	 * 
	 * @see UsbDeviceConnection
	 * @see UsbEndpoint
	 */
	public AndroidUSBOutputStream(UsbEndpoint writeEndpoint, UsbDeviceConnection connection) {
		this.usbConnection = connection;
		this.sendEndPoint = writeEndpoint;

		writeQueue = new LinkedBlockingQueue<>(512);
		DataWriter dataWriter = new DataWriter();
		dataWriter.start();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int oneByte) {
		write(new byte[] {(byte)oneByte});
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] buffer) {
		write(buffer, 0, buffer.length);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] buffer, int offset, int count) {
		final byte[] finalData = new byte[count + offset];
		System.arraycopy(buffer, offset, finalData, 0, count);
		try {
			writeQueue.add(finalData);
		} catch (IllegalStateException  e) {
			Log.e("USBOutputStream","Could not add data, write queue is full: " + e.getMessage(), e);
		}
	}

	/**
	 * Internal class used to write data coming from a queue.
	 */
	class DataWriter extends Thread {
		@Override
		public void run() {
			while (streamOpen) {
				try {
					byte[] dataToWrite = writeQueue.poll(100, TimeUnit.MILLISECONDS);
					if (dataToWrite == null)
						continue;
					usbConnection.bulkTransfer(sendEndPoint, dataToWrite, dataToWrite.length, WRITE_TIMEOUT);
					Log.d("USBOutputStream","Message sent: " + dataToWrite.toString());
				} catch (InterruptedException e) {
					Log.e("USBOutputStream","Interrupted while getting data from the write queue: " + e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		// Stop the data writer.
		streamOpen = false;
		super.close();
	}
}
