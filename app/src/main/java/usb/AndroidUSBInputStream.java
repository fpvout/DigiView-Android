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

/**
 * This class acts as a wrapper to read data from the USB Interface in Android
 * behaving like an {@code InputputStream} class.
 */
public class AndroidUSBInputStream extends InputStream {

	// Constants.
	private static final int READ_BUFFER_SIZE = 50 * 1024 * 1024;
	private static final int OFFSET = 0;
	private static final int READ_TIMEOUT = 100;

	private static final String ERROR_THREAD_NOT_INITIALIZED = "Read thread not initialized, call first 'startReadThread()'";

	// Variables.
	private UsbDeviceConnection usbConnection;

	private UsbEndpoint receiveEndPoint;

	private boolean working = false;

	private Thread receiveThread;

	private CircularByteBuffer readBuffer;

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
	public AndroidUSBInputStream( UsbEndpoint readEndpoint, UsbDeviceConnection connection) {
		this.usbConnection = connection;
		this.receiveEndPoint = readEndpoint;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		byte[] buffer = new byte[1];
		read(buffer);
		return buffer[0] & 0xFF;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (readBuffer == null)
			throw new IOException(ERROR_THREAD_NOT_INITIALIZED);

		long deadLine = System.currentTimeMillis() + READ_TIMEOUT;
		int readBytes = 0;
		while (System.currentTimeMillis() < deadLine && readBytes <= 0)
			readBytes = readBuffer.read(buffer, offset, length);
		if (readBytes <= 0)
			return -1;
		byte[] readData = new byte[readBytes];
		System.arraycopy(buffer, offset, readData, 0, readBytes);
		//Log.d("USBInputStream","Received a read request of " + length + " bytes, returning " + readData.length + ": " + readData.toString());
		return readBytes;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		if (readBuffer == null)
			throw new IOException(ERROR_THREAD_NOT_INITIALIZED);

		return readBuffer.availableToRead();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long byteCount) throws IOException {
		if (readBuffer == null)
			throw new IOException(ERROR_THREAD_NOT_INITIALIZED);
		if(byteCount <= 0){
			return 0;
		}
		return readBuffer.skip((int)byteCount);
	}

	/**
	 * Starts the USB input stream read thread to start reading data from the
	 * USB Android connection.
	 * 
	 * @see #close()
	 */
	public void startReadThread() {
		if (!working) {
			working = true;
			readBuffer = new CircularByteBuffer(READ_BUFFER_SIZE);
			receiveThread = new Thread() {
				@Override
				public void run() {
					while (working) {
						byte[] buffer = new byte[1024];
						int receivedBytes = usbConnection.bulkTransfer(receiveEndPoint, buffer, buffer.length, READ_TIMEOUT) - OFFSET;
						if (receivedBytes > 0) {
							byte[] data = new byte[receivedBytes];
							System.arraycopy(buffer, OFFSET, data, 0, receivedBytes);
							//Log.d("USBInputStream","Message received: " + data.toString());
							readBuffer.write(buffer, OFFSET, receivedBytes);
						}
					}
				}
			};
			receiveThread.start();
		}
	}

	/**
	 * Stops the USB input stream read thread.
	 *
	 * @see #startReadThread()
	 */
	@Override
	public void close() throws IOException {
		working = false;
		if (receiveThread != null)
			receiveThread.interrupt();
		super.close();
	}
}
