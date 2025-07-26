package com.example.healthcare;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class HomeFragment extends Fragment implements SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "HomeFragment";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final long LOCATION_TIMEOUT = 15000; // 15 seconds
    private static final int DEFAULT_STEPS_GOAL = 10000;
    private static final int WATER_GOAL_ML = 2000; // 2 liters daily goal
    private static final int WATER_INCREMENT = 250; // ml

    private static final int REQUEST_STEPS = 1001;
    private static final int REQUEST_WATER = 1002;
    private static final int REQUEST_SLEEP = 1003;

    private static final String PREF_STEPS_KEY = "daily_steps";
    private static final String PREF_WATER_KEY = "daily_water";
    private static final String PREF_SLEEP_START_KEY = "sleep_start";
    private static final String PREF_SLEEP_END_KEY = "sleep_end";
    private static final String PREF_GOAL_REACHED = "goal_reached_today_";
    private static final String PREF_STEPS_GOAL = "steps_goal";
    private static final String PREF_INITIAL_STEPS_KEY = "initial_steps";

    private static final String CHANNEL_ID = "goal_achievements";
    private static final int NOTIFICATION_ID_STEPS = 1001;
    private static final int NOTIFICATION_ID_WATER = 1002;

    private Button panicButton;
    private ProgressBar progressBar;
    private View emergencyOverlay;
    private TextView emergencyStatusText;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    private Vibrator vibrator;
    private boolean isAlertInProgress = false;
    private LocationCallback locationCallback;
    private Handler timeoutHandler;
    private boolean isLocationUpdatesStarted = false;

    private TextView stepCountText;
    private ProgressBar stepProgress;
    private TextView waterIntakeText;
    private MaterialButton addWaterButton;
    private TextView sleepDurationText;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int currentSteps = 0;
    private int dailyWaterIntake = 0;
    private long sleepStartTime = 0;
    private long sleepEndTime = 0;
    private boolean isGoalReachedToday = false;
    private int initialSteps = 0;

    private MaterialButton resetStepsButton;
    private MaterialButton resetWaterButton;
    private MaterialButton resetSleepButton;

    private ToneGenerator toneGenerator;
    private boolean isWaterGoalReached = false;

    private NotificationManager notificationManager;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private boolean isSleepTracking = false;

    private String today;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "HomeFragment onCreate called");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Get today's date in yyyyMMdd format
        today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("HealthCarePrefs", Context.MODE_PRIVATE);

        // Check if it's a new day
        String lastDate = sharedPreferences.getString("last_date", "");
        if (!lastDate.equals(today)) {
            Log.d(TAG, "New day detected. Resetting values.");
            // Reset all values for new day
            initialSteps = -1;  // Reset initial steps to force new initialization
            currentSteps = 0;
            isGoalReachedToday = false;
            isWaterGoalReached = false;
            dailyWaterIntake = 0;
            sleepStartTime = 0;
            sleepEndTime = 0;

            // Save new date
            sharedPreferences.edit()
                    .putString("last_date", today)
                    .putInt(PREF_INITIAL_STEPS_KEY, -1)  // Reset initial steps in SharedPreferences
                    .putInt(PREF_STEPS_KEY, 0)  // Reset current steps in SharedPreferences
                    .apply();

            // Initialize new day data in Firebase
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                DatabaseReference todayRef = databaseReference.child("users")
                        .child(userId)
                        .child("logs")
                        .child(today);

                Map<String, Object> newDayData = new HashMap<>();
                newDayData.put("steps", 0);
                newDayData.put("initial_steps", -1);  // Reset initial steps in Firebase
                newDayData.put("water_intake", 0);
                newDayData.put("water_goal_reached", false);
                newDayData.put("sleep_start", 0);
                newDayData.put("sleep_end", 0);

                todayRef.setValue(newDayData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "New day data initialized in Firebase");
                            Toast.makeText(getContext(), "New day started!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error initializing new day data: " + e.getMessage());
                            Toast.makeText(getContext(), "Error initializing new day data", Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            Log.d(TAG, "Loading existing data for today");
            // Load existing data
            loadDailyData();
        }

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            notificationManager = (NotificationManager) requireActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel();
            timeoutHandler = new Handler(Looper.getMainLooper());
            setupLocationCallback();
            setupSensors();

            Log.d(TAG, "HomeFragment initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing HomeFragment: " + e.getMessage());
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    stopLocationUpdates();
                    sendEmergencyMessages(
                            locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()
                    );
                }
            }
        };
    }

    private void setupSensors() {
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor == null) {
            Toast.makeText(requireContext(), "Step counter not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSleepDataListener() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in for sleep data listener");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Setting up sleep data listener for user: " + userId);

        DatabaseReference sleepRef = databaseReference.child("users")
                .child(userId)
                .child("logs")
                .child(today);

        sleepRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        Log.d(TAG, "Sleep data changed in Firebase");

                        Long startValue = snapshot.child("sleep_start").getValue(Long.class);
                        Long endValue = snapshot.child("sleep_end").getValue(Long.class);
                        Boolean isTracking = snapshot.child("is_sleep_tracking").getValue(Boolean.class);

                        Log.d(TAG, "Sleep data from Firebase - Start: " + startValue +
                                ", End: " + endValue +
                                ", Tracking: " + isTracking);

                        if (startValue != null && startValue > 0) {
                            sleepStartTime = startValue;
                            Log.d(TAG, "Updated sleepStartTime to: " + sleepStartTime);
                        }

                        if (endValue != null && endValue > 0) {
                            sleepEndTime = endValue;
                            Log.d(TAG, "Updated sleepEndTime to: " + sleepEndTime);
                        }

                        if (isTracking != null) {
                            isSleepTracking = isTracking;
                            Log.d(TAG, "Updated isSleepTracking to: " + isSleepTracking);
                        }

                        // Save to SharedPreferences as backup
                        saveToSharedPreferences();
                        updateUI();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in sleep data listener: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Sleep data listener cancelled: " + error.getMessage());
            }
        });
    }

    private void loadDailyData() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in");
            loadFromSharedPreferences();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading data for user: " + userId + " and date: " + today);

        DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

        // First check if the node exists
        todayRef.get().addOnSuccessListener(snapshot -> {
            try {
                if (snapshot.exists()) {
                    Log.d(TAG, "Data exists in Firebase for today");

                    // Load water data
                    Integer waterValue = snapshot.child("water").getValue(Integer.class);
                    Log.d(TAG, "Water value from Firebase: " + waterValue);
                    if (waterValue != null) {
                        dailyWaterIntake = waterValue;
                        Log.d(TAG, "Updated dailyWaterIntake to: " + dailyWaterIntake);
                    }

                    // Load sleep data
                    Long startValue = snapshot.child("sleep_start").getValue(Long.class);
                    Long endValue = snapshot.child("sleep_end").getValue(Long.class);
                    Boolean isTracking = snapshot.child("is_sleep_tracking").getValue(Boolean.class);
                    Log.d(TAG, "Sleep data from Firebase - Start: " + startValue +
                            ", End: " + endValue +
                            ", Tracking: " + isTracking);

                    if (startValue != null && startValue > 0) {
                        sleepStartTime = startValue;
                        isSleepTracking = isTracking != null ? isTracking : false;
                        Log.d(TAG, "Updated sleepStartTime to: " + sleepStartTime);
                    }
                    if (endValue != null && endValue > 0) {
                        sleepEndTime = endValue;
                        isSleepTracking = false;
                        Log.d(TAG, "Updated sleepEndTime to: " + sleepEndTime);
                    }

                    // Save to SharedPreferences as backup
                    saveToSharedPreferences();
                    updateUI();
                    Log.d(TAG, "Data loaded and UI updated successfully");
                } else {
                    Log.d(TAG, "No data exists in Firebase for today, initializing new data");
                    // Load from SharedPreferences first
                    loadFromSharedPreferences();
                    // Then initialize in Firebase
                    initializeNewDayData();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading data from Firebase: " + e.getMessage());
                e.printStackTrace();
                loadFromSharedPreferences();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to read from Firebase: " + e.getMessage());
            e.printStackTrace();
            loadFromSharedPreferences();
        });
    }

    private void initializeNewDayData() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Initializing new day data for user: " + userId);

        DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

        Map<String, Object> newDayData = new HashMap<>();
        newDayData.put("water", dailyWaterIntake);
        newDayData.put("water_goal", WATER_GOAL_ML);
        newDayData.put("water_goal_reached", dailyWaterIntake >= WATER_GOAL_ML);
        newDayData.put("sleep_start", sleepStartTime);
        newDayData.put("sleep_end", sleepEndTime);
        newDayData.put("is_sleep_tracking", isSleepTracking);

        todayRef.setValue(newDayData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New day data initialized successfully in Firebase");
                    saveToSharedPreferences();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error initializing new day data in Firebase: " + e.getMessage());
                    e.printStackTrace();
                    saveToSharedPreferences();
                });
    }

    private void loadFromSharedPreferences() {
        try {
            // Load water data
            dailyWaterIntake = sharedPreferences.getInt(PREF_WATER_KEY + today, 0);
            isWaterGoalReached = sharedPreferences.getBoolean("water_goal_reached_" + today, false);

            // Load sleep data
            sleepStartTime = sharedPreferences.getLong(PREF_SLEEP_START_KEY + today, 0);
            sleepEndTime = sharedPreferences.getLong(PREF_SLEEP_END_KEY + today, 0);
            isSleepTracking = sharedPreferences.getBoolean("is_sleep_tracking_" + today, false);

            // Load steps data
            currentSteps = sharedPreferences.getInt(PREF_STEPS_KEY + today, 0);
            initialSteps = sharedPreferences.getInt(PREF_INITIAL_STEPS_KEY + today, 0);

            updateUI();
            Log.d(TAG, "Data loaded successfully from SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error loading data from SharedPreferences: " + e.getMessage());
            Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToSharedPreferences() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(PREF_WATER_KEY + today, dailyWaterIntake);
            editor.putBoolean("water_goal_reached_" + today, isWaterGoalReached);
            editor.putLong(PREF_SLEEP_START_KEY + today, sleepStartTime);
            editor.putLong(PREF_SLEEP_END_KEY + today, sleepEndTime);
            editor.putBoolean("is_sleep_tracking_" + today, isSleepTracking);
            editor.putInt(PREF_STEPS_KEY + today, currentSteps);
            editor.putInt(PREF_INITIAL_STEPS_KEY + today, initialSteps);
            editor.apply();
            Log.d(TAG, "Data saved successfully to SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving data to SharedPreferences: " + e.getMessage());
        }
    }

    private void saveDailyWaterIntake() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in for saving water intake");
            saveToSharedPreferences();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Saving water intake for user: " + userId + ", intake: " + dailyWaterIntake);

        DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

        Map<String, Object> updates = new HashMap<>();
        updates.put("water", dailyWaterIntake);
        updates.put("water_goal", WATER_GOAL_ML);
        updates.put("water_goal_reached", dailyWaterIntake >= WATER_GOAL_ML);

        todayRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Water intake saved successfully to Firebase");
                    saveToSharedPreferences();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving water intake to Firebase: " + e.getMessage());
                    e.printStackTrace();
                    saveToSharedPreferences();
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "Error saving water intake", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveDailySleepData() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in for saving sleep data");
            saveToSharedPreferences();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Saving sleep data for user: " + userId +
                ", start: " + sleepStartTime +
                ", end: " + sleepEndTime +
                ", tracking: " + isSleepTracking);

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
                    Log.d(TAG, "Sleep data saved successfully to Firebase");
                    saveToSharedPreferences();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving sleep data to Firebase: " + e.getMessage());
                    e.printStackTrace();
                    saveToSharedPreferences();
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "Error saving sleep data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            initializeViews(view);
            setupHealthTracking();
            setupPanicButton();
            animateCards();
            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeViews(View view) {
        try {
            // Initialize emergency-related views
            panicButton = view.findViewById(R.id.addButton);
            progressBar = view.findViewById(R.id.progressBar);
            emergencyOverlay = view.findViewById(R.id.emergencyOverlay);
            emergencyStatusText = view.findViewById(R.id.emergencyStatusText);

            // Initialize health tracking views
            stepCountText = view.findViewById(R.id.stepsText);
            stepProgress = view.findViewById(R.id.stepProgressBar);
            waterIntakeText = view.findViewById(R.id.waterText);
            addWaterButton = view.findViewById(R.id.addLargeButton);
            sleepDurationText = view.findViewById(R.id.sleepStatusText);

            // Initialize reset buttons
            resetStepsButton = view.findViewById(R.id.resetStepsButton);
            resetWaterButton = view.findViewById(R.id.resetWaterButton);
            resetSleepButton = view.findViewById(R.id.resetSleepButton);

            // Hide emergency overlay initially
            if (emergencyOverlay != null) {
                emergencyOverlay.setVisibility(View.GONE);
            }

            setupClickListeners(view.findViewById(R.id.addContactButton));

            // Make cards clickable
            View stepsCard = view.findViewById(R.id.stepsCard);
            View waterCard = view.findViewById(R.id.waterCard);
            View sleepCard = view.findViewById(R.id.sleepCard);

            if (stepsCard != null) {
                stepsCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), StepTrackerActivity.class);
                    intent.putExtra("current_steps", currentSteps);
                    startActivityForResult(intent, REQUEST_STEPS);
                });
            }

            if (waterCard != null) {
                waterCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), WaterIntakeActivity.class);
                    intent.putExtra("current_intake", dailyWaterIntake);
                    startActivityForResult(intent, REQUEST_WATER);
                });
            }

            if (sleepCard != null) {
                sleepCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), SleepTrackerActivity.class);
                    intent.putExtra("sleep_start", sleepStartTime);
                    intent.putExtra("sleep_end", sleepEndTime);
                    startActivityForResult(intent, REQUEST_SLEEP);
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            showError("Error initializing views: " + e.getMessage());
        }
    }

    private void setupClickListeners(Button manageContactsButton) {
        if (manageContactsButton != null) {
            manageContactsButton.setOnClickListener(v -> {
                // Check saved contact when manage contacts button is clicked
                checkSavedEmergencyContact();
                Intent intent = new Intent(getActivity(), EmergencyContactsActivity.class);
                startActivity(intent);
            });
        }

        if (addWaterButton != null) {
            addWaterButton.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(50)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(50);
                            dailyWaterIntake += WATER_INCREMENT;
                            saveDailyWaterIntake();

                            // Add ripple effect
                            if (waterIntakeText != null) {
                                waterIntakeText.animate()
                                        .alpha(0.5f)
                                        .setDuration(100)
                                        .withEndAction(() ->
                                                waterIntakeText.animate()
                                                        .alpha(1f)
                                                        .setDuration(100));
                            }
                        });
            });
        }

        // Setup reset buttons
        if (resetStepsButton != null) {
            resetStepsButton.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Reset Steps")
                        .setMessage("Are you sure you want to reset your step count?")
                        .setPositiveButton("Reset", (dialog, which) -> {
                            resetStepCount();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (resetWaterButton != null) {
            resetWaterButton.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Reset Water Intake")
                        .setMessage("Are you sure you want to reset your water intake?")
                        .setPositiveButton("Reset", (dialog, which) -> {
                            resetWaterIntake();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (resetSleepButton != null) {
            resetSleepButton.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Reset Sleep Time")
                        .setMessage("Are you sure you want to reset your sleep tracking?")
                        .setPositiveButton("Reset", (dialog, which) -> {
                            resetSleepTracking();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void setupPanicButton() {
        if (panicButton != null) {
            panicButton.setOnClickListener(v -> {
                Log.d(TAG, "Panic button clicked");
                vibrateButton();
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100);
                            handleEmergencyButton();
                        });
            });
        }
    }

    private void vibrateButton() {
        if (getActivity() != null) {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(200);
                }
            }
        }
    }

    private void handleEmergencyButton() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to use emergency features", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get emergency contacts from SharedPreferences
        Set<String> emergencyContacts = getEmergencyContacts();
        Log.d(TAG, "Emergency contacts from SharedPreferences: " + emergencyContacts);

        if (emergencyContacts != null && !emergencyContacts.isEmpty()) {
            // Check location permission
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Check SMS permission
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Start emergency alert process
            triggerPanicAlert();
        } else {
            Toast.makeText(getContext(), "Please add emergency contacts first", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), EmergencyContactsActivity.class);
            startActivity(intent);
        }
    }

    private void sendEmergencyAlert(String emergencyContact) {
        Log.d(TAG, "Sending emergency alert to: " + emergencyContact);
        if (emergencyContact == null || emergencyContact.isEmpty()) {
            Toast.makeText(getContext(), "Emergency contact not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission required for emergency alerts", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        String message = "EMERGENCY ALERT!\n" +
                                "Patient: " + mAuth.getCurrentUser().getDisplayName() + "\n" +
                                "Location: " + location.getLatitude() + ", " + location.getLongitude() + "\n" +
                                "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                        Log.d(TAG, "Sending SMS with message: " + message);

                        // Send SMS
                        SmsManager smsManager = SmsManager.getDefault();
                        ArrayList<String> parts = smsManager.divideMessage(message);
                        smsManager.sendMultipartTextMessage(emergencyContact, null, parts, null, null);

                        Toast.makeText(getContext(), "Emergency alert sent successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Location is null");
                        Toast.makeText(getContext(), "Could not get location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting location: " + e.getMessage());
                    Toast.makeText(getContext(), "Error getting location", Toast.LENGTH_SHORT).show();
                });
    }

    private void requestNeededPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.VIBRATE
        };

        if (getActivity() != null) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkRequiredPermissions() {
        try {
            // Check Location Permission
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showError("Location permission required. Please grant permission.");
                requestNeededPermissions();
                return false;
            }

            // Check SMS Permission
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                showError("SMS permission required. Please grant permission.");
                requestNeededPermissions();
                return false;
            }

            // Check Vibrate Permission
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
                showError("Vibrate permission required. Please grant permission.");
                requestNeededPermissions();
                return false;
            }

            // Check if GPS is enabled
            LocationManager locationManager = (LocationManager) requireActivity()
                    .getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showError("Please enable GPS location");
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                return false;
            }

            // Check if we have emergency contacts
            Set<String> contacts = getEmergencyContacts();
            if (contacts.isEmpty()) {
                showError("Please add emergency contacts first");
                Intent intent = new Intent(getActivity(), EmergencyContactsActivity.class);
                startActivity(intent);
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
            showError("Error checking permissions: " + e.getMessage());
            return false;
        }
    }

    private void promptAddContacts() {
        Snackbar.make(requireView(), "Add emergency contacts?", Snackbar.LENGTH_LONG)
                .setAction("ADD", v -> {
                    Intent intent = new Intent(getActivity(), EmergencyContactsActivity.class);
                    startActivity(intent);
                }).show();
    }

    private void promptEnableGPS() {
        Snackbar.make(requireView(), "Enable GPS?", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS", v -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                triggerPanicAlert();
            } else {
                showError("All permissions are required for the panic button to work");
            }
        }
    }

    private void showError(String message) {
        if (isAdded() && getActivity() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus(String message) {
        if (emergencyStatusText != null && emergencyOverlay != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                try {
                    emergencyOverlay.setVisibility(View.VISIBLE);
                    emergencyStatusText.setText(message);
                    emergencyStatusText.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Emergency status updated: " + message); // Debug log

                    // Only auto-hide if not in active alert
                    if (!isAlertInProgress) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (isAdded()) {
                                emergencyOverlay.animate()
                                        .alpha(0f)
                                        .setDuration(500)
                                        .withEndAction(() -> {
                                            emergencyOverlay.setVisibility(View.GONE);
                                            emergencyOverlay.setAlpha(1f);
                                        });
                            }
                        }, 5000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating status: " + e.getMessage());
                }
            });
        } else {
            Log.e(TAG, "Emergency views not properly initialized");
        }
    }

    private void showProgress(boolean show) {
        if (progressBar != null && emergencyOverlay != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                emergencyOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        }
    }

    private void triggerPanicAlert() {
        try {
            isAlertInProgress = true;
            showProgress(true);
            if (emergencyOverlay != null) {
                emergencyOverlay.setVisibility(View.VISIBLE);
            }
            updateStatus("â� ï¸� Triggering Emergency Alert...");
            getCurrentLocationAndSendAlert();

            // Set a timeout for location updates
            timeoutHandler.postDelayed(() -> {
                if (isAlertInProgress) {
                    updateStatus("â�¡ Sending alert without location...");
                    stopLocationUpdates();
                    sendEmergencyMessages(0, 0);
                }
            }, LOCATION_TIMEOUT);
        } catch (Exception e) {
            Log.e(TAG, "Error triggering alert: " + e.getMessage());
            updateStatus("â�� Error: " + e.getMessage());
            showError("Error triggering alert");
            isAlertInProgress = false;
        }
    }

    private void getCurrentLocationAndSendAlert() {
        try {
            updateStatus("â� ï¸� Getting your location...");

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                showError("Location permission missing");
                showProgress(false);
                isAlertInProgress = false;
                return;
            }

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setIntervalMillis(5000)
                    .setMinUpdateIntervalMillis(2000)
                    .build();

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            updateStatus("ð��� Location found! Sending alerts...");
                            stopLocationUpdates();
                            sendEmergencyMessages(location.getLatitude(), location.getLongitude());
                        } else {
                            updateStatus("ð��� Searching for better location...");
                            startLocationUpdates(locationRequest);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting location: " + e.getMessage());
                        updateStatus("â� ï¸� Retrying location search...");
                        startLocationUpdates(locationRequest);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error getting location: " + e.getMessage());
            showError("Error getting location");
            isAlertInProgress = false;
        }
    }

    private void startLocationUpdates(LocationRequest locationRequest) {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            updateStatus("Getting new location fix...");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isLocationUpdatesStarted = true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
            sendEmergencyMessages(0, 0);
        }
    }

    private void stopLocationUpdates() {
        if (isLocationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesStarted = false;
        }
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    private void sendEmergencyMessages(double latitude, double longitude) {
        try {
            Set<String> emergencyContacts = getEmergencyContacts();
            if (emergencyContacts.isEmpty()) {
                updateStatus("â�� No emergency contacts found!");
                showError("No emergency contacts found. Please add contacts first.");
                showProgress(false);
                isAlertInProgress = false;
                return;
            }

            updateStatus("ð��± Sending emergency alerts...");
            String message = createEmergencyMessage(latitude, longitude);
            SmsManager smsManager = SmsManager.getDefault();
            int successCount = 0;

            for (String contactData : emergencyContacts) {
                try {
                    String[] parts = contactData.split("\\|");
                    if (parts.length == 2) {
                        String name = parts[0];
                        String phoneNumber = parts[1];

                        ArrayList<String> messageParts = smsManager.divideMessage(message);
                        smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);
                        Log.d(TAG, "Emergency SMS sent to: " + name);
                        successCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send SMS to contact: " + e.getMessage());
                }
            }

            if (successCount > 0) {
                updateStatus("â�� Alert sent to " + successCount + " contacts");
                Toast.makeText(requireContext(),
                        "Emergency alert sent to " + successCount + " contacts",
                        Toast.LENGTH_LONG).show();
            } else {
                updateStatus("â�� Failed to send alerts!");
                showError("Failed to send emergency alerts");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in sendEmergencyMessages: " + e.getMessage());
            updateStatus("â�� Error sending alerts!");
            showError("Error sending emergency alerts");
        } finally {
            showProgress(false);
            isAlertInProgress = false;

            // Keep the final status visible for a few seconds before hiding
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (emergencyOverlay != null && isAdded()) {
                    emergencyOverlay.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .withEndAction(() -> {
                                emergencyOverlay.setVisibility(View.GONE);
                                emergencyOverlay.setAlpha(1f);
                            });
                }
            }, 3000);

            stopLocationUpdates();
        }
    }

    private String createEmergencyMessage(double latitude, double longitude) {
        StringBuilder message = new StringBuilder();
        message.append("ð��¨ EMERGENCY ALERT!\n");
        message.append("I need immediate help!\n\n");

        if (latitude != 0 && longitude != 0) {
            message.append("My current location:\n");
            message.append("Google Maps Link: https://maps.google.com/?q=").append(latitude).append(",").append(longitude);
        } else {
            message.append("Location not available.\n");
            message.append("Please contact me immediately!");
        }
        return message.toString();
    }

    private Set<String> getEmergencyContacts() {
        // Use the same SharedPreferences instance as EmergencyContactsActivity
        SharedPreferences prefs = requireActivity().getSharedPreferences("HealthcareApp", Context.MODE_PRIVATE);
        Set<String> contacts = prefs.getStringSet("emergency_contacts", new HashSet<>());
        Log.d(TAG, "Retrieved emergency contacts from HealthcareApp prefs: " + contacts);
        return contacts != null ? contacts : new HashSet<>();
    }

    public void addEmergencyContact(String phoneNumber) {
        Set<String> contacts = new HashSet<>(getEmergencyContacts());
        contacts.add(phoneNumber);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("emergency_contacts", contacts);
        editor.apply();
        Log.d("HomeFragment", "Added contact: " + phoneNumber);
    }

    public void removeEmergencyContact(String phoneNumber) {
        Set<String> contacts = new HashSet<>(getEmergencyContacts());
        contacts.remove(phoneNumber);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("emergency_contacts", contacts);
        editor.apply();
        Log.d("HomeFragment", "Removed contact: " + phoneNumber);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        timeoutHandler.removeCallbacksAndMessages(null);
        if (emergencyOverlay != null) {
            emergencyOverlay.setVisibility(View.GONE);
        }
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    private void setupHealthTracking() {
        if (addWaterButton != null) {
            addWaterButton.setOnClickListener(v -> {
                dailyWaterIntake += WATER_INCREMENT;
                saveDailyWaterIntake();
                updateUI();
                Toast.makeText(requireContext(), "Added " + WATER_INCREMENT + "ml of water", Toast.LENGTH_SHORT).show();
            });
        }

        // Set click listeners for cards
        View stepsCard = getView().findViewById(R.id.stepsCard);
        View waterCard = getView().findViewById(R.id.waterCard);
        View sleepCard = getView().findViewById(R.id.sleepCard);

        if (stepsCard != null) {
            stepsCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), StepTrackerActivity.class);
                intent.putExtra("current_steps", currentSteps);
                startActivityForResult(intent, REQUEST_STEPS);
            });
        }

        if (waterCard != null) {
            waterCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WaterIntakeActivity.class);
                intent.putExtra("current_intake", dailyWaterIntake);
                startActivityForResult(intent, REQUEST_WATER);
            });
        }

        if (sleepCard != null) {
            sleepCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SleepTrackerActivity.class);
                intent.putExtra("sleep_start", sleepStartTime);
                intent.putExtra("sleep_end", sleepEndTime);
                startActivityForResult(intent, REQUEST_SLEEP);
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_STEPS && resultCode == Activity.RESULT_OK && data != null) {
            saveStepCount(data.getIntExtra("new_steps", currentSteps));
        } else if (requestCode == REQUEST_WATER && resultCode == Activity.RESULT_OK && data != null) {
            int newWaterIntake = data.getIntExtra("new_intake", dailyWaterIntake);
            if (newWaterIntake != dailyWaterIntake) {
                dailyWaterIntake = newWaterIntake;
                saveDailyWaterIntake();
                Log.d(TAG, "Water intake updated from activity result: " + dailyWaterIntake);
            }
        } else if (requestCode == REQUEST_SLEEP && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Received sleep tracking result");
            long newStartTime = data.getLongExtra("sleep_start", 0);
            long newEndTime = data.getLongExtra("sleep_end", 0);

            Log.d(TAG, "Sleep tracking result - Start: " + newStartTime + ", End: " + newEndTime);

            if (newStartTime > 0) {
                sleepStartTime = newStartTime;
                isSleepTracking = true;
            }
            if (newEndTime > 0) {
                sleepEndTime = newEndTime;
                isSleepTracking = false;
            }

            // Save to Firebase
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                DatabaseReference todayRef = databaseReference.child("users")
                        .child(userId)
                        .child("logs")
                        .child(today);

                Map<String, Object> updates = new HashMap<>();
                updates.put("sleep_start", sleepStartTime);
                updates.put("sleep_end", sleepEndTime);
                updates.put("is_sleep_tracking", isSleepTracking);

                todayRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Sleep data saved successfully in Firebase");
                            saveToSharedPreferences();
                            updateUI();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error saving sleep data in Firebase: " + e.getMessage());
                            e.printStackTrace();
                            saveToSharedPreferences();
                            updateUI();
                        });
            } else {
                saveToSharedPreferences();
                updateUI();
            }
        }
        updateUI();
    }

    private void updateUI() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                // Update steps
                TextView stepsText = getView().findViewById(R.id.stepsText);
                if (stepsText != null) {
                    stepsText.setText(String.format(Locale.getDefault(), "Steps: %d", currentSteps));
                }

                // Update water intake
                TextView waterIntakeText = getView().findViewById(R.id.waterText);
                if (waterIntakeText != null) {
                    waterIntakeText.setText(String.format(Locale.getDefault(),
                            "Water Intake: %dml / %dml", dailyWaterIntake, WATER_GOAL_ML));
                }

                // Update sleep time
                TextView sleepTimeText = getView().findViewById(R.id.sleepStatusText);
                if (sleepTimeText != null) {
                    if (isSleepTracking) {
                        sleepTimeText.setText("Sleep tracking in progress...");
                    } else if (sleepStartTime > 0 && sleepEndTime > 0) {
                        long duration = sleepEndTime - sleepStartTime;
                        long hours = TimeUnit.MILLISECONDS.toHours(duration);
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                        sleepTimeText.setText(String.format(Locale.getDefault(),
                                "Sleep Duration: %d hours %d minutes", hours, minutes));
                    } else {
                        sleepTimeText.setText("No sleep session recorded");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage());
            }
        });
    }

    private void saveStepCount(int steps) {
        try {
            String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            currentSteps = steps;

            Log.d(TAG, "Current steps: " + currentSteps + ", Goal: " + DEFAULT_STEPS_GOAL);

            // Check if goal is reached
            if (!isGoalReachedToday && currentSteps >= DEFAULT_STEPS_GOAL) {
                Log.d(TAG, "Step goal reached! Triggering notification...");
                isGoalReachedToday = true;

                // Show system notification
                showStepGoalNotification();

                // Show congratulations dialog
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Goal Reached! ð���")
                                    .setMessage("Congratulations! You've reached your daily step goal of " + DEFAULT_STEPS_GOAL + " steps!")
                                    .setPositiveButton("Keep Going!", (dialog, which) -> {
                                        // Increase goal by 500 steps
                                        int newGoal = DEFAULT_STEPS_GOAL + 500;
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putInt(PREF_STEPS_GOAL, newGoal);
                                        editor.apply();

                                        // Update UI with new goal
                                        if (stepProgress != null) {
                                            stepProgress.setMax(newGoal);
                                        }
                                        Snackbar.make(requireView(),
                                                "Goal automatically increased to " + newGoal + " steps!",
                                                Snackbar.LENGTH_LONG).show();
                                    })
                                    .show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error showing congratulations dialog: " + e.getMessage());
                        }
                    });
                }
            }

            // Save to Firebase
            saveDailyData();

            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "Error in saveStepCount: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetStepCount() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please login to reset steps", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String logPath = "users/" + userId + "/logs/" + today;

        DatabaseReference logRef = databaseReference.child(logPath);
        Map<String, Object> updates = new HashMap<>();
        updates.put("steps", 0);
        updates.put("lastReset", ServerValue.TIMESTAMP);

        logRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                currentSteps = 0;
                updateUI();
                Toast.makeText(getContext(), "Steps reset successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error resetting steps: " + e.getMessage());
                // Try to reset locally if Firebase fails
                currentSteps = 0;
                updateUI();
                Toast.makeText(getContext(), "Steps reset locally", Toast.LENGTH_SHORT).show();
            });
    }

    private void resetWaterIntake() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        dailyWaterIntake = 0;
        isWaterGoalReached = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_WATER_KEY + today, 0);
        editor.apply();
        updateUI();
        Toast.makeText(requireContext(), "Water intake reset successfully", Toast.LENGTH_SHORT).show();
    }

    private void resetSleepTracking() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        sleepStartTime = 0;
        sleepEndTime = 0;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_SLEEP_START_KEY + today, 0);
        editor.putLong(PREF_SLEEP_END_KEY + today, 0);
        editor.apply();
        updateUI();
        Toast.makeText(requireContext(), "Sleep tracking reset successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                if (initialSteps == -1) {
                    initialSteps = (int) event.values[0];
                    saveDailyData();
                }
                currentSteps = (int) event.values[0] - initialSteps;
                if (currentSteps < 0) currentSteps = 0;
                updateUI();
                saveDailyData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSensorChanged: " + e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "HomeFragment onResume called");

        // Check if it's a new day
        String currentDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!currentDate.equals(today)) {
            Log.d(TAG, "New day detected in onResume. Previous date: " + today + ", Current date: " + currentDate);
            today = currentDate;
            loadDailyData();
        } else {
            Log.d(TAG, "Same day in onResume, loading existing data");
            loadDailyData();
        }

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "HomeFragment onPause called");
        if (stepSensor != null) {
            sensorManager.unregisterListener(this);
        }
        // Save current state before pausing
        saveDailyWaterIntake();
        saveDailySleepData();
    }

    private void animateCards() {
        View[] cards = {
                requireView().findViewById(R.id.stepsCard),
                requireView().findViewById(R.id.waterCard),
                requireView().findViewById(R.id.sleepCard)
        };

        for (int i = 0; i < cards.length; i++) {
            Animation cardEnterAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.card_enter);
            cardEnterAnimation.setStartOffset(300 + (i * 100));
            cards[i].startAnimation(cardEnterAnimation);
        }
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Goal Achievements",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for step and water intake goal achievements");
                channel.enableVibration(true);
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);

                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);

                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created successfully");
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage());
        }
    }

    private void showStepGoalNotification() {
        try {
            if (notificationManager == null || getContext() == null) {
                return;
            }

            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_directions_walk)
                    .setContentTitle("ð��� Daily Step Goal Achieved!")
                    .setContentText("Congratulations! You've reached your goal of " + DEFAULT_STEPS_GOAL + " steps!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setLights(Color.BLUE, 1000, 1000)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent);

            notificationManager.notify(NOTIFICATION_ID_STEPS, builder.build());

            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000, 500, 1000}, -1));
                } else {
                    vibrator.vibrate(new long[]{0, 1000, 500, 1000}, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing step goal notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showWaterGoalNotification() {
        try {
            if (notificationManager == null || getContext() == null) {
                return;
            }

            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_water)
                    .setContentTitle("ð��§ Water Intake Goal Achieved!")
                    .setContentText("Great job! You've reached your daily water intake goal of " + WATER_GOAL_ML + "ml!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setLights(Color.BLUE, 1000, 1000)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent);

            notificationManager.notify(NOTIFICATION_ID_WATER, builder.build());

            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000, 500, 1000}, -1));
                } else {
                    vibrator.vibrate(new long[]{0, 1000, 500, 1000}, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing water goal notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveDailyData() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in for saving daily data");
            saveToSharedPreferences();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Saving daily data for user: " + userId);

        DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

        Map<String, Object> updates = new HashMap<>();
        updates.put("steps", currentSteps);
        updates.put("water", dailyWaterIntake);
        updates.put("water_goal", WATER_GOAL_ML);
        updates.put("sleep_start", sleepStartTime);
        updates.put("sleep_end", sleepEndTime);
        updates.put("goal_reached", isGoalReachedToday);
        updates.put("water_goal_reached", isWaterGoalReached);
        updates.put("initial_steps", initialSteps);
        updates.put("is_sleep_tracking", isSleepTracking);

        todayRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Daily data saved successfully to Firebase");
                    saveToSharedPreferences();
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving daily data: " + e.getMessage());
                    e.printStackTrace();
                    saveToSharedPreferences();
                    Context context = getContext();
                    if (context != null) {
                        String errorMessage = "Error saving data: ";
                        if (e.getMessage().contains("Permission denied")) {
                            errorMessage += "Please make sure you're logged in";
                        } else {
                            errorMessage += e.getMessage();
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startSleepTracking() {
        Log.d(TAG, "Starting sleep tracking");
        sleepStartTime = System.currentTimeMillis();
        isSleepTracking = true;

        // Update UI immediately
        if (sleepDurationText != null) {
            sleepDurationText.setText("Sleep tracking in progress...");
        }

        // Save to Firebase
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference todayRef = databaseReference.child("users")
                    .child(userId)
                    .child("logs")
                    .child(today);

            Map<String, Object> updates = new HashMap<>();
            updates.put("sleep_start", sleepStartTime);
            updates.put("sleep_end", 0L);
            updates.put("is_sleep_tracking", true);

            todayRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Sleep tracking started successfully in Firebase");
                        saveToSharedPreferences();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error starting sleep tracking in Firebase: " + e.getMessage());
                        e.printStackTrace();
                        saveToSharedPreferences();
                    });
        } else {
            saveToSharedPreferences();
        }

        Intent intent = new Intent(getActivity(), SleepTrackerActivity.class);
        startActivityForResult(intent, REQUEST_SLEEP);
    }

    private void endSleepTracking() {
        Log.d(TAG, "Ending sleep tracking");
        if (isSleepTracking) {
            sleepEndTime = System.currentTimeMillis();
            isSleepTracking = false;

            // Update UI immediately
            if (sleepDurationText != null) {
                long duration = sleepEndTime - sleepStartTime;
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                sleepDurationText.setText(String.format(Locale.getDefault(),
                        "Sleep Duration: %d hours %d minutes", hours, minutes));
            }

            // Save to Firebase
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                DatabaseReference todayRef = databaseReference.child("users")
                        .child(userId)
                        .child("logs")
                        .child(today);

                Map<String, Object> updates = new HashMap<>();
                updates.put("sleep_end", sleepEndTime);
                updates.put("is_sleep_tracking", false);

                todayRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Sleep tracking ended successfully in Firebase");
                            saveToSharedPreferences();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error ending sleep tracking in Firebase: " + e.getMessage());
                            e.printStackTrace();
                            saveToSharedPreferences();
                        });
            } else {
                saveToSharedPreferences();
            }
        }
    }

    private void setupWaterDataListener() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference waterRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today)
                .child("water");

        waterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer waterValue = snapshot.getValue(Integer.class);
                    if (waterValue != null && waterValue != dailyWaterIntake) {
                        dailyWaterIntake = waterValue;
                        updateUI();
                        Log.d(TAG, "Water intake updated from listener: " + dailyWaterIntake);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error listening to water data: " + error.getMessage());
            }
        });
    }

    private void saveEmergencyContact(String contact) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("emergency_contact", contact);
        editor.apply();
        Log.d(TAG, "Emergency contact saved to SharedPreferences: " + contact);
    }

    private void updateProfile(String name, String email, String phone, String emergencyContact) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to update profile", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save emergency contact to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("emergency_contact", emergencyContact);
        editor.apply();
        Log.d(TAG, "Emergency contact saved to SharedPreferences: " + emergencyContact);

        // Create profile data for Firebase (without emergency contact)
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("email", email);
        profileData.put("phone", phone);

        // Update profile in Firebase
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = databaseReference.child("patients").child(userId);
        userRef.child("profile").setValue(profileData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Profile updated successfully in Firebase");
                Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating profile in Firebase: " + e.getMessage());
                Toast.makeText(getContext(), "Profile saved locally, but could not sync with server", Toast.LENGTH_SHORT).show();
            });
    }

    private void showUpdateProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Profile");

        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_profile, null);
        builder.setView(dialogView);

        // Get references to the EditText fields
        TextInputEditText nameEditText = dialogView.findViewById(R.id.editTextName);
        TextInputEditText emailEditText = dialogView.findViewById(R.id.editTextEmail);
        TextInputEditText phoneEditText = dialogView.findViewById(R.id.editTextPhone);
        TextInputEditText emergencyContactEditText = dialogView.findViewById(R.id.editTextEmergencyContact);

        // Load current profile data
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference userRef = databaseReference.child("patients").child(userId).child("profile");

            userRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String emergencyContact = snapshot.child("emergency_contact").getValue(String.class);

                    if (name != null) nameEditText.setText(name);
                    if (email != null) emailEditText.setText(email);
                    if (phone != null) phoneEditText.setText(phone);
                    if (emergencyContact != null) emergencyContactEditText.setText(emergencyContact);
                }
            });
        }

        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
            String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
            String phone = phoneEditText.getText() != null ? phoneEditText.getText().toString().trim() : "";
            String emergencyContact = emergencyContactEditText.getText() != null ? emergencyContactEditText.getText().toString().trim() : "";

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || emergencyContact.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            updateProfile(name, email, phone, emergencyContact);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkSavedEmergencyContact() {
        String emergencyContact = sharedPreferences.getString("emergency_contact", null);
        if (emergencyContact != null && !emergencyContact.isEmpty()) {
            Log.d(TAG, "Found saved emergency contact: " + emergencyContact);
            Toast.makeText(getContext(), "Emergency contact saved: " + emergencyContact, Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "No emergency contact found in SharedPreferences");
            Toast.makeText(getContext(), "No emergency contact saved", Toast.LENGTH_LONG).show();
        }
    }
}