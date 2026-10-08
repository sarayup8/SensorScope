package com.example.sensorscope;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RadioGroup sensorGroup;

    private RadioButton radioAccelerometer;
    private RadioButton radioGyroscope;

    private TextView tvAvailability;
    private TextView tvMaxRange;
    private TextView tvResolution;
    private TextView tvMinDelay;

    private Button btnStartSession;

    private SensorManager sensorManager;

    private Sensor accelerometer;
    private Sensor gyroscope;

    private Sensor selectedSensor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorGroup = findViewById(R.id.sensorGroup);

        radioAccelerometer = findViewById(R.id.radioAccelerometer);
        radioGyroscope = findViewById(R.id.radioGyroscope);

        tvAvailability = findViewById(R.id.tvAvailability);
        tvMaxRange = findViewById(R.id.tvMaxRange);
        tvResolution = findViewById(R.id.tvResolution);
        tvMinDelay = findViewById(R.id.tvMinDelay);

        btnStartSession = findViewById(R.id.btnStartSession);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        btnStartSession.setEnabled(false);

        sensorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioAccelerometer) {
                    selectedSensor = accelerometer;
                    showSensorInformation(selectedSensor);
                } else if (checkedId == R.id.radioGyroscope) {
                    selectedSensor = gyroscope;
                    showSensorInformation(selectedSensor);
                }
            }
        });

        btnStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSession();
            }
        });
    }

    private void showSensorInformation(Sensor sensor) {
        if (sensor == null) {
            tvAvailability.setText("Status: Not Available");
            tvMaxRange.setText("Max Range: --");
            tvResolution.setText("Resolution: --");
            tvMinDelay.setText("Min Delay: --");
            btnStartSession.setEnabled(false);
        } else {
            tvAvailability.setText("Status: Available");
            tvMaxRange.setText(String.format(Locale.US, "Max Range: %.2f", sensor.getMaximumRange()));
            tvResolution.setText(String.format(Locale.US, "Resolution: %.4f", sensor.getResolution()));
            tvMinDelay.setText("Min Delay: " + sensor.getMinDelay() + " us");
            btnStartSession.setEnabled(true);
        }
    }

    private void startSession() {
        Intent intent = new Intent(MainActivity.this, SessionActivity.class);

        // Pass the selected sensor to Screen 2
        if (selectedSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            intent.putExtra("sensorType", Sensor.TYPE_ACCELEROMETER);
        } else {
            intent.putExtra("sensorType", Sensor.TYPE_GYROSCOPE);
        }
        startActivity(intent);
    }
}