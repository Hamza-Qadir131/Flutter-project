package com.example.healthcare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "StepCounterChannel";
    private static final String ACHIEVEMENT_CHANNEL_ID = "StepAchievementChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int ACHIEVEMENT_NOTIFICATION_ID = 2;
    private static final String PREF_STEPS_KEY = "daily_steps";
    private static final String PREF_INITIAL_STEPS_KEY = "initial_steps";
    private static final String PREF_STEPS_GOAL = "steps_goal";
    private static final String PREF_GOAL_NOTIFIED_KEY = "goal_notified_";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences sharedPreferences;
    private int currentSteps = 0;
    private int initialSteps = -1;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sharedPreferences = getSharedPreferences("HealthcareApp", MODE_PRIVATE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Load current steps
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        currentSteps = sharedPreferences.getInt(PREF_STEPS_KEY + today, 0);
        initialSteps = sharedPreferences.getInt(PREF_INITIAL_STEPS_KEY + today, -1);

        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                updateNotification();
                checkStepGoal();
            }
        }
    }

    private void checkStepGoal() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        int stepGoal = sharedPreferences.getInt(PREF_STEPS_GOAL, 10000);
        boolean goalNotified = sharedPreferences.getBoolean(PREF_GOAL_NOTIFIED_KEY + today, false);

        if (currentSteps >= stepGoal && !goalNotified) {
            // Show achievement notification
            showGoalAchievementNotification(stepGoal);
            
            // Mark goal as notified for today
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(PREF_GOAL_NOTIFIED_KEY + today, true);
            editor.apply();

            // Broadcast achievement
            Intent achievementIntent = new Intent("step_goal_achieved");
            achievementIntent.putExtra("steps", currentSteps);
            achievementIntent.putExtra("goal", stepGoal);
            sendBroadcast(achievementIntent);
        }
    }

    private void showGoalAchievementNotification(int goal) {
        Intent notificationIntent = new Intent(this, StepTrackerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, ACHIEVEMENT_CHANNEL_ID)
                .setContentTitle("Congratulations! 🎉")
                .setContentText("You've reached your daily goal of " + goal + " steps!")
                .setSmallIcon(R.drawable.ic_directions_walk)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(ACHIEVEMENT_NOTIFICATION_ID, notification);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void saveSteps() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_STEPS_KEY + today, currentSteps);
        editor.apply();

        // Broadcast the step count update
        Intent intent = new Intent("step_counter_update");
        intent.putExtra("steps", currentSteps);
        sendBroadcast(intent);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Step Counter Channel
            NotificationChannel stepChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter",
                    NotificationManager.IMPORTANCE_LOW
            );
            stepChannel.setDescription("Shows current step count");
            
            // Achievement Channel
            NotificationChannel achievementChannel = new NotificationChannel(
                    ACHIEVEMENT_CHANNEL_ID,
                    "Step Goal Achievements",
                    NotificationManager.IMPORTANCE_HIGH
            );
            achievementChannel.setDescription("Notifies when you reach your step goals");
            achievementChannel.enableVibration(true);
            achievementChannel.setVibrationPattern(new long[]{0, 500, 250, 500});
            
            notificationManager.createNotificationChannel(stepChannel);
            notificationManager.createNotificationChannel(achievementChannel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, StepTrackerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Step Counter Active")
                .setContentText(currentSteps + " steps taken today")
                .setSmallIcon(R.drawable.ic_directions_walk)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }
} 