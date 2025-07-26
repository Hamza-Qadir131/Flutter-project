package com.example.healthcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        MaterialButton btnPatient = findViewById(R.id.btnPatient);
        MaterialButton btnDoctor = findViewById(R.id.btnDoctor);

        // Show selection buttons after splash delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            buttonContainer.setVisibility(View.VISIBLE);
        }, SPLASH_DELAY);

        // Set click listeners for buttons
        btnPatient.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        });

        btnDoctor.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, DoctorLoginActivity.class));
            finish();
        });
    }
}
