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

/**
 * Helper class used to store data bytes as a circular buffer.
 */
public class CircularByteBuffer {

	// Variables.
	private byte[] buffer;

	private int readIndex;
	private int writeIndex;
	
	private boolean empty = true;

	/**
	 * Instantiates a new {@code CircularByteBuffer} with the given capacity
	 * in bytes.
	 * 
	 * @param size Circular byte buffer size in bytes.
	 * 
	 * @throws IllegalArgumentException if {@code size < 1}.
	 */
	public CircularByteBuffer(int size) {
		if (size < 1)
			throw new IllegalArgumentException("Buffer size must be greater than 0.");
		
		buffer = new byte[size];
		readIndex = 0;
		writeIndex = 0;
	}

	/**
	 * Writes the given amount of bytes to the circular byte buffer.
	 * 
	 * @param data Bytes to write.
	 * @param offset Offset inside data where bytes to write start.
	 * @param numBytes Number of bytes to write.
	 * @return The number of bytes actually written.
	 * 
	 * @throws IllegalArgumentException if {@code offset < 0} or
	 *                                  if {@code numBytes < 1}.
	 * @throws NullPointerException if {@code data == null}.
	 * 
	 * @see #read(byte[], int, int)
	 * @see #skip(int)
	 */
	public synchronized int write(byte[] data, int offset, int numBytes) {
		if (data == null)
			throw new NullPointerException("Data cannot be null.");
		if (offset < 0)
			throw new IllegalArgumentException("Offset cannot be negative.");
		if (numBytes < 1)
			throw new IllegalArgumentException("Number of bytes to write must be greater than 0.");
		
		// Check if there are enough bytes to write.
		int availableBytes = data.length - offset;
		if (numBytes > availableBytes)
			numBytes = availableBytes;
		
		// Check where we should start writing.
		if (numBytes < buffer.length - getWriteIndex()) {
			System.arraycopy(data, offset, buffer, getWriteIndex(), numBytes);
			writeIndex = getWriteIndex() + numBytes;
		} else {
			System.arraycopy(data, offset, buffer, getWriteIndex(), buffer.length - getWriteIndex());
			System.arraycopy(data, offset + buffer.length-getWriteIndex(), buffer, 0, numBytes - (buffer.length - getWriteIndex()));
			writeIndex = numBytes - (buffer.length-getWriteIndex());
			if (getReadIndex() < getWriteIndex())
				readIndex = getWriteIndex();
		}
		
		// Check if we were able to write all the bytes.
		if (numBytes > getCapacity())
			numBytes = getCapacity();
		
		empty = false;
		return numBytes;
	}

	/**
	 * Reads the given amount of bytes to the given array from the circular byte
	 * buffer.
	 * 
	 * @param data Byte buffer to place read bytes in.
	 * @param offset Offset inside data to start placing read bytes in.
	 * @param numBytes Number of bytes to read.
	 * @return The number of bytes actually read.
	 * 
	 * @throws IllegalArgumentException if {@code offset < 0} or
	 *                                  if {@code numBytes < 1}.
	 * @throws NullPointerException if {@code data == null}.
	 * 
	 * @see #skip(int)
	 * @see #write(byte[], int, int)
	 */
	public synchronized int read(byte[] data, int offset, int numBytes) {
		if (data == null)
			throw new NullPointerException("Data cannot be null.");
		if (offset < 0)
			throw new IllegalArgumentException("Offset cannot be negative.");
		if (numBytes < 1)
			throw new IllegalArgumentException("Number of bytes to read must be greater than 0.");
		
		// If we are empty, return 0.
		if (empty)
			return 0;
		
		// If we try to place bytes in an index bigger than buffer index, return 0 read bytes.
		if (offset >= data.length)
			return 0;
		
		if (data.length - offset < numBytes)
			return read(data, offset, data.length - offset);
		if (availableToRead() < numBytes)
			return read(data, offset, availableToRead());
		if (numBytes < buffer.length - getReadIndex()){
			System.arraycopy(buffer, getReadIndex(), data, offset, numBytes);
			readIndex = getReadIndex() + numBytes;
		} else {
			System.arraycopy(buffer, getReadIndex(), data, offset, buffer.length - getReadIndex());
			System.arraycopy(buffer, 0, data, offset + buffer.length - getReadIndex(), numBytes - (buffer.length - getReadIndex()));
			readIndex = numBytes-(buffer.length - getReadIndex());
		}
		
		// If we have read all bytes, set the buffer as empty.
		if (readIndex == writeIndex)
			empty = true;
		
		return numBytes;
	}

	/**
	 * Skips the given number of bytes from the circular byte buffer.
	 * 
	 * @param numBytes Number of bytes to skip.
	 * @return The number of bytes actually skipped.
	 * 
	 * @throws IllegalArgumentException if {@code numBytes < 1}.
	 * 
	 * @see #read(byte[], int, int)
	 * @see #write(byte[], int, int)
	 */
	public synchronized int skip(int numBytes) {
		if (numBytes < 1)
			throw new IllegalArgumentException("Number of bytes to skip must be greater than 0.");
		
		// If we are empty, return 0.
		if (empty)
			return 0;
		
		if (availableToRead() < numBytes)
			return skip(availableToRead());
		if (numBytes < buffer.length - getReadIndex())
			readIndex = getReadIndex() + numBytes;
		else
			readIndex = numBytes - (buffer.length - getReadIndex());
		
		// If we have skipped all bytes, set the buffer as empty.
		if (readIndex == writeIndex)
			empty = true;
		
		return numBytes;
	}

	/**
	 * Returns the available number of bytes to read from the byte buffer.
	 * 
	 * @return The number of bytes in the buffer available for reading.
	 * 
	 * @see #getCapacity()
	 * @see #read(byte[], int, int)
	 */
	public int availableToRead() {
		if (empty)
			return 0;
		if (getReadIndex() < getWriteIndex())
			return (getWriteIndex() - getReadIndex());
		else
			return (buffer.length - getReadIndex() + getWriteIndex());
	}

	/**
	 * Returns the current read index.
	 * 
	 * @return readIndex The current read index.
	 */
	private int getReadIndex() {
		return readIndex;
	}

	/**
	 * Returns the current write index.
	 * 
	 * @return writeIndex The current write index.
	 */
	private int getWriteIndex() {
		return writeIndex;
	}
	
	/**
	 * Returns the circular byte buffer capacity.
	 * 
	 * @return The circular byte buffer capacity.
	 */
	public int getCapacity() {
		return buffer.length;
	}
	
	/**
	 * Clears the circular buffer.
	 */
	public void clearBuffer() {
		empty = true;
		readIndex = 0;
		writeIndex = 0;
	}
}