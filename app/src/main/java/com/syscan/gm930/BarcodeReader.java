
package com.syscan.gm930;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import android_serialport_api.SerialPort;

public class BarcodeReader {

	private static final String TAG = "BarcodeReader";

	private SerialPort mSerialPort;

	private ReadThread mReadThread;
	private boolean mThreadRun;
	
	private Handler mMsgHandler;
	private String mFirmwareFile;
	
	private byte[] serial_cmd;
	private volatile SCAN_STATE mScanState=SCAN_STATE.OFF;
	
	private boolean trig_on=false;
	private boolean power_on=false;

	private boolean bContinueScan=false;
	private long mTimeOut=0;
	private long mStartTime=0;
	
	public enum SCAN_STATE {
		//0: 关闭; 1:准备好; 2:扫描; 3:命令
		OFF, READY, SCAN, CAP_IMAGE, UPGRADE, COMMAND
	};
	
	public enum MESSAGE_TYPE {
		MSG_BARCODE, MSG_COMMAND, 
		MSG_VERSION
	};
	
	static{
		System.loadLibrary("devapi");
		System.loadLibrary("BarcodeReader");
	}

	public void open(Handler msgHandler)
	{
		if (power_on) return;
		
		scaner_poweron();

		//等待模组上电
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		try {
			mSerialPort = new SerialPort();
			mSerialPort.Open("/dev/ttyMT0", 115200, 0);// scaner
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		serial_cmd = new byte[20];

		mMsgHandler = msgHandler;
		
		mThreadRun = true;
		mReadThread = new ReadThread();
		mReadThread.start(); // 开启读线程
		
		mScanState = SCAN_STATE.READY;
		bContinueScan = false;
		power_on = true;
	}

	public void close()
	{
		if (!power_on) return;
		if (mScanState==SCAN_STATE.UPGRADE) return;
		
		while (mReadThread.isAlive())
		{
			mThreadRun = false;
			stop_scan();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		mSerialPort.Close();
		scaner_poweroff();
		
		power_on = false;
	}
	
	public void stop_scan()
	{
		if (mScanState==SCAN_STATE.CAP_IMAGE
				|| mScanState==SCAN_STATE.UPGRADE)
			return;

		mScanState = SCAN_STATE.READY;

		scaner_trigoff();
	}

	public void single_scan(int timeout)
	{
		if (mScanState!=SCAN_STATE.READY) return;
		mTimeOut = timeout;
		mStartTime = System.currentTimeMillis();
		bContinueScan = false;
		mScanState = SCAN_STATE.SCAN;
		//Log.w(TAG, "single_scan");
		
		scaner_trigoff();
		scaner_trigon();
	}
	
	public void continuous_scan()
	{
		if (mScanState!=SCAN_STATE.READY) return;
		mTimeOut = 5000;
		mStartTime = System.currentTimeMillis();
		bContinueScan = true;
		mScanState = SCAN_STATE.SCAN;

		scaner_trigoff();
		scaner_trigon();
	}

	public void get_firmware_version()
	{
		if (mScanState!=SCAN_STATE.READY) return;
		
		scaner_trigoff();

		mScanState = SCAN_STATE.COMMAND;

		serial_cmd[0] = 0x52;
		serial_cmd[1] = 0x00;
		serial_cmd[2] = 0x20;
		serial_cmd[3] = 0;
		Log.w(TAG,"get_firmware_version");

		mSerialPort.Write(serial_cmd, 0, 4);
	}
	
	private void scaner_poweron() {
		Log.w(TAG, "scan power on");
		scanerpoweron();
		scaner_trigoff();
	}
	private void scaner_poweroff() {
		Log.w(TAG, "scan power off");
		scanerpoweroff();
		//scaner_trigoff();
	}
	private void scaner_trigon() {
		scanertrigeron();
		trig_on=true;
	}
	private void scaner_trigoff() {
		scanertrigeroff();
		trig_on=false;
	}
	private boolean scaner_trig_stat(){
		return trig_on;
	}

	//
	private class ReadThread extends Thread {

		private void parse_cmd(byte[] cmd, int cmd_size)
		{
			if (cmd_size<4) return;
			if (cmd[0]!=0x52) return;
			switch (cmd[1])
			{
			case 0x00:
				String ver="";
				try {
					ver = new String(cmd, 4, cmd_size-4, "GB18030");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				Message toMain = mMsgHandler.obtainMessage();
				toMain.obj = ver;
				toMain.what = MESSAGE_TYPE.MSG_VERSION.ordinal();

				mMsgHandler.sendMessage(toMain);
				mScanState=SCAN_STATE.READY;
				break;
			default:
				break;
			}
		}
		@Override
		public void run() {
			super.run();
			Log.w(TAG, "ReadThread start mThreadRun ="+mThreadRun);

			int size;
			long t1;
			byte[] buffer = new byte[4096];
			String data;
			
			while (mThreadRun) {
				if (mScanState == SCAN_STATE.SCAN)
				{
					t1 = System.currentTimeMillis();
					if (t1-mStartTime>mTimeOut)
					{
						scaner_trigoff();
						if (bContinueScan)
						{
							mStartTime = t1;
							scaner_trigon();
							Log.w(TAG, "Continue scaner_trigon");
						}
						else
							mScanState = SCAN_STATE.READY;
					}
				}
				try {
					size = mSerialPort.Read(buffer);
					//Log.w(TAG, "ReadThread read:"+size);
					if (size<=0) continue;
					//Log.w(TAG, "ReadThread read:"+size+"; state:"+mScanState+" img_len:"+img_len);
					
					switch (mScanState)
					{
					case SCAN://数据
						data = new String(buffer, 0, size, "GB18030");

						if(data != null && data.length() != 0 ){
							Message toMain = mMsgHandler.obtainMessage();
							toMain.obj = data;
							toMain.what = MESSAGE_TYPE.MSG_BARCODE.ordinal();

							mMsgHandler.sendMessage(toMain);
							data = null;
						}
						scaner_trigoff();
						if (bContinueScan)
						{
							mStartTime = System.currentTimeMillis();
							scaner_trigon();
						}
						else
							mScanState = SCAN_STATE.READY;
						break;
					case COMMAND:
						parse_cmd(buffer, size);
						break;
					default:
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
			Log.w(TAG, "ReadThread stop");
		}
	}
	
	// JNI
	public native void scanerpoweron();
	public native void scanerpoweroff();
	public native void scanertrigeron();
	public native void scanertrigeroff();
	
}
