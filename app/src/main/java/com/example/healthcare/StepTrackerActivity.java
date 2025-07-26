package com.example.healthcare;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepTrackerActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREF_STEPS_GOAL = "steps_goal";
    private static final String PREF_STEPS_KEY = "daily_steps";
    private static final String PREF_INITIAL_STEPS_KEY = "initial_steps";
    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001;

    private TextView currentStepsText;
    private EditText goalInput;
    private ProgressBar stepProgressBar;
    private SharedPreferences sharedPreferences;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private Sensor stepDetectorSensor;
    private int currentSteps = 0;
    private int initialSteps = -1;
    private ToneGenerator toneGenerator;
    private boolean goalAchievementSounded = false;
    private BroadcastReceiver stepUpdateReceiver;
    private BroadcastReceiver goalAchievementReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_tracker);

        sharedPreferences = getSharedPreferences("HealthcareApp", MODE_PRIVATE);
        
        // Initialize views
        currentStepsText = findViewById(R.id.currentStepsText);
        goalInput = findViewById(R.id.goalInput);
        stepProgressBar = findViewById(R.id.stepProgressBar);
        MaterialButton setGoalButton = findViewById(R.id.setGoalButton);
        MaterialButton addStepsManually = findViewById(R.id.addStepsManually);
        
        // Get current steps and goal
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        currentSteps = sharedPreferences.getInt(PREF_STEPS_KEY + today, 0);
        initialSteps = sharedPreferences.getInt(PREF_INITIAL_STEPS_KEY + today, -1);
        int currentGoal = sharedPreferences.getInt(PREF_STEPS_GOAL, 10000);
        
        // Update UI
        updateStepDisplay(currentGoal);
        goalInput.setHint(String.valueOf(currentGoal));
        stepProgressBar.setMax(currentGoal);
        stepProgressBar.setProgress(currentSteps);

        // Initialize step counting
        setupStepCounting();
        
        // Initialize tone generator for goal achievement sound
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        // Register step update receiver
        stepUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("step_counter_update")) {
                    currentSteps = intent.getIntExtra("steps", currentSteps);
                    int currentGoal = sharedPreferences.getInt(PREF_STEPS_GOAL, 10000);
                    updateStepDisplay(currentGoal);
                    stepProgressBar.setProgress(currentSteps);
                    checkGoalAchievement(currentGoal);
                }
            }
        };
        registerReceiver(stepUpdateReceiver, new IntentFilter("step_counter_update"));

        // Register goal achievement receiver
        goalAchievementReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("step_goal_achieved")) {
                    int achievedSteps = intent.getIntExtra("steps", 0);
                    int achievedGoal = intent.getIntExtra("goal", 10000);
                    
                    // Play achievement sound
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    
                    // Show congratulations message
                    Toast.makeText(context, 
                        "Congratulations! You've reached your step goal!", 
                        Toast.LENGTH_LONG).show();
                    
                    // Increase goal by 50 steps
                    int newGoal = achievedGoal + 50;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(PREF_STEPS_GOAL, newGoal);
                    editor.apply();
                    
                    // Update UI with new goal
                    stepProgressBar.setMax(newGoal);
                    updateStepDisplay(newGoal);
                    Toast.makeText(context, 
                        "Goal automatically increased to " + newGoal + " steps!", 
                        Toast.LENGTH_LONG).show();
                }
            }
        };
        registerReceiver(goalAchievementReceiver, new IntentFilter("step_goal_achieved"));

        // Start the step counter service
        startStepCounterService();

        setGoalButton.setOnClickListener(v -> {
            String goalStr = goalInput.getText().toString();
            if (!goalStr.isEmpty()) {
                try {
                    int newGoal = Integer.parseInt(goalStr);
                    if (newGoal > 0) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(PREF_STEPS_GOAL, newGoal);
                        editor.apply();
                        stepProgressBar.setMax(newGoal);
                        updateStepDisplay(newGoal);
                        Toast.makeText(this, "New goal set: " + newGoal + " steps", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        goalAchievementSounded = false;
                    } else {
                        Toast.makeText(this, "Please enter a valid goal (greater than 0)", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addStepsManually.setOnClickListener(v -> {
            EditText manualStepsInput = findViewById(R.id.manualStepsInput);
            String stepsStr = manualStepsInput.getText().toString();
            if (!stepsStr.isEmpty()) {
                try {
                    int additionalSteps = Integer.parseInt(stepsStr);
                    if (additionalSteps > 0) {
                        currentSteps += additionalSteps;
                        saveSteps();
                        updateStepDisplay(currentGoal);
                        stepProgressBar.setProgress(currentSteps);
                        Toast.makeText(this, "Added " + additionalSteps + " steps", Toast.LENGTH_SHORT).show();
                        checkGoalAchievement(currentGoal);
                    } else {
                        Toast.makeText(this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupStepCounting() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        // Try to get step counter sensor first
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        
        // If step counter is not available, try step detector
        if (stepSensor == null) {
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (stepDetectorSensor == null) {
                Toast.makeText(this, "No step sensor found on this device", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Using step detector sensor", Toast.LENGTH_SHORT).show();
            }
        }

        // Request permission for activity recognition
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
        }
    }

    private void saveSteps() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_STEPS_KEY + today, currentSteps);
        editor.apply();
        
        // Update result
        setResult(RESULT_OK, getIntent().putExtra("new_steps", currentSteps));
    }

    private void updateStepDisplay(int goal) {
        currentStepsText.setText(String.format("Current Steps: %d / %d", currentSteps, goal));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];
            String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            
            // If initial steps not set for today, set it
            if (initialSteps == -1) {
                initialSteps = totalSteps - currentSteps; // Preserve any steps from today
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_INITIAL_STEPS_KEY + today, initialSteps);
                editor.apply();
            }
            
            // Calculate today's steps
            int todaySteps = totalSteps - initialSteps;
            if (todaySteps >= 0) {
                currentSteps = todaySteps;
                saveSteps();
                int currentGoal = sharedPreferences.getInt(PREF_STEPS_GOAL, 10000);
                updateStepDisplay(currentGoal);
                stepProgressBar.setProgress(currentSteps);
                checkGoalAchievement(currentGoal);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // Step detector directly reports each step
            currentSteps++;
            saveSteps();
            int currentGoal = sharedPreferences.getInt(PREF_STEPS_GOAL, 10000);
            updateStepDisplay(currentGoal);
            stepProgressBar.setProgress(currentSteps);
            checkGoalAchievement(currentGoal);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Step counting permission granted", Toast.LENGTH_SHORT).show();
                // Restart step counting
                if (stepSensor != null) {
                    sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else if (stepDetectorSensor != null) {
                    sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            } else {
                Toast.makeText(this, "Permission required for step counting", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkGoalAchievement(int goal) {
        if (!goalAchievementSounded && currentSteps >= goal) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            Toast.makeText(this, "Congratulations! You've reached your step goal!", Toast.LENGTH_LONG).show();
            goalAchievementSounded = true;
            
            // Increase goal by 50 steps
            int newGoal = goal + 50;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(PREF_STEPS_GOAL, newGoal);
            editor.apply();
            
            // Update UI with new goal
            stepProgressBar.setMax(newGoal);
            updateStepDisplay(newGoal);
            Toast.makeText(this, "Goal automatically increased to " + newGoal + " steps!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED) {
            if (stepSensor != null) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else if (stepDetectorSensor != null) {
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stepSensor != null) {
            sensorManager.unregisterListener(this, stepSensor);
        }
        if (stepDetectorSensor != null) {
            sensorManager.unregisterListener(this, stepDetectorSensor);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        if (stepUpdateReceiver != null) {
            unregisterReceiver(stepUpdateReceiver);
        }
        if (goalAchievementReceiver != null) {
            unregisterReceiver(goalAchievementReceiver);
        }
    }

    private void startStepCounterService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED) {
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }
} 