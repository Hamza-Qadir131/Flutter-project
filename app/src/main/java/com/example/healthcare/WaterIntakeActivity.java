package com.example.healthcare;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import android.widget.ProgressBar;

public class WaterIntakeActivity extends AppCompatActivity {

    private static final String TAG = "WaterIntakeActivity";
    private static final int WATER_GOAL = 2000; // Default water goal in ml
    private static final String PREF_WATER_GOAL = "water_goal";
    private static final String PREF_WATER_KEY = "daily_water";  // Match LogFragment key

    private TextView waterIntakeTextView;
    private ProgressBar progressBar;
    private EditText goalInput;
    private int dailyWaterIntake = 0;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_intake);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        // Initialize views
        waterIntakeTextView = findViewById(R.id.waterIntakeTextView);
        progressBar = findViewById(R.id.waterProgressBar);
        goalInput = findViewById(R.id.goalInput);
        MaterialButton setGoalButton = findViewById(R.id.setWaterGoalButton);
        MaterialButton addSmallButton = findViewById(R.id.addSmallButton);
        MaterialButton addMediumButton = findViewById(R.id.addMediumButton);
        MaterialButton addLargeButton = findViewById(R.id.addLargeButton);
        MaterialButton removeButton = findViewById(R.id.removeWaterButton);
        
        // Set up buttons
        findViewById(R.id.add100Button).setOnClickListener(v -> addWater(100));
        findViewById(R.id.add200Button).setOnClickListener(v -> addWater(200));
        findViewById(R.id.add300Button).setOnClickListener(v -> addWater(300));
        findViewById(R.id.addCustomButton).setOnClickListener(v -> {
            try {
                String amountStr = goalInput.getText().toString();
                if (!amountStr.isEmpty()) {
                    int amount = Integer.parseInt(amountStr);
                    if (amount > 0) {
                        addWater(amount);
                        goalInput.setText("");
                    } else {
                        Toast.makeText(this, "Please enter a positive amount", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        // Load initial data
        loadWaterData();

        if (setGoalButton != null) {
            setGoalButton.setOnClickListener(v -> setGoal());
        }

        // Add different amounts of water
        if (addSmallButton != null) {
            addSmallButton.setOnClickListener(v -> addWater(200));  // Small glass
        }
        if (addMediumButton != null) {
            addMediumButton.setOnClickListener(v -> addWater(300)); // Medium glass
        }
        if (addLargeButton != null) {
            addLargeButton.setOnClickListener(v -> addWater(500));  // Large glass/bottle
        }
        
        // Remove last water intake
        if (removeButton != null) {
            removeButton.setOnClickListener(v -> {
                if (dailyWaterIntake >= 200) {
                    addWater(-200);
                } else {
                    Toast.makeText(this, "Not enough water to remove", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Load water data when activity starts
        loadWaterData();
    }

    private void loadWaterData() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to view water intake", Toast.LENGTH_SHORT).show();
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
                        Integer water = snapshot.child("water").getValue(Integer.class);
                        if (water != null) {
                            dailyWaterIntake = water;
                            updateUI();
                            Log.d(TAG, "Loaded water intake: " + dailyWaterIntake);
                        }
                    } else {
                        // Initialize water data if it doesn't exist
                        Map<String, Object> initialData = new HashMap<>();
                        initialData.put("water", 0);
                        initialData.put("water_goal", WATER_GOAL);
                        initialData.put("water_goal_reached", false);
                        
                        todayRef.setValue(initialData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Initialized water data");
                                dailyWaterIntake = 0;
                                updateUI();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error initializing water data: " + e.getMessage());
                            });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing water data: " + e.getMessage());
                    Toast.makeText(WaterIntakeActivity.this,
                        "Error processing water data",
                        Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error reading water data: " + e.getMessage());
                Toast.makeText(WaterIntakeActivity.this,
                    "Error reading water data: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadWaterData: " + e.getMessage());
            Toast.makeText(this, "Error loading water data", Toast.LENGTH_SHORT).show();
        }
    }

    private void addWater(int amount) {
        try {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to track water intake", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();
            dailyWaterIntake += amount;
            
            DatabaseReference todayRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(today);

            Map<String, Object> updates = new HashMap<>();
            updates.put("water", dailyWaterIntake);
            updates.put("water_goal", WATER_GOAL);
            updates.put("water_goal_reached", dailyWaterIntake >= WATER_GOAL);

            todayRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Water intake updated successfully: " + dailyWaterIntake);
                    updateUI();
                    Toast.makeText(WaterIntakeActivity.this,
                        "Added " + amount + "ml of water",
                        Toast.LENGTH_SHORT).show();
                    
                    // Set result and finish activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("new_intake", dailyWaterIntake);
                    setResult(RESULT_OK, resultIntent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating water intake: " + e.getMessage());
                    Toast.makeText(WaterIntakeActivity.this,
                        "Error updating water intake: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in addWater: " + e.getMessage());
            Toast.makeText(this, "Error adding water", Toast.LENGTH_SHORT).show();
        }
    }

    private void setGoal() {
        if (mAuth.getCurrentUser() == null) return;

        String goalStr = goalInput.getText().toString();
        if (goalStr.isEmpty()) {
            Toast.makeText(this, "Please enter a goal", Toast.LENGTH_SHORT).show();
            return;
        }

        int goal = Integer.parseInt(goalStr);
        String userId = mAuth.getCurrentUser().getUid();
        
        DatabaseReference todayRef = databaseReference.child("users")
            .child(userId)
            .child("logs")
            .child(today);

        Map<String, Object> updates = new HashMap<>();
        updates.put("water_goal", goal);

        todayRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                updateUI(goal);
                saveWaterIntake();
                Toast.makeText(this, "Goal updated", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this, 
                "Error setting goal: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show());
    }

    private void saveWaterIntake() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference todayRef = databaseReference.child("users")
            .child(userId)
            .child("logs")
            .child(today);

        Map<String, Object> updates = new HashMap<>();
        updates.put(PREF_WATER_KEY, dailyWaterIntake);

        todayRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                // Successfully saved water intake
            })
            .addOnFailureListener(e -> Toast.makeText(this, 
                "Error saving water intake: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show());
    }

    private void updateUI(int goal) {
        try {
            if (waterIntakeTextView != null && goalInput != null) {
                waterIntakeTextView.setText(String.format(Locale.getDefault(), 
                    "Current Intake: %dml / %dml", dailyWaterIntake, goal));
                goalInput.setText(String.valueOf(goal));
            }
        } catch (Exception e) {
            Log.e("WaterIntakeActivity", "Error in updateUI: " + e.getMessage());
        }
    }

    private void updateUI() {
        if (waterIntakeTextView != null) {
            waterIntakeTextView.setText(String.format(Locale.getDefault(), 
                "Water Intake: %dml / %dml", dailyWaterIntake, WATER_GOAL));
        }
        if (progressBar != null) {
            int progress = (int) ((float) dailyWaterIntake / WATER_GOAL * 100);
            progressBar.setProgress(progress);
        }
    }
} 