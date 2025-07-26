package com.example.healthcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PermissionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaterialButton btnOpenSettings;
    private MaterialButton btnPrivacyPolicy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("App Permissions");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        btnOpenSettings = findViewById(R.id.btnOpenSettings);
        btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<PermissionItem> permissions = getPermissionsList();
        PermissionsAdapter adapter = new PermissionsAdapter(permissions);
        recyclerView.setAdapter(adapter);

        // Setup button clicks
        btnOpenSettings.setOnClickListener(v -> openAppSettings());
        btnPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
    }

    private List<PermissionItem> getPermissionsList() {
        List<PermissionItem> permissions = new ArrayList<>();
        
        permissions.add(new PermissionItem(
            "Location Access",
            "Required for emergency features. Only used when panic button is pressed. Helps emergency contacts locate you.",
            R.drawable.ic_location,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ));

        permissions.add(new PermissionItem(
            "SMS Permission",
            "Sends emergency alerts to your contacts. Only used during emergencies.",
            R.drawable.ic_sms,
            android.Manifest.permission.SEND_SMS
        ));

        permissions.add(new PermissionItem(
            "Contacts",
            "Stores emergency contact information. Managed locally on your device.",
            R.drawable.ic_contacts,
            android.Manifest.permission.READ_CONTACTS
        ));

        permissions.add(new PermissionItem(
            "Activity Recognition",
            "Required for step counting. Tracks your daily activity.",
            R.drawable.ic_directions_walk,
            "android.permission.ACTIVITY_RECOGNITION"
        ));

        permissions.add(new PermissionItem(
            "Camera",
            "Used for profile picture. Only accessed when you choose to update your photo.",
            R.drawable.ic_camera,
            android.Manifest.permission.CAMERA
        ));

        return permissions;
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open app settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPrivacyPolicy() {
        Intent intent = new Intent(this, PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Permission item data class
    public static class PermissionItem {
        String title;
        String description;
        int iconResId;
        String permission;

        public PermissionItem(String title, String description, int iconResId, String permission) {
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
            this.permission = permission;
        }
    }
} 