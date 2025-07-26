package com.example.healthcare;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.healthcare.ui.realtime.RealTimeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.healthcare.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "MainActivity created");
            setupNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment fragment) {
        try {
            Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            Log.d(TAG, "Fragment loaded successfully: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupNavigation() {
        Log.d(TAG, "Setting up navigation");
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView == null) {
            Log.e(TAG, "Bottom navigation view not found!");
            return;
        }

        // Set default fragment if this is the first creation
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            Log.d(TAG, "No fragment in container, loading HomeFragment");
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_ai) {
                selectedFragment = new AiFragment();
            } else if (itemId == R.id.navigation_log) {
                selectedFragment = new LogFragment();
            } else if (itemId == R.id.navigation_realtime) {
                Log.d(TAG, "RealTimeFragment selected");
                selectedFragment = new RealTimeFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }
}
