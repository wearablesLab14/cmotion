package de.freiburg.ese.cmotion.smartwatch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Currency;
import java.util.Date;

import android.R.integer;
import android.os.AsyncTask;
import android.text.method.DateTimeKeyListener;
import android.util.Log;

public class SendQuaternionUDPTask extends AsyncTask<Object, Object, Object> {

	DatagramSocket s;

	InetAddress address;
	int destPort;
	float[] lastReceiveData = new float[4];
	int updateDelay;
	long msLastUpdateTime;

	public SendQuaternionUDPTask(int frameCounter, String uriString, int port)
			throws UnknownHostException {
		address = InetAddress.getByName(uriString);
		destPort = port;
		updateDelay = 1000 / frameCounter;
		msLastUpdateTime = getCurrentTimeInMs();

	}

	@Override
	protected Object doInBackground(Object... params) {
		try {
			while (true) {
				if (getCurrentTimeInMs() - msLastUpdateTime >= updateDelay
						&& getDataQQ()) {

					msLastUpdateTime = getCurrentTimeInMs();
					
					byte[] cMotionPaket = convertToSendingPaket();
					DatagramPacket p = new DatagramPacket(cMotionPaket,
							cMotionPaket.length, address, destPort);

					s = GlobalState.getSocket();
					s.send(p);

					// update;
				

				} else {
					// sleep
					Thread.sleep(updateDelay);
				}
			}
		} catch (SocketException e) {
			Log.e("", "Unable to create socket.", e);
			// e.printStackTrace();
		} catch (UnknownHostException e) {
			Log.e("", "Host " + address.getHostName() + " is unknown.", e);
			// e.printStackTrace();
		} catch (IOException e) {
			Log.e("", "Failed to send UPD message.", e);
			// e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * return true, if got new datas
	 */
	private boolean getDataQQ() {
		float[] qq = MainActivity.getSensorData();
		if (qq != null && this.lastReceiveData != null
				&& qq.length == this.lastReceiveData.length) {
			for (int i = 0; i < qq.length; i++) {
				if (qq[i] != this.lastReceiveData[i]) {
					lastReceiveData = qq;
					return true;
				}
			}
		}
		
		// todo  return false
		return false;
	}

	public byte[] convertToSendingPaket() {

		// float to byte
		return convertToCMotionHeader(floatArray2ByteArray(this.lastReceiveData));

	}

	public static long getCurrentTimeInMs() {
		return System.currentTimeMillis();
	}

	/**
	 * 
	 * @param values
	 * @return
	 */
	public byte[] floatArray2ByteArray(float[] values) {
		ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);
		byte[] bytesTest = ByteBuffer.allocate(4).putLong(msLastUpdateTime).array();
		
		for (float value : values) {
			buffer.putFloat(value);
		}

		return buffer.array();
	}

	/**
	 * 4 bytes timestamp, 16 bytes quaternions, 12 bytes correction data
	 * 
	 * @param msg
	 * @return
	 */
	public static byte[] convertToCMotionHeader(byte[] msg) {
			
		
		byte[] result = new byte[32];
		// long to byte
		
		int beginIndex = 4;
		int copyIndex = 0;

		for (int i = beginIndex; copyIndex < msg.length; i++) {
			result[i] = msg[copyIndex++];
		}

		return result;
	}
}
