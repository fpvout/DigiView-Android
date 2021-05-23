package usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class acts as a wrapper to write data to the USB Interface in Android
 * behaving like an {@code OutputStream} class.
 */
public class AndroidUSBOutputStream extends OutputStream {

	// Constants.
	private static final int WRITE_TIMEOUT = 2000;

	private final UsbDeviceConnection usbConnection;
	private final UsbEndpoint sendEndPoint;

	private final boolean streamOpen = true;

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
		usbConnection.bulkTransfer(sendEndPoint, buffer, count, WRITE_TIMEOUT);
	}

	@Override
	public void close() throws IOException {
		super.close();
	}
}
