package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.JsonObject;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {
    private final static String TAG = "Mobile MainActivity";
    private final static String sensorDataPath = "/sensor_data";
    private final static String streamingControlPath = "/streaming_control";

    private Boolean isStreaming = false;

    private LineChart mAccelChart = null;
    private LineChart mGyroChartChart = null;
    private Thread visualizeThread = null;
    private float xAccel = 0, yAccel = 0, zAccel = 0, xGyro = 0, yGyro = 0, zGyro = 0;


    private OpenHabApi mOpenHabApi;
    private String previousGesture = "";

    private final int FREQUENCY = 50;
    private List<Float> listAcceX = new ArrayList<>();
    private List<Float> listAcceY = new ArrayList<>();
    private List<Float> listAcceZ = new ArrayList<>();
    private List<Float> listGyroX = new ArrayList<>();
    private List<Float> listGyroY = new ArrayList<>();
    private List<Float> listGyroZ = new ArrayList<>();
    private List<Float> input_signal = new ArrayList<>();

    private TextView mLabel = null;
    private TextView mAction = null;
    private Interpreter tflite;

    private String[] colors = {"230,255,255", "0,0,153", "153,0,255"};
    private int current_color_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        // 192.168.1.6 is local server address
        mOpenHabApi = new Retrofit.Builder()
                .baseUrl("http://192.168.1.6:8080/rest/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(OpenHabApi.class);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        this.mLabel = findViewById(R.id.label);
        this.mAction = findViewById(R.id.action);

        this.mAccelChart = findViewById(R.id.lineChartAccel);
        this.mGyroChartChart = findViewById(R.id.lineChartGyro);
        this.acceGraphInit(this.mAccelChart);
        this.acceGraphInit(this.mGyroChartChart);
    }

    private void sendsACommandToYeelight(String bodyText) {
        RequestBody body =
                RequestBody.create(MediaType.parse("text/plain"), bodyText);
        //'yeelight_wonder_0x0000000007e3c802_color' is device's ID
        mOpenHabApi.sendsACommandToAnItem("yeelight_wonder_0x0000000007e3c802_color", body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
//                Toast.makeText(getActivity(), response.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
//                Toast.makeText(getActivity(), t.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void controlYeelightState(final int actionType) {
        mOpenHabApi.getASingleItem("yeelight_wonder_0x0000000007e3c802_color")
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        JsonObject stateResponse = response.body();
                        String state = stateResponse.get("state").getAsString();
                        switch (actionType) {
                            case 0: {
                                String[] splitted = state.split(",", 3);
                                int b = Integer.parseInt(splitted[2]);
                                if (b == 0) {
                                    sendsACommandToYeelight("ON");
                                    mAction.setText("Turn on");
                                } else {
                                    sendsACommandToYeelight("OFF");
                                    mAction.setText("Turn off");
                                }
                                break;
                            }
                            case 1: {
                                current_color_index += 1;
                                if (current_color_index >= colors.length) {
                                    current_color_index = colors.length - 1;
                                }
                                sendsACommandToYeelight(colors[current_color_index]);
                                mAction.setText("Next color");
                                break;
                            }
                            case 2: {
                                current_color_index -= 1;
                                if (current_color_index < 0) {
                                    current_color_index = 0;
                                }
                                sendsACommandToYeelight(colors[current_color_index]);
                                mAction.setText("Previous color");
                                break;
                            }
                            case 3: {
                                String[] splitted = state.split(",", 3);
                                int res = Integer.parseInt(splitted[2]);
                                if (res < 100) {
                                    res += 50;
                                    if (res > 100)
                                        res = 100;
                                }
                                sendsACommandToYeelight(String.valueOf(res));
                                mAction.setText("Brighter");
                                break;
                            }

                            case 4: {
                                String[] splitted = state.split(",", 3);
                                int res = Integer.parseInt(splitted[2]);
                                if (res > 10) {
                                    res -= 30;
                                    if (res < 10) {
                                        res = 10;
                                    }
                                }
                                sendsACommandToYeelight(String.valueOf(res));
                                mAction.setText("Darker");
                                break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.visualizeThread.interrupt();
        this.visualizeThread = null;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        String message = new String(messageEvent.getData());
        if (path.equals(this.sensorDataPath)) {
            String[] values = message.split(",");
            try {
                this.xAccel = Float.parseFloat(values[1]);
                this.yAccel = Float.parseFloat(values[2]);
                this.zAccel = Float.parseFloat(values[3]);
                this.xGyro = Float.parseFloat(values[4]);
                this.yGyro = Float.parseFloat(values[5]);
                this.zGyro = Float.parseFloat(values[6]);

                this.listAcceX.add(this.xAccel);
                this.listAcceY.add(this.yAccel);
                this.listAcceZ.add(this.zAccel);
                this.listGyroX.add(this.xGyro);
                this.listGyroY.add(this.yGyro);
                this.listGyroZ.add(this.zGyro);
                doPrediction();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else if (path.equals(this.streamingControlPath)) {
            if (message.equals("STOP_STREAMING")) {
                this.isStreaming = false;
                Log.i(TAG, "STOP streaming");
            } else if (message.equals("START_STREAMING")) {
                this.isStreaming = true;
                Log.i(TAG, "START streaming");
                this.feedMultiple();
            } else {
                Log.e(TAG, "Unknown command from " + streamingControlPath);
            }
        }

    }

    private void acceGraphInit(LineChart lineChart) {
        lineChart.setDrawGridBackground(true);
        lineChart.setDrawBorders(true);

        // enable description text
        lineChart.getDescription().setEnabled(true);
        lineChart.setContentDescription("Initial Description");

        // set an alternative background color
        lineChart.setBackgroundColor(Color.TRANSPARENT);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);


        // add empty data
        lineChart.setData(data);
        lineChart.getDescription().setEnabled(false);

        // get the legend (only possible after setting data)
        Legend l = lineChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLUE);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        XAxis xl = lineChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(false);
        xl.setCenterAxisLabels(true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLUE);
        leftAxis.setAxisMaximum(40f);
        leftAxis.setAxisMinimum(-40f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void feedMultiple() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                addEntry();
            }
        };

        this.visualizeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isStreaming) {
                    runOnUiThread(runnable);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        visualizeThread.start();
    }

    private void addEntry() {
        LineData accelData = this.mAccelChart.getData();
        if (accelData != null) {
            ILineDataSet setX = accelData.getDataSetByIndex(0);
            ILineDataSet setY = accelData.getDataSetByIndex(1);
            ILineDataSet setZ = accelData.getDataSetByIndex(2);

            if (setX == null || setY == null || setZ == null) {
                setX = createSet("Accel X", Color.parseColor("#2ecc71"));
                setY = createSet("Accel Y", Color.parseColor("#e74c3c"));
                setZ = createSet("Accel Z", Color.parseColor("#2c3e50"));

                accelData.addDataSet(setX);
                accelData.addDataSet(setY);
                accelData.addDataSet(setZ);
            }

            accelData.addEntry(new Entry(setX.getEntryCount(), xAccel), 0);
            accelData.addEntry(new Entry(setY.getEntryCount(), yAccel), 1);
            accelData.addEntry(new Entry(setZ.getEntryCount(), zAccel), 2);

            accelData.notifyDataChanged();

            this.mAccelChart.notifyDataSetChanged();
            this.mAccelChart.setVisibleXRangeMaximum(120);
            this.mAccelChart.moveViewToX(accelData.getEntryCount());
        }

        LineData gyroData = this.mGyroChartChart.getData();
        if (gyroData != null) {
            ILineDataSet setX = gyroData.getDataSetByIndex(0);
            ILineDataSet setY = gyroData.getDataSetByIndex(1);
            ILineDataSet setZ = gyroData.getDataSetByIndex(2);

            if (setX == null || setY == null || setZ == null) {
                setX = createSet("Gyro X", Color.parseColor("#0984e3"));
                setY = createSet("Gyro Y", Color.parseColor("#55E6C1"));
                setZ = createSet("Gyro Z", Color.parseColor("#ff5252"));

                gyroData.addDataSet(setX);
                gyroData.addDataSet(setY);
                gyroData.addDataSet(setZ);
            }

            gyroData.addEntry(new Entry(setX.getEntryCount(), xGyro), 0);
            gyroData.addEntry(new Entry(setY.getEntryCount(), yGyro), 1);
            gyroData.addEntry(new Entry(setZ.getEntryCount(), zGyro), 2);

            gyroData.notifyDataChanged();

            this.mGyroChartChart.notifyDataSetChanged();
            this.mGyroChartChart.setVisibleXRangeMaximum(120);
            this.mGyroChartChart.moveViewToX(gyroData.getEntryCount());
        }
    }

    private LineDataSet createSet(String labelName, int colorValue) {
        LineDataSet set = new LineDataSet(null, labelName);
        set.setColor(colorValue);
        set.setDrawCircles(false);
        set.setDrawValues(true);
        set.setLineWidth(1.3f);
        return set;
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private FloatBuffer toFloatTensor(float[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    private void doPrediction() {
        if (listAcceX.size() == FREQUENCY && listAcceY.size() == FREQUENCY && listAcceZ.size() == FREQUENCY &&
                listGyroX.size() == FREQUENCY && listGyroY.size() == FREQUENCY &&
                listGyroZ.size() == FREQUENCY) {
            for (int i = 0; i < listAcceX.size(); ++i) {
                input_signal.add(listAcceX.get(i));
                input_signal.add(listAcceY.get(i));
                input_signal.add(listAcceZ.get(i));
                input_signal.add(listGyroX.get(i));
                input_signal.add(listGyroY.get(i));
                input_signal.add(listGyroZ.get(i));
            }
            final float[][] results = new float[1][24];
            FloatBuffer tf_input = this.toFloatTensor(this.toFloatArray(input_signal));
            tflite.run(tf_input, results);
            final String predictValue = getPredictValue(results[0]);
            postPredict(predictValue);
            listAcceX.clear();
            listAcceY.clear();
            listAcceZ.clear();
            listGyroX.clear();
            listGyroY.clear();
            listGyroZ.clear();
            input_signal.clear();
        }
    }

    private void postPredict(final String predictValue) {
        if (predictValue.equals("Unknown")) {
            mLabel.setText(predictValue);
            mAction.setText("");
            previousGesture = predictValue;
        } else if (predictValue.equals("Start_gesture")) {
            previousGesture = predictValue;
        } else if ((previousGesture.equals("Start_move_left") && !predictValue.equals("Move_left")) || (previousGesture.equals("Start_move_right") && !predictValue.equals("Move_right"))
                || (previousGesture.equals("Start_move_down") && !predictValue.equals("Move_down")) || (previousGesture.equals("Start_move_up") && !predictValue.equals("Move_up"))) {
            mLabel.setText("Unknown");
            mAction.setText("");
            previousGesture = "Unknown";
        } else if (predictValue.equals("Start_move_left")) {
            previousGesture = predictValue;
        } else if (predictValue.equals("Start_move_right")) {
            previousGesture = predictValue;
        } else if (predictValue.equals("Start_move_up")) {
            previousGesture = predictValue;
        } else if (predictValue.equals("Start_move_down")) {
            previousGesture = predictValue;
        } else if (predictValue.equals("Move_down")) {
            if (previousGesture.equals("Start_move_down")) {
                controlYeelightState(4);
                mLabel.setText("Move down");
            } else {
                mLabel.setText("Unknown");
                previousGesture = "Unknown";
            }
        } else if (predictValue.equals("Move_up")) {
            if (previousGesture.equals("Start_move_up")) {
                controlYeelightState(3);
                mLabel.setText("Move up");
            } else {
                mLabel.setText("Unknown");
                mAction.setText("");
                previousGesture = "Unknown";
            }
        } else if (predictValue.equals("Move_left")) {
            if (previousGesture.equals("Start_move_left")) {
                controlYeelightState(2);
                mLabel.setText("Move left");
            } else {
                mLabel.setText("Unknown");
                mAction.setText("");
                previousGesture = "Unknown";
            }
        } else if (predictValue.equals("Move_right")) {
            if (previousGesture.equals("Start_move_right")) {
                controlYeelightState(1);
                mLabel.setText("Move right");
            } else {
                mLabel.setText("Unknown");
                mAction.setText("");
                previousGesture = "Unknown";
            }
        } else if (predictValue.equals("Select")) {
            mLabel.setText(predictValue);
            previousGesture = predictValue;
            controlYeelightState(0);
        } else if (predictValue.equals("0") || predictValue.equals("1") || predictValue.equals("2") || predictValue.equals("3") || predictValue.equals("4") || predictValue.equals("5")
                || predictValue.equals("6") || predictValue.equals("7") || predictValue.equals("8") || predictValue.equals("9") || predictValue.equals("CCWCircle") || predictValue.equals("CWCircle")
                || predictValue.equals("Clap")) {
            mLabel.setText("Unknown");
            mAction.setText("");
            previousGesture = predictValue;
        }
    }

    private String getPredictValue(float results[]) {
        float max_prob = results[0];
        int most_label_index = 0;
        for (int i = 0; i < results.length; i++) {
            // Skip start gesture and unknown action
            if (i == 18 || i == 23)
                continue;
            if (max_prob < results[i]) {
                max_prob = results[i];
                most_label_index = i;
            }
        }
        if (most_label_index <= 9) {
            return String.valueOf(most_label_index);
        } else if (most_label_index == 10) {
            return "CCWCircle";
        } else if (most_label_index == 11) {
            return "CWCircle";
        } else if (most_label_index == 12) {
            return "Clap";
        } else if (most_label_index == 13) {
            return "Move_down";
        } else if (most_label_index == 14) {
            return "Move_left";
        } else if (most_label_index == 15) {
            return "Move_right";
        } else if (most_label_index == 16) {
            return "Move_up";
        } else if (most_label_index == 17) {
            return "Select";
        } else if (most_label_index == 18) {
            return "Start_gesture";
        } else if (most_label_index == 19) {
            return "Start_move_down";
        } else if (most_label_index == 20) {
            return "Start_move_left";
        } else if (most_label_index == 21) {
            return "Start_move_right";
        } else if (most_label_index == 22) {
            return "Start_move_up";
        } else {
            return "Unknown";
        }
    }

    // Memory-map the model file in Assets.
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
