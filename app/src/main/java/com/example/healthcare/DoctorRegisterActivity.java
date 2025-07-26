package com.example.healthcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DoctorRegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword, etSpecialization, etLicenseNumber;
    private Button btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_register);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etDoctorName);
        etEmail = findViewById(R.id.etDoctorEmail);
        etPassword = findViewById(R.id.etDoctorPassword);
        etConfirmPassword = findViewById(R.id.etDoctorConfirmPassword);
        etSpecialization = findViewById(R.id.etDoctorSpecialization);
        etLicenseNumber = findViewById(R.id.etDoctorLicenseNumber);
        btnRegister = findViewById(R.id.btnDoctorRegister);
        progressBar = findViewById(R.id.progressBar);
        TextView tvLoginInstead = findViewById(R.id.tvDoctorLoginInstead);

        btnRegister.setOnClickListener(v -> registerDoctor());
        tvLoginInstead.setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorLoginActivity.class));
            finish();
        });
    }

    private void registerDoctor() {
        String name = Objects.requireNonNull(etName.getText()).toString().trim();
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString().trim();
        String specialization = Objects.requireNonNull(etSpecialization.getText()).toString().trim();
        String licenseNumber = Objects.requireNonNull(etLicenseNumber.getText()).toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || 
            specialization.isEmpty() || licenseNumber.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Set doctor role and name in Firebase
                        String userId = mAuth.getCurrentUser().getUid();
                        DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference()
                            .child("doctors")
                            .child(userId);

                        Map<String, Object> doctorData = new HashMap<>();
                        doctorData.put("name", name);
                        doctorData.put("email", email);
                        doctorData.put("specialization", specialization);
                        doctorData.put("licenseNumber", licenseNumber);
                        doctorData.put("registrationDate", System.currentTimeMillis());

                        doctorRef.setValue(doctorData)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(DoctorRegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(DoctorRegisterActivity.this, DoctorMainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(DoctorRegisterActivity.this, "Error saving doctor data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(DoctorRegisterActivity.this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
} 