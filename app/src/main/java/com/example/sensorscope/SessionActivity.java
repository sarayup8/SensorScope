package com.example.sensorscope;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvSensorName, tvElapsedTime, tvSampleCount, tvMotionFeature, tvCurrentMotion;

    private EditText T1, T2;
    private Button btnApply, btnStopSession;

    private TimePlotView timePlotView;
    private FFTPlotView fftPlotView;
    private MotionAnimationView motionAnimationView;

    private SensorManager sensorManager;
    private Sensor selectedSensor;

    private BufferedWriter writer;

    private int sampleCount = 0;
    private long firstTimestamp = -1;
    private long lastFFTTime = 0;

    private double threshold1, threshold2;

    // Most recent 128 samples
    private static final int N = 128;

    private double[] xBuffer = new double[N];
    private double[] yBuffer = new double[N];
    private double[] zBuffer = new double[N];
    private long[] timeBuffer = new long[N];

    private int bufferCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        tvSensorName = findViewById(R.id.tvSensorName);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvSampleCount = findViewById(R.id.tvSampleCount);
        tvMotionFeature = findViewById(R.id.tvMotionFeature);
        tvCurrentMotion = findViewById(R.id.tvCurrentMotion);

        T1 = findViewById(R.id.T1);
        T2 = findViewById(R.id.T2);

        btnApply = findViewById(R.id.btnApply);
        btnStopSession = findViewById(R.id.btnStopSession);

        timePlotView = findViewById(R.id.timePlotView);
        fftPlotView = findViewById(R.id.fftPlotView);
        motionAnimationView = findViewById(R.id.motionAnimationView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Get sensor selected on Screen 1
        int sensorType = getIntent().getIntExtra("sensorType", Sensor.TYPE_ACCELEROMETER);

        selectedSensor = sensorManager.getDefaultSensor(sensorType);

        // Set required default thresholds
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            tvSensorName.setText("Accelerometer");
            threshold1 = 0.20;
            threshold2 = 0.80;
        } else {
            tvSensorName.setText("Gyroscope");
            threshold1 = 0.05;
            threshold2 = 0.25;
        }

        T1.setText(String.valueOf(threshold1));
        T2.setText(String.valueOf(threshold2));

        createFile(sensorType);

        // Only activate the selected sensor
        sensorManager.registerListener(this, selectedSensor, SensorManager.SENSOR_DELAY_GAME);

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyThresholds();
            }
        });

        btnStopSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSession();
            }
        });

        // Back button works the same as Stop Session
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        stopSession();
                    }
                }
        );
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (firstTimestamp == -1) {
            firstTimestamp = event.timestamp;
        }

        sampleCount++;

        double elapsed = (event.timestamp - firstTimestamp) / 1000000000.0;

        tvElapsedTime.setText(String.format(Locale.US, "Time: %.1f s", elapsed));

        tvSampleCount.setText("Samples: " + sampleCount);

        timePlotView.addSample(event.timestamp, x, y, z);

        saveSample(event.timestamp, x, y, z);
        addToBuffer(event.timestamp, x, y, z);

        // Start analysis once we have 128 samples
        if (bufferCount == N) {
            classifyMotion();

            long currentTime = System.currentTimeMillis();

            if (currentTime - lastFFTTime >= 500) {
                calculateFFT();
                lastFFTTime = currentTime;
            }
        }
    }

    // Keep only the newest 128 samples
    private void addToBuffer(long time, float x, float y, float z) {

        if (bufferCount < N) {
            xBuffer[bufferCount] = x;
            yBuffer[bufferCount] = y;
            zBuffer[bufferCount] = z;
            timeBuffer[bufferCount] = time;
            bufferCount++;
            return;
        }

        for (int i = 1; i < N; i++) {
            xBuffer[i - 1] = xBuffer[i];
            yBuffer[i - 1] = yBuffer[i];
            zBuffer[i - 1] = zBuffer[i];
            timeBuffer[i - 1] = timeBuffer[i];
        }

        xBuffer[N - 1] = x;
        yBuffer[N - 1] = y;
        zBuffer[N - 1] = z;
        timeBuffer[N - 1] = time;
    }

    private void classifyMotion() {

        double stdX = standardDeviation(xBuffer);
        double stdY = standardDeviation(yBuffer);
        double stdZ = standardDeviation(zBuffer);

        double motion = Math.sqrt(stdX * stdX + stdY * stdY + stdZ * stdZ);

        tvMotionFeature.setText(String.format(Locale.US, "Motion Feature M: %.2f", motion)
        );

        String level;

        if (motion < threshold1) {
            level = "LOW";
        } else if (motion < threshold2) {
            level = "MEDIUM";
        } else {
            level = "HIGH";
        }

        tvCurrentMotion.setText("Current Motion: " + level);
        motionAnimationView.setMotionLevel(level);
    }

    private double standardDeviation(double[] values) {

        double sum = 0;

        for (int i = 0; i < N; i++) {
            sum += values[i];
        }

        double mean = sum / N;
        double total = 0;

        for (int i = 0; i < N; i++) {
            double difference = values[i] - mean;
            total += difference * difference;
        }

        return Math.sqrt(total / N);
    }

    private void calculateFFT() {

        double[] xMagnitude = getMagnitude(xBuffer);
        double[] yMagnitude = getMagnitude(yBuffer);
        double[] zMagnitude = getMagnitude(zBuffer);

        // Convert timestamp difference from nanoseconds to seconds
        double timeSeconds = (timeBuffer[N - 1] - timeBuffer[0]) / 1000000000.0;

        // Fs = (N - 1) / (tN-1 - t0)
        double samplingFrequency = (N - 1) / timeSeconds;

        fftPlotView.setFFTData(xMagnitude, yMagnitude, zMagnitude, samplingFrequency);
    }

    private double[] getMagnitude(double[] values) {

        double mean = 0;

        for (int i = 0; i < N; i++) {
            mean += values[i];
        }

        mean /= N;

        double[] samples = new double[N];

        // Remove mean before FFT (average value creates a large DC component at bin 0)
        for (int i = 0; i < N; i++) {
            samples[i] = values[i] - mean;
        }

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        Complex[] result = fft.transform(samples, TransformType.FORWARD);

        double[] magnitude = new double[N / 2];

        for (int k = 0; k < magnitude.length; k++) {
            magnitude[k] = result[k].abs();
        }

        return magnitude;
    }

    private void applyThresholds() {

        try {
            double newT1 = Double.parseDouble(T1.getText().toString());
            double newT2 = Double.parseDouble(T2.getText().toString());

            if (newT1 >= newT2) {
                Toast.makeText(this, "T1 must be less than T2", Toast.LENGTH_SHORT).show();
                return;
            }

            threshold1 = newT1;
            threshold2 = newT2;

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter numeric threshold values", Toast.LENGTH_SHORT).show();
        }
    }

    // Create a new CSV file for this session
    private void createFile(int sensorType) {

        try {
            File folder = getExternalFilesDir("sessions");

            // Make sure the sessions folder exists
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }

            String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

            String prefix;

            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                prefix = "ACCEL_";
            } else {
                prefix = "GYRO_";
            }

            File file = new File(folder, prefix + date + ".csv");

            writer = new BufferedWriter(new FileWriter(file));
            writer.write("timestamp_ns,x,y,z\n");
            writer.flush();
        } catch (Exception e) {
            Toast.makeText(this, "Could not create session file", Toast.LENGTH_SHORT).show();
        }
    }

    // Save raw sensor data only
    private void saveSample(long timestamp, float x, float y, float z) {

        if (writer == null) {
            return;
        }

        try {
            writer.write(timestamp + "," + x + "," + y + "," + z + "\n");
        } catch (Exception e) {
            Toast.makeText(this, "Could not write sensor data", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopSession() {

        sensorManager.unregisterListener(this);
        motionAnimationView.setMotionLevel("LOW");

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {}
        finish();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}