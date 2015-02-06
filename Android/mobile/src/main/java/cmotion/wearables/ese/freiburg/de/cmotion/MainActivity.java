package cmotion.wearables.ese.freiburg.de.cmotion;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import cmotion.wearables.ese.freiburg.de.cmotion.SensorStack.SensorData;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

/**
 * MainActivity for the mobile app of cmotion project. The app will transmit its local device sensor data as well as
 * connected wearables sensor data via udp packets to the cmotion server which will listen to the broadcast
 * network address. Every sensor - local and connected ones - will be registered in the sensor stack and for each
 * registered sensor an async task will be started in the background which will handle the udp packeting and
 * transmitting. Paired wearable devices will send via the message api which this app is listening to.
 *
 * @author Sebastian Jäger<jaegerse@informatik.uni-freiburg.de>
 * @version 0.0.1
 * @date 05.02.2015
 * @see cmotion.wearables.ese.freiburg.de.cmotion.SensorStack
 * @see cmotion.wearables.ese.freiburg.de.cmotion.SendQuaternionUDPTask
 */
public class MainActivity extends ActionBarActivity implements SensorEventListener, MessageApi.MessageListener, DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = "CMOTION";
    private static final String DEVICE_ID = UUID.randomUUID().toString();
    private static final int UDP_PORT = 5050;
    private static final String UDP_DEST = "192.168.0.255";
    private static final int FRAME_RATE = 60;
    private GoogleApiClient mApiClient;
    private TextView mTextView;

    private Button button;
    private Button buttonDuplicate; // Testfunc for sending duplicate data
    private TextView txtSensorData;
    private TextView txtSensorInfo;
    private boolean isSending = true;
    private SensorManager sensorManager;
    private HashSet<SendQuaternionUDPTask> asyncStack = new HashSet<SendQuaternionUDPTask>();
    private static SensorStack sensorStack = new SensorStack();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        initSensorListeners();
        initGoogleApiClient();

        button = (Button) findViewById(R.id.btn_sending);
        button.setBackgroundColor(Color.RED);
        button.setOnClickListener(btnSendUDPListener);

        buttonDuplicate = (Button) findViewById(R.id.btn_duplicate);
        buttonDuplicate.setBackgroundColor(Color.RED);
        buttonDuplicate.setOnClickListener(btnDuplicateSensorListener);

        txtSensorData = (TextView) findViewById(R.id.textView_sensorData);
        txtSensorData.setTextSize(12);
        txtSensorInfo = (TextView) findViewById(R.id.textView_info);
        txtSensorInfo.setTextSize(12);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorStack == null) {
            sensorStack = new SensorStack();
        }

        sensorStack.registerSensor(DEVICE_ID); // local sensor
        sensorStack.registerSensor("DUPLICATE_SENSOR");
        sensorStack.getSensorByID("DUPLICATE_SENSOR").sleep();

        initSensorListeners();
        initGoogleApiClient();

        createQuaternionUDPTasks();
        updateUI();

        // show client info
        TextView clientInfoView = (TextView) findViewById(R.id.textView_clientInfo);
        clientInfoView.setText("Client info:\nIP-Address: " + getWifiIpAddress());
    }

    @Override
    protected void onStop() {
        if (mApiClient != null) {
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mApiClient != null) {
            mApiClient.unregisterConnectionCallbacks(this);
            mApiClient.unregisterConnectionFailedListener(this);
            Wearable.MessageApi.removeListener(mApiClient, this);
        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        cancelQuaternionUDPTasks();

        super.onDestroy();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            getRotationMeter(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // currently nothing to do...
    }

    @Override
    public void onConnected(Bundle bundle) {
        sendMessage("START_ACTIVITY", "test");
        Log.d(TAG, "onConnected() called");
        TextView textView = (TextView) this.findViewById(R.id.textView_wear);
        textView.setText("Wearable device connected");
        updateUI();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d(TAG, "onConnectionFailed() called");
        TextView textView = (TextView) this.findViewById(R.id.textView_wear);
        textView.setText("Connection to wearable device failed!");
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.d(TAG, "onConnectionSuspended() called");
        TextView textView = (TextView) this.findViewById(R.id.textView_wear);
        textView.setText("No wearable device connected!");
        updateUI();
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

    /**
     *
     */
    private void initGoogleApiClient() {
        if (mApiClient == null) {
            Log.d(TAG, "Init GoogleApiClient");
            mApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            mApiClient.connect();
        } else if (!mApiClient.isConnected()) {
            Log.d(TAG, "Reconnect GoogleApiClient");
            mApiClient.reconnect();
        }

        // Has to be added every time after connect() or reconnect() was called
        Wearable.MessageApi.addListener(mApiClient, this);

        Log.d(TAG, "API Client is " + mApiClient);
        Log.d(TAG, "API Client is connecting " + mApiClient.isConnecting());
        Log.d(TAG, "API Client is connected " + mApiClient.isConnected());
    }


    /**
     * Creates an asnyc task which periodically retrieves and then transmits
     * sensor data via udp packets.
     */
    protected void createQuaternionUDPTasks() {
        cancelQuaternionUDPTasks(); // stop all current async tasks

        Log.d(TAG, "Creating async background tasks for " + sensorStack.size()
                + " registered sensors...");

        for (Iterator<SensorData> iter = sensorStack.getSensorData(); iter
                .hasNext(); ) {

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
        for (Iterator<SensorData> iter = sensorStack.getSensorData(); iter.hasNext(); ) {
            if (iter.next().isAlive()) {
                activeSensors++;
            }
        }
        sensorInfo = "Registered sensors: " + registeredSensors;
        sensorInfo += "\nActive sensors: " + Math.max(0,
                (asyncStack.size() - (registeredSensors - activeSensors)));
        txtSensorInfo.setText(sensorInfo);


    }

    /**
     * Interrupts all current running async tasks.
     */
    protected void cancelQuaternionUDPTasks() {
        Log.d(TAG, "Canceling all " + asyncStack.size()
                + " async background tasks...");

        for (Iterator<SendQuaternionUDPTask> iter = this.asyncStack.iterator(); iter
                .hasNext(); ) {
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
     * Activating the function will duplicate the local sensor data and register it in the sensor
     * stack. Resulting in transmitting the same sensor data twice for parallel or testing purposes.
     */
    private View.OnClickListener btnDuplicateSensorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SensorData sensorData = sensorStack.getSensorByID("DUPLICATE_SENSOR");
            if (sensorData.isAlive()) {
                buttonDuplicate.setBackgroundColor(Color.RED);
                sensorData.sleep();
            } else {
                buttonDuplicate.setBackgroundColor(Color.GREEN);
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
        float[] rv = new float[]{rotation[0], rotation[1], rotation[2]};
        float[] quaternions = new float[4];

        SensorManager.getQuaternionFromVector(quaternions, rv);
        sensorStack.updateSensor(DEVICE_ID, quaternions);
        sensorStack.updateSensor("DUPLICATE_SENSOR", quaternions);

        this.txtSensorData.setText("Rotation X: " + (int) (rotation[0] * 1000)
                + "\n" + "Rotation Y: " + ((int) (rotation[1] * 1000)) + "\n"
                + "Rotation Z: " + ((int) (rotation[2] * 1000))
                + "\n\nQuaternions: \n" + quaternions[0] + "\n" + quaternions[1] + "\n"
                + quaternions[2] + "\n" + quaternions[3] + "\n");

    }

    /**
     * Reads out the device ip address if its connected to a wireless network.
     * Needs the ACCESS_WIFI_STATE permission.
     *
     * @return
     */
    protected String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff),
                (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }


    /**
     * @param path
     * @param text
     */
    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes()).await();
                }
            }
        }).start();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v(TAG, "onMessageReceived() called");
        Log.v(TAG, messageEvent.toString());
        byte[] data = messageEvent.getData();
        float[] quaternions = new float[4];
        String deviceId = messageEvent.getSourceNodeId(); // unique node id of the sender


        // 16 bytes of payload data
        ByteBuffer buffer = ByteBuffer.wrap(data);
        quaternions[0] = buffer.getFloat();
        quaternions[1] = buffer.getFloat();
        quaternions[2] = buffer.getFloat();
        quaternions[3] = buffer.getFloat();


        if (!sensorStack.sensorExists(deviceId)) {
            sensorStack.registerSensor(deviceId);
            createQuaternionUDPTasks();
        }
        sensorStack.updateSensor(deviceId, quaternions);


        if (mTextView == null) return;
        this.mTextView.setText(quaternions[0] + "\n" + quaternions[1] + "\n" + quaternions[2]
                        + "\n" + quaternions[3] + "\n"

        );
        Log.v("WEAR", quaternions[0] + "\n" + quaternions[1] + "\n" + quaternions[2]
                        + "\n" + quaternions[3] + "\n"

        );

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged called");
    }

}

