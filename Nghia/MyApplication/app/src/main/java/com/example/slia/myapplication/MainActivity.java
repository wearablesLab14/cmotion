package com.example.slia.myapplication;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

import java.util.Date;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity implements SensorEventListener{

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;

    /**
     * "Hello World!" {@link View} generated by {@link #buildView()}.
     */
    private View mView;
    CardBuilder card ;
    /*
    start sending paket
     */

    private SendQuaternionUDPTask async;
    private Button button;
    // private TextView text;
    private SensorManager sensorManager;
    private boolean isSending = true;
    private static float[] lastQQ = new float[4];

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mView = buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });


        setContentView(mCardScroller);
         // card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        // start sending and never end it
        initSensorListeners();
        createQuaternionUDPTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView() {
      card = new CardBuilder(this, CardBuilder.Layout.TEXT);

        card.setText(R.string.hello_world + new Date().toString());
        return card.getView();
    }


    /* start sending paket*/
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
            // stop this first
            // Log.v("someTag","bullshit");
            getRotationMeter(event);
            //  return card.getView();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    // help Methods
    private void getRotationMeter(SensorEvent event) {
        try {
            float[] rotation = event.values;
            float[] rv = new float[]{rotation[0], rotation[1], rotation[2]};
            SensorManager.getQuaternionFromVector(lastQQ, rv);
            /* other outputs
            this.text.setText(
                    lastQQ[0] + "\n" + lastQQ[1] + "\n" + lastQQ[2] + "\n"
                            + lastQQ[3] + "\n" + "Rotation \n" + "X: "
                            + rotation[0] + "\nY: " + rotation[1] + "\nZ: "
                            + rotation[2]

            );
            */
            Log.d("Coords", lastQQ[0] + "\n" + lastQQ[1] + "\n" + lastQQ[2] + "\n"
                    + lastQQ[3] + "\n" + "Rotation \n" + "X: "
                    + rotation[0] + "\nY: " + rotation[1] + "\nZ: "
                    + rotation[2]);
        } catch (Exception ex)
        {
           Log.d("Error", ex.toString());
        }
    }
    // Init
    protected void initSensorListeners() {
        try {
            if (sensorManager == null) {
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            }
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    SensorManager.SENSOR_DELAY_FASTEST);
        } catch (Exception ex)
        {
            card.setText(ex.toString());
        }

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