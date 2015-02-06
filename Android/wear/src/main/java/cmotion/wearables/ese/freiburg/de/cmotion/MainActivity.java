package cmotion.wearables.ese.freiburg.de.cmotion;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MainActivity for the wearable app of cmotion project. The app will transmit its local device sensor data
 * via message api to its paired mobile device. The data will be transmitted through bluetooth le.
 *
 * @author Sebastian Jäger<jaegerse@informatik.uni-freiburg.de>
 * @date 05.02.2015
 * @version 0.0.1
 */
public class MainActivity extends Activity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnLongClickListener {

    private static final long CONNECTION_TIME_OUT_MS = 100;
    public static final String TAG = "CMOTION";
    private TextView mTextView;
    private SensorManager sensorManager;
    private static float[] lastQQ = new float[4];
    protected GoogleApiClient mGoogleApiClient;
    private DismissOverlayView mDismissOverlay;
    private GestureDetector mDetector;
    private String nodeId;
    private long lastTime = System.currentTimeMillis();
    private int FRAMES = 70; // messages per second send to mobile


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");

        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

/*
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
*/

        // Obtain the DismissOverlayView element
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.long_press_intro);
        mDismissOverlay.showIntroIfNecessary();

        // Configure a gesture detector
        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent ev) {
                mDismissOverlay.show();
            }
        });


        initGoogleApi();
    }


    @Override
    protected void onResume() {
        super.onResume();
        initSensorListeners();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() called");

        if (mGoogleApiClient != null) {
            //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() called");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }

    // Capture long presses
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
    }


    /**
     * Initializes the GoogleApiClient and gets the Node ID of the connected device.
     */
    private void initGoogleApi() {
        mGoogleApiClient = getGoogleApiClient(this);
        retrieveDeviceNode();
    }

    /**
     * Returns a GoogleApiClient that can access the Wear API.
     *
     * @param context
     * @return A GoogleApiClient that can make calls to the Wear API
     */
    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }


    /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                Log.d(TAG, "Connected nodes: " + nodes.size());
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                    Log.d(TAG, "Node ID: " + nodeId);
                }
                mGoogleApiClient.disconnect();
            }
        }).start();
    }


    /**
     * Sends a message to the connected mobile phone. The payload contains the quaternions as float array.
     *
     */
    private void sendQuaternionsToMobilePhone() {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Log.v(TAG, "Message sent...");
                    PendingResult<MessageApi.SendMessageResult> pend = Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "Quaternions", floatArray2ByteArray(lastQQ));
                    pend.setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                    Status status = sendMessageResult.getStatus();
                                    Log.v(TAG, "" + sendMessageResult.getStatus());
                                }
                            }
                    );
                    mGoogleApiClient.disconnect();
                }
            }).start();
        }
    }

    /**
     *
     */
    protected void initSensorListeners() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            getRotationMeter(event);
        }

        if (System.currentTimeMillis() - lastTime >= FRAMES) {
            sendQuaternionsToMobilePhone();
            lastTime = System.currentTimeMillis();
        }

        //Wearable.MessageApi.sendMessage(mGoogleApiClient, "some ID", "path", FloatArray2ByteArray(lastQQ));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // currently not needed...
    }

    /**
     * @param event
     */
    private void getRotationMeter(SensorEvent event) {
        float[] rotation = event.values;
        float[] q = new float[4];
        float[] rv = new float[]{rotation[0], rotation[1], rotation[2]};
        SensorManager.getQuaternionFromVector(lastQQ, rv);

        if (mTextView == null) return;
        this.mTextView.setText("Rotation X: " + (int) (rotation[0] * 1000)
                        + "\n" + "Rotation Y: " + ((int) (rotation[1] * 1000)) + "\n"
                        + "Rotation Z: " + ((int) (rotation[2] * 1000)) + "\n"
                        + lastQQ[0] + "\n" + lastQQ[1] + "\n" + lastQQ[2]
                        + "\n" + lastQQ[3] + "\n"
        );
    }

    /**
     * @param values
     * @return
     */
    public static byte[] floatArray2ByteArray(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

        for (float value : values) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }
}

