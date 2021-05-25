package usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import com.fpvout.digiview.dvr.DVR;

/**
 * This class acts as a wrapper to read data from the USB Interface in Android
 * behaving like an {@code InputputStream} class.
 */
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class AndroidUSBInputStream extends InputStream {

	private static final int READ_TIMEOUT = 100;

	private final UsbDeviceConnection usbConnection;
	private final UsbEndpoint receiveEndPoint;
	private final UsbEndpoint sendEndPoint;

	private boolean working = false;


	private DVR dvr;

	/**
	 * Class constructor. Instantiates a new {@code AndroidUSBInputStream}
	 * object with the given parameters.
	 *
	 * @param readEndpoint The USB end point to use to read data from.
	 * @param sendEndpoint The USB end point to use to sent data to.
	 * @param connection   The USB connection to use to read data from.
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
	public int read() {
		byte[] buffer = new byte[131072];
		return read(buffer, 0, buffer.length);
	}

	@Override
	public int read(byte[] buffer, int offset, int length) {
		int receivedBytes = usbConnection.bulkTransfer(receiveEndPoint, buffer, buffer.length, READ_TIMEOUT);
		if (receivedBytes <= 0) {
			// send magic packet again; Would be great to handle this in UsbMaskConnection directly...
			//Log.d(TAG, "received buffer empty, sending magic packet again...");
			usbConnection.bulkTransfer(sendEndPoint, "RMVT".getBytes(), "RMVT".getBytes().length, 2000);
			receivedBytes = usbConnection.bulkTransfer(receiveEndPoint, buffer, buffer.length, READ_TIMEOUT);
		} else {
			byte[] copiedBuffer = Arrays.copyOf(buffer, buffer.length);
			this.dvr.recordVideoDVR(copiedBuffer,offset , copiedBuffer.length);
		}

		return receivedBytes;
	}


	@Override
	public void close() throws IOException {
		super.close();
	}

}
