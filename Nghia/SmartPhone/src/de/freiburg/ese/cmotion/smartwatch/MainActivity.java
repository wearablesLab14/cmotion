/*
 * @Slia
 */
package de.freiburg.ese.cmotion.smartwatch;

// import com.example.eyespeedtest.R;

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private boolean color = false;
	// private View view;
	private long lastUpdate;
	private float[] lastValues;
	SendQuaternionUDPTask async;
	Button button;
	TextView text;
	private Object rotation;

	private static float[] lastQQ = new float[4];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);
		// view = findViewById(R.id.textView1);
		// view.setBackgroundColor(Color.GREEN);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_FASTEST);

		lastUpdate = System.currentTimeMillis();

		button = (Button) findViewById(R.id.button1);
		text = (TextView) findViewById(R.id.textView1);

		try {
			async = new SendQuaternionUDPTask(60, "192.168.0.255", 5050);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		async.execute();
		text.setTextSize(20);
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
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			getRotationMeter(event);
		}
	}

	private void getRotationMeter(SensorEvent event) {
		float[] rotation = event.values;

		float[] q = new float[4];
		float[] rv = new float[] { rotation[0], rotation[1], rotation[2] };
		sensorManager.getQuaternionFromVector(lastQQ, rv);
		this.text.setText(
		// "Rotation X: " + (int)(rotationx*1000) + "\n" +
		// "Rotation Y: " + ((int)(rotationy*1000) )+ "\n" +
		// "Rotation Z: " + ((int)(rotationz*1000) )+ "\n" +
		// "\n\n\n"++
				lastQQ[0] + "\n" + lastQQ[1] + "\n" + lastQQ[2] + "\n"
						+ lastQQ[3] + "\n" + "Rotation \n" + "X: "
						+ rotation[0] + "\nY: " + rotation[1] + "\nZ: "
						+ rotation[2]

				);

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		super.onResume();
		// register this class as a listener for the orientation and
		// accelerometer sensors
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	public static float[] getSensorData() {
		float[] copyArray = new float[lastQQ.length];
		System.arraycopy(lastQQ, 0, copyArray, 0, lastQQ.length);
		return copyArray;

	}

}
