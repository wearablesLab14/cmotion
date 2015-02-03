package de.freiburg.ese.cmotion.smartwatch;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

import de.freiburg.ese.cmotion.smartwatch.SensorStack.SensorData;
import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	protected static final String TAG = "CMOTION";
	private static final String DEVICE_ID = UUID.randomUUID().toString();
	private static final int UDP_PORT = 5050;
	private static final String UDP_DEST = "192.168.0.255";
	private static final int FRAME_RATE = 60;

	private Button button;
	private Button buttonTestMulti; // Testfunc for sending multiple data
	private TextView txtSensorData;
	private TextView txtSensorInfo;
	private boolean isSending = true;
	private SensorManager sensorManager;
	private HashSet<SendQuaternionUDPTask> asyncStack = new HashSet<SendQuaternionUDPTask>();
	private static SensorStack sensorStack = new SensorStack();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		initSensorListeners();

		button = (Button) findViewById(R.id.btn_sending);
		button.setBackgroundColor(Color.RED);
		button.setOnClickListener(btnSendUDPListener);
		txtSensorData = (TextView) findViewById(R.id.textView_sensorData);
		txtSensorData.setTextSize(12);
		txtSensorInfo = (TextView) findViewById(R.id.textView_info);
		txtSensorInfo.setTextSize(12);

		// TEMPORARY Test Button for multiple sensor data
		buttonTestMulti = (Button) findViewById(R.id.button2);
		buttonTestMulti.setBackgroundColor(Color.RED);
		buttonTestMulti.setOnClickListener(btnMultiSensorListener);
		//

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();

		initSensorListeners();

		if (sensorStack == null) {
			sensorStack = new SensorStack();
		}

		sensorStack.registerSensor(DEVICE_ID); // local sensor
		sensorStack.registerSensor("MULTI_TEST");
		sensorStack.getSensorByID("MULTI_TEST").sleep();

		createQuaternionUDPTasks();
		updateUI();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			getRotationMeter(event);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	/**
	 * 
	 */
	protected void initSensorListeners() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		}
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	/**
	 * Creates an asnyc task which periodically retrieves and then transmits
	 * sensor data via udp packets.
	 */
	protected void createQuaternionUDPTasks() {
		Log.d(TAG, "Creating async background tasks for " + sensorStack.size()
				+ " registered sensors...");

		for (Iterator<SensorData> iter = sensorStack.getSensorData(); iter
				.hasNext();) {

			try {
				SensorData sensorData = iter.next();
				SendQuaternionUDPTask async = new SendQuaternionUDPTask(
						sensorData, FRAME_RATE, UDP_DEST, UDP_PORT);
				asyncStack.add(async);

				TaskHelper.execute(async);
			} catch (UnknownHostException e) {
				Log.e(TAG, "Given host is unknown!", e);
			}
		}
	}

	
	/**
	 * Manages common UI updates.
	 */
	private void updateUI() {

		String sensorInfo = "";
		int activeSensors = 0;
		int registeredSensors = sensorStack.size();
		for (Iterator<SensorData> iter = sensorStack.getSensorData(); iter
				.hasNext();) {
			if (iter.next().isAlive()) {
				activeSensors++;
			}
		}
		sensorInfo = "Registered sensors: " + registeredSensors;
		sensorInfo += "\nActive sensors: " + Math.max(0, (asyncStack.size() - (registeredSensors - activeSensors)));
		txtSensorInfo.setText(sensorInfo);

	}

	/**
	 * Interrupts all current running async tasks.
	 */
	protected void cancelQuaternionUDPTasks() {
		Log.d(TAG, "Canceling all " + asyncStack.size()
				+ " async background tasks...");

		for (Iterator<SendQuaternionUDPTask> iter = this.asyncStack.iterator(); iter
				.hasNext();) {
			SendQuaternionUDPTask async = iter.next();
			Log.d(TAG, "Status async task: " + async.getStatus());
			async.cancel(false);
		}

		// clear the set
		asyncStack.clear();
	}

	/**
	 * Start and stop sending udp packets including sensor data. It will cancel
	 * and restart the async udp task.
	 */
	private View.OnClickListener btnSendUDPListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (isSending) {

				cancelQuaternionUDPTasks();

				button.setBackgroundColor(Color.GREEN);
				button.setText("send");
				isSending = false;
			} else {

				createQuaternionUDPTasks();

				button.setBackgroundColor(Color.RED);
				button.setText("stop");
				isSending = true;
			}
			
			updateUI();
		}
	};

	/**
	 * TODO Temporary test function for sending multiple sensor data
	 */
	private View.OnClickListener btnMultiSensorListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			SensorData sensorData = sensorStack.getSensorByID("MULTI_TEST");
			if (sensorData.isAlive()) {
				buttonTestMulti.setBackgroundColor(Color.RED);
				sensorData.sleep();
			} else {
				buttonTestMulti.setBackgroundColor(Color.GREEN);
				sensorData.wakeUp();
			}

			updateUI();
		}
	};

	/**
	 * Get rotation sensor data and transform them to quaternions.
	 * 
	 * @param event
	 */
	private void getRotationMeter(SensorEvent event) {
		float[] rotation = event.values;
		float[] rv = new float[] { rotation[0], rotation[1], rotation[2] };
		float[] quaternions = new float[4];

		SensorManager.getQuaternionFromVector(quaternions, rv);
		sensorStack.updateSensor(DEVICE_ID, quaternions);
		sensorStack.updateSensor("MULTI_TEST", quaternions);

		this.txtSensorData.setText("Rotation X: " + (int) (rotation[0] * 1000)
				+ "\n" + "Rotation Y: " + ((int) (rotation[1] * 1000)) + "\n"
				+ "Rotation Z: " + ((int) (rotation[2] * 1000)) + "\n"
				+ "\n\n\n" + quaternions[0] + "\n" + quaternions[1] + "\n"
				+ quaternions[2] + "\n" + quaternions[3] + "\n" + "Rotation \n"
				+ "X: " + rotation[0] + "\nY: " + rotation[1] + "\nZ: "
				+ rotation[2]);
	}

}
