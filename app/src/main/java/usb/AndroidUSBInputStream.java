package usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Message;
import com.fpvout.digiview.helpers.DataListener;

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
	private DataListener inputListener = null;

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
	public AndroidUSBInputStream( UsbEndpoint readEndpoint, UsbEndpoint sendEndpoint, UsbDeviceConnection connection) {
		this.usbConnection = connection;
		this.receiveEndPoint = readEndpoint;
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
			if (inputListener != null) {
				byte[] bufferCopy = Arrays.copyOf(buffer, buffer.length);
				this.inputListener.calllback(bufferCopy, offset, bufferCopy.length);
			}
		}

		return receivedBytes;
	}

	public void setInputStreamListener(DataListener inputListener) {
		this.inputListener = inputListener;
	}


	@Override
	public void close() throws IOException {
		super.close();
	}

}
