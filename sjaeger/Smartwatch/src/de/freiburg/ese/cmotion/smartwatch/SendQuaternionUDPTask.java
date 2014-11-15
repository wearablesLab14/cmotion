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

	@Override
	protected Object doInBackground(Object... params) {

		float[] quaternions = (float[]) params[0];
		String broadcastUriString = (String) params[1];
		int destPort = (Integer) params[2];
		DatagramSocket s = null;

		try {
			s = new DatagramSocket();
			InetAddress local = InetAddress.getByName(broadcastUriString);

			// TODO determine proper format of udp packets
			int msgLength = quaternions.length;
			byte[] message = floatArray2ByteArray(quaternions);
			msgLength = message.length;
			System.out.println(message);
			System.out.println(message.length);
			DatagramPacket p = new DatagramPacket(message, msgLength, local,
					destPort);
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
		} finally {
			if (s != null) {
				s.close();
			}
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
}