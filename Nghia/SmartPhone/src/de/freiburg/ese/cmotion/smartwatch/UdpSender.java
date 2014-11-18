package de.freiburg.ese.cmotion.smartwatch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.util.Log;

public  class UdpSender {

 
// private static int destPort ;
// private static String broadcastUriString ;
// private static  DatagramSocket s  ;

	public  void Init(String ip, int port) throws SocketException 
 {	 
		// destPort = port;
		//  broadcastUriString  = ip;
		// f(s == null)
		// s = new DatagramSocket();
 }
	public   void sendContent (float[] values)
	{
		 DatagramSocket s = null;
		if(s == null)
			try {
				s=  new DatagramSocket();
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
		float[] quaternions = values;
		String broadcastUriString = "192.168.0.255";
				int destPort = 5050;
		try {
			
			InetAddress local = InetAddress.getByName(broadcastUriString);
			// int msgLength = quaternions.length;
			byte[] message = floatArray2ByteArray(quaternions);
			byte  [] cMotionPaket = convertToCMotionHeader(message);			
			// msgLength = message.length;
			DatagramPacket p = new DatagramPacket(cMotionPaket, cMotionPaket.length, local,
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
			/*
			if (s != null) {
				s.close();
			}
			*/
		}
	}
 
 
	/**
	 * 
	 * @param values
	 * @return
	 */
	public  byte[] floatArray2ByteArray(float[] values) {
		ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

		for (float value : values) {
			buffer.putFloat(value);
		}

		return buffer.array();
	}
	
	public  byte[] convertToCMotionHeader(byte[] msg)
	{
		byte[] result  = new  byte[32];
		try
		{
		int beginIndex  = 2;

		int copyIndex   = 0;
		for (int i  = beginIndex; i < (beginIndex + msg.length); i++  )
		{
			result[i] = msg[copyIndex++];
		}
		}catch (Exception e)
		{
			return null;
		}
		
		return result;
	}
	

}
