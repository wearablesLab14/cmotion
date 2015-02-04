package de.freiburg.ese.cmotion.smartwatch;

import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;

/**
 * This class holds all registered rotation sensor data represented as
 * quaternions. Every registered sensor has to be unique by a given deviceID as
 * a string value. The stored sensors will be enumerated by an int value
 * starting at zero. Generally the local sensor - the device the app is running
 * - will be zero. If a registered sensor fails or the connection breaks down
 * completely the sensor data will be unchanged. After reconnecting the deviceID
 * should be the same and the data will be updated.
 * 
 * Currently removing sensor data resp. devices from stack is not provided!
 *
 */
public class SensorStack {

	private HashMap<String, SensorData> sensorDataMap;
	private int counter = 0;

	/**
	 * 
	 */
	public SensorStack() {
		sensorDataMap = new HashMap<String, SensorStack.SensorData>();
	}

	/**
	 * 
	 * @param deviceID
	 */
	public void registerSensor(String deviceID) {
		Log.d("PONY", "sensor registered " + deviceID);
		
		if(sensorDataMap.containsKey(deviceID)) {
			Log.d("PONY", "sensor already registered");
				return;
		}
		
		sensorDataMap.put(deviceID, new SensorData(deviceID, counter,
				new float[4]));
		counter++;
	}

	/**
	 * Updates an existing sensor data for a registered device. If device is
	 * unknown an exception will be raised.
	 * 
	 * @param deviceID
	 * @param quaternions
	 * @throws UnknownSensorDeviceException
	 */
	public void updateSensor(String deviceID, float[] quaternions)
			throws UnknownSensorDeviceException {
		if (sensorDataMap.containsKey(deviceID)) {
			sensorDataMap.get(deviceID).setQuaternions(quaternions);
			return;
		}

		throw new UnknownSensorDeviceException(
				"No sensor registered for device " + deviceID);
	}

	/**
	 * 
	 * @return
	 */
	public Iterator<SensorData> getSensorData() {
		return sensorDataMap.values().iterator();
	}

	/**
	 * Returns a specific sensor data. If device is unknown an exception will be
	 * raised.
	 * 
	 * @param deviceID
	 * @return
	 * @throws UnknownSensorDeviceException
	 */
	public SensorData getSensorByID(String deviceID) {
		if (sensorDataMap.containsKey(deviceID)) {
			return sensorDataMap.get(deviceID);
		}

		throw new UnknownSensorDeviceException(
				"No sensor registered for device " + deviceID);
	}
	
	/**
	 * 
	 * @return
	 */
	public int size() {
		return sensorDataMap.size();
	}

	/**
	 * 
	 *
	 */
	protected class SensorData {

		private String deviceID;
		private int sensorNo;
		private float[] quaternions;
		private boolean isAlive = true;

		/**
		 * 
		 * @param deviceID
		 * @param sensorNo
		 * @param quaternions
		 */
		public SensorData(String deviceID, int sensorNo, float[] quaternions) {
			this.deviceID = deviceID;
			this.sensorNo = sensorNo;
			this.quaternions = quaternions;
		}
		
		@Override
		public String toString() {
			return "SensorData [deviceID=" + deviceID + ", sensorNo="
					+ sensorNo + "]";
		}

		/**
		 * 
		 * @return
		 */
		public float[] getQuaternions() {
			return quaternions;
		}

		/**
		 * 
		 * @param quaternions
		 */
		public void setQuaternions(float[] quaternions) {
			this.quaternions = quaternions;
		}

		/**
		 * 
		 * @return
		 */
		public String getDeviceID() {
			return deviceID;
		}

		/**
		 * 
		 * @return
		 */
		public int getSensorNo() {
			return sensorNo;
		}

		/**
		 * 
		 * @return
		 */
		public boolean isAlive() {
			return isAlive;
		}

		/**
		 * 
		 */
		public void sleep() {
			isAlive = false;
		}

		/**
		 * 
		 */
		public void wakeUp() {
			isAlive = true;
		}

	}

}
