package com.example.healthcare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SleepTrackerActivity extends AppCompatActivity {

    private static final String TAG = "SleepTrackerActivity";
    private static final String PREF_SLEEP_GOAL = "sleep_goal_hours";
    private static final int BEDTIME_ALARM_ID = 1001;
    private static final int WAKETIME_ALARM_ID = 1002;
    private static final String PREF_SLEEP_START_KEY = "sleep_start_time";
    private static final String PREF_SLEEP_END_KEY = "sleep_end_time";
    
    private TextView sleepStatusText;
    private TextView sleepGoalText;
    private TextView scheduledBedtimeText;
    private TextView scheduledWakeTimeText;
    private SharedPreferences sharedPreferences;
    private long sleepStartTime;
    private long sleepEndTime;
    private AlarmManager alarmManager;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String today;
    private TextView sleepStartTimeText;
    private TextView sleepEndTimeText;
    private TextView sleepDurationText;
    private MaterialButton startSleepButton;
    private MaterialButton endSleepButton;
    private boolean isSleepTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_tracker);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        // Initialize views
        sleepStartTimeText = findViewById(R.id.sleep_start_time_text);
        sleepEndTimeText = findViewById(R.id.sleep_end_time_text);
        sleepDurationText = findViewById(R.id.sleep_duration_text);
        startSleepButton = findViewById(R.id.start_sleep_button);
        endSleepButton = findViewById(R.id.end_sleep_button);
        sleepGoalText = findViewById(R.id.sleepGoalText);
        scheduledBedtimeText = findViewById(R.id.scheduledBedtimeText);
        scheduledWakeTimeText = findViewById(R.id.scheduledWakeTimeText);

        // Initialize SharedPreferences - use only one instance
        sharedPreferences = getSharedPreferences("HealthCarePrefs", MODE_PRIVATE);

        // Load existing sleep data
        loadSleepData();

        // Set up buttons
        startSleepButton.setOnClickListener(v -> startSleepTracking());
        endSleepButton.setOnClickListener(v -> endSleepTracking());

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        MaterialButton setBedtimeButton = findViewById(R.id.setBedtimeButton);
        MaterialButton setWakeTimeButton = findViewById(R.id.setWakeTimeButton);
        MaterialButton setSleepGoalButton = findViewById(R.id.setSleepGoalButton);
        EditText sleepGoalInput = findViewById(R.id.sleepGoalInput);
        
        int sleepGoal = sharedPreferences.getInt(PREF_SLEEP_GOAL, 8);
        
        updateSleepStatus();
        updateScheduledTimes();
        sleepGoalText.setText(String.format("Sleep Goal: %d hours", sleepGoal));

        setSleepGoalButton.setOnClickListener(v -> {
            String goalStr = sleepGoalInput.getText().toString();
            if (!goalStr.isEmpty()) {
                int newGoal = Integer.parseInt(goalStr);
                if (newGoal > 0 && newGoal <= 24) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(PREF_SLEEP_GOAL, newGoal);
                    editor.apply();
                    sleepGoalText.setText(String.format("Sleep Goal: %d hours", newGoal));
                    Toast.makeText(this, "Sleep goal updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please enter a valid goal (1-24 hours)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setBedtimeButton.setOnClickListener(v -> showTimePicker(true));
        setWakeTimeButton.setOnClickListener(v -> showTimePicker(false));
    }

    private void loadSleepData() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to track sleep", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

            todayRef.get().addOnSuccessListener(snapshot -> {
                try {
                    if (snapshot.exists()) {
                        Long startValue = snapshot.child("sleep_start").getValue(Long.class);
                        Long endValue = snapshot.child("sleep_end").getValue(Long.class);
                        Boolean isTracking = snapshot.child("is_sleep_tracking").getValue(Boolean.class);

                        if (startValue != null && startValue > 0) {
                            sleepStartTime = startValue;
                            isSleepTracking = isTracking != null ? isTracking : false;
                        }
                        if (endValue != null && endValue > 0) {
                            sleepEndTime = endValue;
                            isSleepTracking = false;
                        }
                        updateUI();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing sleep data: " + e.getMessage());
                    Toast.makeText(SleepTrackerActivity.this,
                        "Error processing sleep data",
                        Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error reading sleep data: " + e.getMessage());
                Toast.makeText(SleepTrackerActivity.this,
                    "Error reading sleep data: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadSleepData: " + e.getMessage());
            Toast.makeText(this, "Error loading sleep data", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSleepData() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to save sleep data", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

            Map<String, Object> updates = new HashMap<>();
            updates.put("sleep_start", sleepStartTime);
            updates.put("sleep_end", sleepEndTime);
            updates.put("is_sleep_tracking", isSleepTracking);

            todayRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sleep data saved successfully");
                    updateUI();
                    Toast.makeText(SleepTrackerActivity.this,
                        "Sleep data saved successfully",
                        Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving sleep data: " + e.getMessage());
                    Toast.makeText(SleepTrackerActivity.this,
                        "Error saving sleep data: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in saveSleepData: " + e.getMessage());
            Toast.makeText(this, "Error saving sleep data", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSleepTracking() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to track sleep", Toast.LENGTH_SHORT).show();
                return;
            }

            sleepStartTime = System.currentTimeMillis();
            isSleepTracking = true;
            Log.d(TAG, "Starting sleep tracking at: " + sleepStartTime);

            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

            Map<String, Object> updates = new HashMap<>();
            updates.put("sleep_start", sleepStartTime);
            updates.put("is_sleep_tracking", true);

            todayRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sleep tracking started successfully");
                    updateUI();
                    Toast.makeText(SleepTrackerActivity.this,
                        "Sleep tracking started",
                        Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving sleep start time: " + e.getMessage());
                    Toast.makeText(SleepTrackerActivity.this,
                        "Error saving sleep start time: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in startSleepTracking: " + e.getMessage());
            Toast.makeText(this, "Error starting sleep tracking", Toast.LENGTH_SHORT).show();
        }
    }

    private void endSleepTracking() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to track sleep", Toast.LENGTH_SHORT).show();
                return;
            }

            sleepEndTime = System.currentTimeMillis();
            isSleepTracking = false;
            Log.d(TAG, "Ending sleep tracking at: " + sleepEndTime);

            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

            Map<String, Object> updates = new HashMap<>();
            updates.put("sleep_end", sleepEndTime);
            updates.put("is_sleep_tracking", false);

            todayRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sleep tracking ended successfully");
                    updateUI();
                    Toast.makeText(SleepTrackerActivity.this,
                        "Sleep tracking ended",
                        Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving sleep end time: " + e.getMessage());
                    Toast.makeText(SleepTrackerActivity.this,
                        "Error saving sleep end time: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in endSleepTracking: " + e.getMessage());
            Toast.makeText(this, "Error ending sleep tracking", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStartTimeUI() {
        if (sleepStartTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = sdf.format(new Date(sleepStartTime));
            sleepStartTimeText.setText("Sleep Start: " + timeStr);
        } else {
            sleepStartTimeText.setText("Sleep Start: Not started");
        }
    }

    private void updateEndTimeUI() {
        if (sleepEndTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = sdf.format(new Date(sleepEndTime));
            sleepEndTimeText.setText("Sleep End: " + timeStr);
        } else {
            sleepEndTimeText.setText("Sleep End: Not ended");
        }
    }

    private void updateDurationUI() {
        if (sleepStartTime > 0 && sleepEndTime > 0) {
            long duration = sleepEndTime - sleepStartTime;
            long hours = TimeUnit.MILLISECONDS.toHours(duration);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
            sleepDurationText.setText(String.format(Locale.getDefault(),
                "Sleep Duration: %d hours %d minutes", hours, minutes));
        } else {
            sleepDurationText.setText("Sleep Duration: Not available");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadSleepData();
    }

    private void showTimePicker(boolean isBedtime) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(isBedtime ? 22 : 7)
            .setMinute(0)
            .setTitleText(isBedtime ? "Set Bedtime" : "Set Wake Time")
            .build();

        picker.addOnPositiveButtonClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, picker.getHour());
            calendar.set(Calendar.MINUTE, picker.getMinute());
            calendar.set(Calendar.SECOND, 0);
            
            // If the time is in the past, add one day
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(calendar.getTime());
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(isBedtime ? "bedtime" : "waketime", timeStr);
            editor.putLong(isBedtime ? "bedtime_millis" : "waketime_millis", 
                calendar.getTimeInMillis());
            editor.apply();

            // Schedule the alarm
            scheduleAlarm(calendar.getTimeInMillis(), isBedtime);

            Toast.makeText(this, 
                (isBedtime ? "Bedtime" : "Wake time") + " set to " + timeStr,
                Toast.LENGTH_SHORT).show();
                
            updateScheduledTimes();
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void scheduleAlarm(long timeInMillis, boolean isBedtime) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("is_bedtime", isBedtime);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            isBedtime ? BEDTIME_ALARM_ID : WAKETIME_ALARM_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel any existing alarm
        alarmManager.cancel(pendingIntent);

        // Schedule new alarm
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        }
    }

    private void updateScheduledTimes() {
        String bedtime = sharedPreferences.getString("bedtime", "Not set");
        String wakeTime = sharedPreferences.getString("waketime", "Not set");
        
        if (scheduledBedtimeText != null) {
            scheduledBedtimeText.setText("Scheduled Bedtime: " + bedtime);
        }
        if (scheduledWakeTimeText != null) {
            scheduledWakeTimeText.setText("Scheduled Wake Time: " + wakeTime);
        }
    }

    private void updateSleepStatus() {
        if (sleepStartTime > 0 && sleepEndTime > 0) {
            long durationMillis = sleepEndTime - sleepStartTime;
            long hours = durationMillis / (1000 * 60 * 60);
            long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (durationMillis % (1000 * 60)) / 1000;
            
            // Update the duration text instead of status text
            if (sleepDurationText != null) {
                sleepDurationText.setText(String.format("Sleep Duration: %d hrs %d min %d sec", 
                    hours, minutes, seconds));
            }
        } else if (sleepStartTime > 0) {
            // Update the start time text to show tracking in progress
            if (sleepStartTimeText != null) {
                sleepStartTimeText.setText("Sleep tracking in progress...");
            }
        }
    }

    private void updateUI() {
        updateStartTimeUI();
        updateEndTimeUI();
        updateDurationUI();
        updateSleepStatus();
        updateScheduledTimes();
    }
} 