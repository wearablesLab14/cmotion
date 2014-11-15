package de.freiburg.ese.cmotion.smartwatch;

// import com.example.eyespeedtest.R;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Activity;
import android.app.LauncherActivity.IconResizer;
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
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private boolean color = false;
	// private View view;
	private long lastUpdate;
	private  float [] lastValues ;
	
	Button button;
	TextView text;
	private float[] lastRotation;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);
		//view = findViewById(R.id.textView1);
		//view.setBackgroundColor(Color.GREEN);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_NORMAL);
		
		lastUpdate = System.currentTimeMillis();
		
		
		button = (Button) findViewById(R.id.button1);
		text = (TextView) findViewById(R.id.textView1);
		
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
	
	private void getAccelerometer(SensorEvent event) {
		float[] values = event.values;
		if(this.lastRotation == null || this.lastRotation.length != 5)
			return;
		
		// beschleunigung
		float x = values[0];
		float y = values[1];
		float z = values[2];	
		
		
		// rotation berechnen
	
		float rotationx = this.lastRotation[0];
		float 	rotationy= this.lastRotation[1];
		float 	rotationz= this.lastRotation[2];
		float[] rotation = new float[]{rotationx,rotationy,rotationz} ;

			float w = (float)Math.sqrt(1
					- rotation[0]*rotation[0] - rotation[1]*rotation[1] - rotation[2]*rotation[2]);
			//In this case, the w component of the quaternion is known to be a positive number

			float [] q  = new float[4];
			float [] rv = new float[]{w, rotationx,rotationy,rotationz}; 
		
		sensorManager.getQuaternionFromVector(q, rv);
		
		float[] qq = q;
		this.text.setText("Rotation X: " + (int)(rotationx*1000)  + "\n" + 
				"Rotation Y: " + ((int)(rotationy*1000) )+ "\n" + 
				"Rotation Z: " + ((int)(rotationz*1000) )+ "\n" + 
				"\n\n\n"+
				qq[0] + "\n" +qq[1] + "\n" + qq[2] + "\n" +qq[3] + "\n");
		
		// float accelationSquareRoot = (x * x + y * y + z * z)
				// / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
		//long actualTime = System.currentTimeMillis();
		/*
		if (accelationSquareRoot >= 2) //
		{
			if (actualTime - lastUpdate < 200) {
				return;
			}
			lastUpdate = actualTime;
			Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT)
					.show();
			if (color) {
				view.setBackgroundColor(Color.GREEN);

			} else {
				view.setBackgroundColor(Color.RED);
			}
			color = !color;
		}
		*/
		this.lastValues = values;
	}
	
	private static void sendingInformation(float[] infos) throws IOException
	{
	    String text;
	    int server_port = 12345;
	    byte[] message = new byte[32];
	    DatagramPacket p = new DatagramPacket(message, message.length);
	    
	    DatagramSocket s = new DatagramSocket(server_port);
	    s.receive(p);
	    text = new String(message, 0, p.getLength());
	    Log.d("Udp tutorial","message:" + text);
	    s.close();

		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			getAccelerometer(event);
		}	else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
		{
				getRotationMeter(event);
		}
	}
	
	

	private void getRotationMeter(SensorEvent event) {
		this.lastRotation =   event.values;
		
		
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
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	
	
}
