package com.example.myapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends WearableActivity implements
        MessageClient.OnMessageReceivedListener, SensorEventListener {
    private final static String TAG = "Wear MainActivity";
    private final static String sensorDataPath = "/sensor_data";
    private final static String streamingControlPath = "/streaming_control";

    private float xAccel = 0, yAccel = 0, zAccel = 0, xGyro = 0, yGyro = 0, zGyro = 0;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;

    private Task<List<Node>> nodeListTask;
    private List<Node> nodes = null;
    private String phoneName = "NO DEVICE CONNECTED";
    private FileOutputStream fos = null;

    private int freq = 50;
    private int intervalTime = 1000 / freq;

    private Button mButtonRecord;
    private Button mButtonStream;
    private EditText mFreq;
    private TextView mStatus;

    private Boolean isRecording = false;
    private Boolean isStreaming = false;
    private Thread startRecordingThread = null;
    private Thread startStreamingThread = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mStatus = (TextView) findViewById(R.id.status);

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        Wearable.getMessageClient(this).addListener(this);
        this.sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        this.sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);

        this.nodeListTask = Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
//        Get connected devices
        new ListConnectedDevice().start();

        this.mButtonRecord = (Button) findViewById(R.id.record);
        this.mButtonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonRecord.getText() == "RECORDING") {
                    if (fos != null) {
                        try {
                            isRecording = false;
                            long startTime = System.currentTimeMillis();
                            while (System.currentTimeMillis() - startTime < 1000) {
                                mStatus.setText("Write done in " + (System.currentTimeMillis() - startTime) / 1000.0 + " s");
                                continue;
                            }
                            fos.close();
                            Toast.makeText(getApplicationContext(), (CharSequence) ("File saved in " + getFilesDir()), Toast.LENGTH_SHORT).show();
                            fos = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mFreq.setFocusableInTouchMode(true);
                    mButtonRecord.setText("START RECORD");
                } else {
                    try {
                        fos = openFileOutput("Record_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm").format(new Date()) + ".txt", MODE_PRIVATE);
                        isRecording = true;
                        startRecordingThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                startRecording();
                            }
                        });
                        startRecordingThread.start();
                        mStatus.setText("Recorder has started.");
                        Toast.makeText(getApplicationContext(), (CharSequence) ("Recording sensor data"), Toast.LENGTH_SHORT).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    mFreq.setFocusable(false);
                    mButtonStream.setFocusable(false);
                    mButtonRecord.setText("RECORDING");
                }
            }
        });

        this.mButtonStream = (Button) findViewById(R.id.stream);
        this.mButtonStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonStream.getText() == "STREAMING") {
                    isStreaming = false;
                    mFreq.setFocusableInTouchMode(true);
                    mButtonStream.setText("START STREAM");
                    mStatus.setText("--x--> " + phoneName);
                    Wearable.getMessageClient(MainActivity.this).sendMessage(nodes.get(0).getId(), streamingControlPath, "STOP_STREAMING".getBytes());
//                    Toast.makeText(getApplicationContext(), (CharSequence) ("Stop streaming sensor data to mobile"), Toast.LENGTH_SHORT).show();
                } else {
                    mStatus.setText("----> " + phoneName);
                    isStreaming = true;
                    startStreamingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startStreaming();
                        }
                    });
                    startStreamingThread.start();

                    Wearable.getMessageClient(MainActivity.this).sendMessage(nodes.get(0).getId(), streamingControlPath, "START_STREAMING".getBytes());

                    mFreq.setFocusable(false);
                    mButtonRecord.setFocusable(false);
                    mButtonStream.setText("STREAMING");
//                    Toast.makeText(getApplicationContext(), (CharSequence) ("Streaming sensor data to mobile"), Toast.LENGTH_SHORT).show();
                }
            }
        });

        this.mFreq = (EditText) findViewById(R.id.freqValue);
        this.mFreq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals("") || !editable.toString().matches("[0-9]+")) {
                    Toast.makeText(getApplicationContext(), (CharSequence) ("Frequency must be a number. Reset freq into 50 as default."), Toast.LENGTH_SHORT).show();
                    freq = 50;
                    intervalTime = 1000 / freq;
                    return;
                }
                try {
                    freq = Integer.parseInt(editable.toString());
                    if (freq != 0) {
                        intervalTime = 1000 / freq;
                    } else {
                        freq = 50;
                        intervalTime = 1000 / freq;
                    }
                } catch (NumberFormatException e) {
                    freq = 50;
                    intervalTime = 1000 / freq;
                }
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }

    void waitForNextData(int intervalTime) {
        long now = System.currentTimeMillis();
        while (System.currentTimeMillis() - now < intervalTime) {
            continue;
        }
    }

    void startRecording() {
        while (this.isRecording) {
            String data = System.currentTimeMillis() + "," + this.xAccel + "," + this.yAccel + "," + this.zAccel + "," + this.xGyro + "," + this.yGyro + "," + this.zGyro + "\n";
            try {
                this.fos.write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            waitForNextData(this.intervalTime);
        }
    }

    //    My XZ Premium id: a18c294e
    void startStreaming() {
        while (this.isStreaming) {
            String data = System.currentTimeMillis() + "," + this.xAccel + "," + this.yAccel + "," + this.zAccel + "," + this.xGyro + "," + this.yGyro + "," + this.zGyro;
            Wearable.getMessageClient(MainActivity.this).sendMessage(nodes.get(0).getId(), this.sensorDataPath, data.getBytes());
            this.waitForNextData(this.intervalTime);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        this.sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.sensorManager.unregisterListener(this);
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
//        Log.d(TAG, "onMessageReceived() A message from watch was received:"
//                + messageEvent.getRequestId() + " " + messageEvent.getPath());
//        if (messageEvent.getPath().equals("/sensor_data")) {  //don't think this if is necessary anymore.
//            String message = new String(messageEvent.getData());
//            Log.v(TAG, "Wear activity received message: " + message);
//        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.xAccel = sensorEvent.values[0];
            this.yAccel = sensorEvent.values[1];
            this.zAccel = sensorEvent.values[2];
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            this.xGyro = sensorEvent.values[0];
            this.yGyro = sensorEvent.values[1];
            this.zGyro = sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    class ListConnectedDevice extends Thread {
        public void run() {
            try {
                nodes = Tasks.await(nodeListTask);
                if (nodes.size() > 0) {
                    phoneName = nodes.get(0).toString().substring(5).split(",")[0];
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
