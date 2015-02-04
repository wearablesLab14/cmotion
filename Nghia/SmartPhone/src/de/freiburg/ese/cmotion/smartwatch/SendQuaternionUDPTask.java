package de.freiburg.ese.cmotion.smartwatch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import android.os.AsyncTask;
import android.util.Log;
import de.freiburg.ese.cmotion.smartwatch.SensorStack.SensorData;

/**
 * This class represents an asynchronous background task which sends
 * periodically all connected sensor data (local and connected devices) via UDP
 * packets.
 */
public class SendQuaternionUDPTask extends AsyncTask<Object, Object, Object> {

	private DatagramSocket s;
	private InetAddress address;
	private int destPort;
	private int updateDelay;
	private long msLastUpdateTime;
	private SensorData sensorData;

	private LinkedList<Long> times = new LinkedList<Long>();
	private final int MAX_SIZE = 100;
	private final double NANOS = 1000000000.0;

	/**
	 * 
	 * @param sensorData
	 * @param frameCounter
	 * @param uriString
	 * @param port
	 * @throws UnknownHostException
	 */
	public SendQuaternionUDPTask(SensorData sensorData, int frameCounter,
			String uriString, int port) throws UnknownHostException {
		this.sensorData = sensorData;
		address = InetAddress.getByName(uriString);
		destPort = port;
		updateDelay = 1000 / frameCounter;
		msLastUpdateTime = getCurrentTimeInMs();
		times.add(System.nanoTime());
	}

	private double fps() {
		long lastTime = System.nanoTime();
		double difference = (lastTime - times.getFirst()) / NANOS;
		times.addLast(lastTime);
		int size = times.size();
		if (size > MAX_SIZE) {
			times.removeFirst();
		}
		return difference > 0 ? times.size() / difference : 0.0;
	}

	@Override
	protected Object doInBackground(Object... params) {
		try {

			while (true) {

				// Escape early if cancel() is called
				if (isCancelled()) {
					break;
				}

				if (getCurrentTimeInMs() - msLastUpdateTime >= updateDelay) {
					msLastUpdateTime = getCurrentTimeInMs();

					Log.v("FPS", fps() + " fps");

					if (!sensorData.isAlive()) {
						Log.d(MainActivity.TAG, "Sensor '" + sensorData
								+ "' is sleeping...");
						continue;
					}

					byte[] cMotionPacket = convertToSendingPacket(sensorData);
					DatagramPacket p = new DatagramPacket(cMotionPacket,
							cMotionPacket.length, address, destPort);

					s = getSocket();
					s.send(p);
					Log.v(MainActivity.TAG,
							java.util.Arrays.toString(cMotionPacket));

				} else {
					Thread.sleep(updateDelay);
				}
			}
		} catch (SocketException e) {
			Log.e(MainActivity.TAG, "Unable to create socket.", e);
		} catch (UnknownHostException e) {
			Log.e(MainActivity.TAG, "Host " + address.getHostName()
					+ " is unknown.", e);
		} catch (IOException e) {
			Log.e(MainActivity.TAG, "Failed to send UPD message.", e);
		} catch (InterruptedException e) {
			Log.e(MainActivity.TAG, "Failed to send Thread to sleep.", e);
		}

		return null;
	}
	
	public DatagramSocket getSocket() throws SocketException {
		if (s == null || s.isClosed()) {
			s = new DatagramSocket();
			Log.d(MainActivity.TAG, "socket was null and will be created");
		}
		return s;
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
	 * @param sensorData
	 * @return
	 */
	public byte[] convertToSendingPacket(SensorData sensorData) {
		return convertToCMotionHeader(sensorData.getSensorNo(),
				floatArray2ByteArray(sensorData.getQuaternions()));
	}

	/**
	 * Transforms the a float array to a byte array using required byte order.
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
	 * A udp packets consists of a 32 bytes array. In human_cognition project
	 * for ROS the 4 bytes are intended for sending a timestamp. But in the
	 * current version it is not used. For supporting multiple sensor data from
	 * a single IP the first 4 bytes are used to transmit a device number.
	 * Therefore, the combination of IP and device no is unique. The next 16
	 * bytes contains the quaternion float data array. The last 12 bytes is
	 * resevered for correction data which will be currently empty
	 * 
	 * @param deviceNo
	 * @param msg
	 * @return
	 */
	protected byte[] convertToCMotionHeader(int deviceNo, byte[] msg) {
		byte[] result = new byte[32];

		// Transmits the device number. Local device is always zero
		byte[] bytesDeviceNo = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(deviceNo).array();
		System.arraycopy(bytesDeviceNo, 0, result, 0, 4);

		// copy quaternions
		int beginIndex = 4;
		int copyIndex = 0;

		for (int i = beginIndex; copyIndex < msg.length; i++) {
			result[i] = msg[copyIndex++];
		}

		return result;
	}
}
