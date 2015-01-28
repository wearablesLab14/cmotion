package de.freiburg.ese.cmotion.smartwatch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.AsyncTask;
import android.util.Log;

/**
 * This class represents an asynchronous background task which sends
 * periodically all connected sensor data (local and connected devices) via udp
 * packets.
 */
public class SendQuaternionUDPTask extends AsyncTask<Object, Object, Object> {

	DatagramSocket s;
	InetAddress address;
	int destPort;
	int updateDelay;
	long msLastUpdateTime;
	float[] lastReceiveData = new float[4];

	/**
	 * 
	 * @param frameCounter
	 * @param uriString
	 * @param port
	 * @throws UnknownHostException
	 */
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

					byte[] cMotionPacket = convertToSendingPaket(0);
					DatagramPacket p = new DatagramPacket(cMotionPacket,
							cMotionPacket.length, address, destPort);

					s = GlobalState.getSocket();
					s.send(p);
					Log.d(MainActivity.TAG, java.util.Arrays.toString(cMotionPacket));

					if (MainActivity.multiSensor) {
						byte[] cMotionPacket2 = convertToSendingPaket(1123);
						DatagramPacket p2 = new DatagramPacket(cMotionPacket2,
								cMotionPacket2.length, address, destPort);

						s = GlobalState.getSocket();
						s.send(p2);
						Log.d(MainActivity.TAG, java.util.Arrays.toString(cMotionPacket2));
					}

				} else {
					Thread.sleep(updateDelay);
				}
			}
		} catch (SocketException e) {
			Log.e(MainActivity.TAG, "Unable to create socket.", e);
		} catch (UnknownHostException e) {
			Log.e(MainActivity.TAG, "Host " + address.getHostName() + " is unknown.", e);
		} catch (IOException e) {
			Log.e(MainActivity.TAG, "Failed to send UPD message.", e);
		} catch (InterruptedException e) {
			Log.e(MainActivity.TAG, "Failed to send Thread to sleep.", e);
		}

		return null;
	}

	/**
	 * 
	 * @return
	 */
	public static long getCurrentTimeInMs() {
		return System.currentTimeMillis();
	}

	/**
	 * 
	 * @return true, if only got new data
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

		return false;
	}

	/**
	 * float to byte
	 * 
	 * @return
	 */
	public byte[] convertToSendingPaket(int deviceID) {
		return convertToCMotionHeader(deviceID,
				floatArray2ByteArray(this.lastReceiveData));
	}

	/**
	 * 
	 * @param values
	 * @return
	 */
	public byte[] floatArray2ByteArray(float[] values) {
		ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		for (float value : values) {
			buffer.putFloat(value);
		}

		return buffer.array();
	}

	/**
	 * 4 bytes timestamp: only last 4 bytes of timestamp 16 bytes quaternions:
	 * data from rotation sensor 12 bytes correction data: currently empty
	 * 
	 * UPDATE first 4 bytes determines the device ID
	 * 
	 * @param msg
	 * @return
	 */
	protected byte[] convertToCMotionHeader(int deviceID, byte[] msg) {
		byte[] result = new byte[32];

		// copy timestamp
		// byte[] bytesTest = ByteBuffer.allocate(8).putLong(msLastUpdateTime)
		// .array();
		// System.arraycopy(bytesTest, 4, result, 0, 4);

		// Transmits the sender ID. Local device is always zero
		byte[] bytesDeviceID = ByteBuffer.allocate(4).putInt(deviceID).array();
		System.arraycopy(bytesDeviceID, 0, result, 0, 4);

		// copy quaternions
		int beginIndex = 4;
		int copyIndex = 0;

		for (int i = beginIndex; copyIndex < msg.length; i++) {
			result[i] = msg[copyIndex++];
		}

		return result;
	}
}
