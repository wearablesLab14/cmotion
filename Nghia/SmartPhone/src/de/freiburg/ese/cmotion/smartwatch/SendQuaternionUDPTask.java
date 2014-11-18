package de.freiburg.ese.cmotion.smartwatch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.os.AsyncTask;
import android.provider.Telephony.Sms.Conversations;
import android.util.Log;

public class SendQuaternionUDPTask extends AsyncTask<Object, Object, Object> {

	DatagramSocket s;

	@Override
	protected Object doInBackground(Object... params) {

		if (s == null)
			try {
				s = new DatagramSocket();
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		float[] quaternions = (float[]) params[0];
		String broadcastUriString = "192.168.0.255";
		int destPort = 5050;
		try {

			InetAddress local = InetAddress.getByName(broadcastUriString);

			int msgLength = quaternions.length;
			byte[] message = floatArray2ByteArray(quaternions);
			byte[] cMotionPaket = convertToCMotionHeader(message);
			msgLength = message.length;
			DatagramPacket p = new DatagramPacket(cMotionPaket,
					cMotionPaket.length, local, destPort);
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
			/*
			 * if (s != null) { s.close(); }
			 */
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
	 * 4 bytes header, 
	 * @param msg
	 * @return
	 */
	
	public static byte[] convertToCMotionHeader(byte[] msg) {
		try {
		byte[] result = new byte[32];
		
			int beginIndex = 4;

			int copyIndex = 0;
			for (int i = beginIndex; copyIndex < msg.length; i++) {
				result[i] = msg[copyIndex++];
			}

			// System.out.println("debug");
			return result;
		} catch (Exception e) {
			return null;
		}

	
	}
}
