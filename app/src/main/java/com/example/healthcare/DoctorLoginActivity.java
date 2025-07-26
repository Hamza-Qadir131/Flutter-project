package com.example.healthcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DoctorLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_login);

        mAuth = FirebaseAuth.getInstance();

        // Check if already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, DoctorMainActivity.class));
            finish();
            return;
        }

        // Bind views
        etEmail = findViewById(R.id.etDoctorEmail);
        etPassword = findViewById(R.id.etDoctorPassword);
        btnLogin = findViewById(R.id.btnDoctorLogin);
        TextView tvCreateDoctorAccount = findViewById(R.id.tvCreateDoctorAccount);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginDoctor());

        // Go to DoctorRegisterActivity
        tvCreateDoctorAccount.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorLoginActivity.this, DoctorRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginDoctor() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Verify if user is a doctor
                        String userId = mAuth.getCurrentUser().getUid();
                        DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference()
                            .child("doctors")
                            .child(userId);

                        doctorRef.get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    progressBar.setVisibility(View.GONE);
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(DoctorLoginActivity.this, "Doctor login successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(DoctorLoginActivity.this, DoctorMainActivity.class));
                                    finish();
                                } else {
                                    // User exists but is not a doctor
                                    mAuth.signOut();
                                    progressBar.setVisibility(View.GONE);
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(DoctorLoginActivity.this, "Access denied. This account is not registered as a doctor.", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                Toast.makeText(DoctorLoginActivity.this, "Error verifying doctor account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(DoctorLoginActivity.this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
} 