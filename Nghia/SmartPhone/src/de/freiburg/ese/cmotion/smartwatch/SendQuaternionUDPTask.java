package de.freiburg.ese.cmotion.smartwatch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.os.AsyncTask;
import android.util.Log;

public class SendQuaternionUDPTask extends AsyncTask<Object, Object, Object> {

	DatagramSocket s;

	@Override
	protected Object doInBackground(Object... params) {

		float[] quaternions = (float[]) params[0];
		String broadcastUriString = "192.168.0.255";
		int destPort = 5050;
		byte[] message = floatArray2ByteArray(quaternions);
		byte[] cMotionPaket = convertToCMotionHeader(message);

		try {
			InetAddress local = InetAddress.getByName(broadcastUriString);
			DatagramPacket p = new DatagramPacket(cMotionPaket,
					cMotionPaket.length, local, destPort);
			
			s = GlobalState.getSocket();
			s.send(p);
		} catch (SocketException e) {
			Log.e("", "Unable to create socket.", e);
			// e.printStackTrace();
		} catch (UnknownHostException e) {
			Log.e("", "Host " + broadcastUriString + " is unknown.", e);
			// e.printStackTrace();
		} catch (IOException e) {
			Log.e("", "Failed to send UPD message.", e);
			// e.printStackTrace();
		}

		return null;
	}

	/**
	 * 
	 * @param values
	 * @return
	 */
	public byte[] floatArray2ByteArray(float[] values) {
		ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

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
		int beginIndex = 4;
		int copyIndex = 0;

		for (int i = beginIndex; copyIndex < msg.length; i++) {
			result[i] = msg[copyIndex++];
		}

		return result;
	}
}
