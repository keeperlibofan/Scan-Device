/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android_serialport_api;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

public class SerialPort {

	private static final String TAG = "SerialPort";
	
	private static final int O_NOCTTY=00000400;
	private static final int O_NDELAY=00004000;

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;

	public SerialPort()
	{
	}
	
	public void Open(String devicename, int baudrate, int flags) throws SecurityException, IOException 
	{
		File device = new File(devicename);
		/* Check access permission */
		if (!device.canRead() || !device.canWrite()) {
			try {
				/* Missing read/write permission, trying to chmod the file */
				Process su;
				su = Runtime.getRuntime().exec("/system/bin/su");
				String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
						+ "exit\n";
				su.getOutputStream().write(cmd.getBytes());
				if ((su.waitFor() != 0) || !device.canRead()
						|| !device.canWrite()) {
					throw new SecurityException();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new SecurityException();
			}
		}

		mFd = open(device.getAbsolutePath(), baudrate, flags|O_NOCTTY|O_NDELAY);
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
	}
	
	public void Close()
	{
		close();
		mFd = null;
		mFileInputStream = null;
		mFileOutputStream = null;
	}

	public int Read(byte[] buffer, int offset, int read_len)
	{
		int len=0;
		try {
			len = mFileInputStream.read(buffer, offset, read_len);
		} catch (IOException e) {
			Log.w("BarcodeReader", e.toString());
		}
		return len;
	}

	public int Read(byte[] buffer)
	{
		return Read(buffer, 0, buffer.length);
	}
	
	public int Write(byte[] buffer, int offset, int write_len)
	{
		try {
			mFileOutputStream.write(buffer, offset, write_len);
			return write_len;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public int Write(byte[] buffer)
	{
		return Write(buffer, 0, buffer.length);
	}
	
	// JNI
	private native static FileDescriptor open(String path, int baudrate, int flags);
	public native void close();
	static {
		System.loadLibrary("serial_port");
	}
}
