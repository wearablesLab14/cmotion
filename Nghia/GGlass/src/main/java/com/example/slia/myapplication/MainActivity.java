package com.example.slia.myapplication;


import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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


public class MainActivity extends Activity implements SensorEventListener{

    private SendQuaternionUDPTask async;
    private Button button;
    private TextView text;
    private SensorManager sensorManager;
    private boolean isSending = true;
    private static float[] lastQQ = new float[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start sending and never end it
        initSensorListeners();
        createQuaternionUDPTask();
    }


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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static float[] getSensorData() {
        float[] copyArray = new float[lastQQ.length];
        System.arraycopy(lastQQ, 0, copyArray, 0, lastQQ.length);
        return copyArray;

    }
    // implements API
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            getRotationMeter(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
    private void getRotationMeter(SensorEvent event) {
        float[] rotation = event.values;
        float[] rv = new float[] { rotation[0], rotation[1], rotation[2] };
        SensorManager.getQuaternionFromVector(lastQQ, rv);
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

    protected void createQuaternionUDPTask() {
        try {
            async = new SendQuaternionUDPTask(60, "192.168.0.255", 5050);
        } catch (Exception e) {
            Log.e("", "Given host is unknown.", e);
            // e.printStackTrace();
        }
        async.execute();
    }

}
