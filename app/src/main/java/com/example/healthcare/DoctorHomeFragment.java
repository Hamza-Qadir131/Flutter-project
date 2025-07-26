package com.example.healthcare;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class DoctorHomeFragment extends Fragment {
    private static final String TAG = "DoctorHomeFragment";
    private RecyclerView recyclerView;
    private PatientLogAdapter adapter;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private TextInputEditText etSearchPatient;
    private MaterialButton btnSearch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewPatientLogs);
        progressBar = view.findViewById(R.id.progressBar);
        etSearchPatient = view.findViewById(R.id.etSearchPatient);
        btnSearch = view.findViewById(R.id.btnSearch);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PatientLogAdapter();
        recyclerView.setAdapter(adapter);
        
        // Check and set doctor role
        checkAndSetRoles();
        
        // Setup search button
        btnSearch.setOnClickListener(v -> {
            String patientEmail = etSearchPatient.getText().toString().trim();
            if (!patientEmail.isEmpty()) {
                searchPatientLogs(patientEmail);
            } else {
                Toast.makeText(getContext(), "Please enter patient email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndSetRoles() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        String currentUserEmail = mAuth.getCurrentUser().getEmail();
        
        Log.d(TAG, "Checking roles for user: " + currentUserId);
        
        databaseReference.child("doctors").child(currentUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        // Create new doctor entry
                        Map<String, Object> doctorData = new HashMap<>();
                        doctorData.put("profile/email", currentUserEmail);
                        doctorData.put("profile/name", mAuth.getCurrentUser().getDisplayName());
                        
                        databaseReference.child("doctors").child(currentUserId)
                            .setValue(doctorData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Created new doctor user");
                                Toast.makeText(getContext(), "Doctor profile created successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error creating doctor user: " + e.getMessage());
                                Toast.makeText(getContext(), "Error creating doctor profile", Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        Log.d(TAG, "Doctor profile already exists");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking doctor status: " + error.getMessage());
                    Toast.makeText(getContext(), "Error checking doctor status", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void searchPatientLogs(String patientEmail) {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in");
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Starting database access check...");
        Log.d(TAG, "Current user ID: " + mAuth.getCurrentUser().getUid());
        Log.d(TAG, "Database reference path: " + databaseReference.toString());
        
        progressBar.setVisibility(View.VISIBLE);

        // First verify Firebase connection
        databaseReference.child(".info/connected")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    Log.d(TAG, "Firebase connection status: " + connected);
                    
                    if (connected) {
                        // If connected, proceed with data fetch
                        fetchPatientData();
                    } else {
                        Log.e(TAG, "Not connected to Firebase");
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Not connected to database. Please check your internet connection.", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking Firebase connection: " + error.getMessage());
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Error connecting to database: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
    }

    private void fetchPatientData() {
        Log.d(TAG, "Attempting to fetch patient data...");
        
        databaseReference.child("patients")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "onDataChange called");
                    List<PatientLog> logs = new ArrayList<>();
                    
                    if (snapshot.exists()) {
                        Log.d(TAG, "Successfully accessed patients node");
                        Log.d(TAG, "Number of patients found: " + snapshot.getChildrenCount());
                        
                        // Iterate through all patients
                        for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                            String patientId = patientSnapshot.getKey();
                            Log.d(TAG, "Processing patient ID: " + patientId);
                            
                            String patientName = patientSnapshot.child("profile/name").getValue(String.class);
                            String patientEmail = patientSnapshot.child("profile/email").getValue(String.class);
                            
                            Log.d(TAG, "Found patient: " + patientName + " with ID: " + patientId + " and email: " + patientEmail);
                            
                            DataSnapshot logsSnapshot = patientSnapshot.child("logs");
                            if (logsSnapshot.exists()) {
                                Log.d(TAG, "Found logs for patient: " + patientName);
                                Log.d(TAG, "Number of log entries: " + logsSnapshot.getChildrenCount());
                                
                                for (DataSnapshot dateSnapshot : logsSnapshot.getChildren()) {
                                    String date = dateSnapshot.getKey();
                                    Log.d(TAG, "Processing log for date: " + date);
                                    
                                    // Get health data
                                    Integer steps = dateSnapshot.child("steps").getValue(Integer.class);
                                    Integer water = dateSnapshot.child("water").getValue(Integer.class);
                                    Long sleepStart = dateSnapshot.child("sleep_start").getValue(Long.class);
                                    Long sleepEnd = dateSnapshot.child("sleep_end").getValue(Long.class);
                                    
                                    Log.d(TAG, "Log data - Steps: " + steps + ", Water: " + water + 
                                          ", Sleep Start: " + sleepStart + ", Sleep End: " + sleepEnd);
                                    
                                    // Calculate sleep duration
                                    String sleepDuration = "No sleep data";
                                    if (sleepStart != null && sleepEnd != null && sleepStart > 0 && sleepEnd > 0) {
                                        long durationMinutes = (sleepEnd - sleepStart) / (1000 * 60);
                                        long hours = durationMinutes / 60;
                                        long minutes = durationMinutes % 60;
                                        sleepDuration = String.format("%d hrs %d min", hours, minutes);
                                    }
                                    
                                    // Construct patient display name
                                    String displayPatientInfo;
                                    if (patientName != null && !patientName.isEmpty()) {
                                        if (patientEmail != null && !patientEmail.isEmpty()) {
                                            displayPatientInfo = patientName + " (" + patientEmail + ")";
                                        } else {
                                            displayPatientInfo = patientName;
                                        }
                                    } else if (patientEmail != null && !patientEmail.isEmpty()) {
                                        displayPatientInfo = patientEmail;
                                    } else {
                                        displayPatientInfo = "";
                                    }
                                    
                                    // Create log entry
                                    PatientLog log = new PatientLog(
                                        formatDate(date),
                                        displayPatientInfo,
                                        steps != null ? steps : 0,
                                        water != null ? water : 0,
                                        sleepDuration
                                    );
                                    logs.add(log);
                                }
                            } else {
                                Log.d(TAG, "No logs found for patient: " + patientName);
                            }
                        }
                        
                        // Sort logs by date (newest first)
                        logs.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                        
                        // Update UI
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                adapter.setLogs(logs);
                                progressBar.setVisibility(View.GONE);
                                if (logs.isEmpty()) {
                                    Log.d(TAG, "No logs found in database");
                                    Toast.makeText(getContext(), "No logs found in database", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d(TAG, "Successfully loaded " + logs.size() + " log entries");
                                    Toast.makeText(getContext(), "Found " + logs.size() + " log entries", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Log.d(TAG, "No patients found in database");
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                adapter.setLogs(new ArrayList<>());
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "No patients found in database", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error accessing patients: " + error.getMessage());
                    Log.e(TAG, "Error code: " + error.getCode());
                    Log.e(TAG, "Error details: " + error.getDetails());
                    
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            String errorMessage = "Error accessing database: " + error.getMessage();
                            if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                                errorMessage = "Permission denied. Please check database rules.";
                            }
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }
} 