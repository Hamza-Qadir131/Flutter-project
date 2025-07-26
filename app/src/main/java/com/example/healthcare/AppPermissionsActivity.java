package com.example.healthcare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class AppPermissionsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.SEND_SMS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_permissions);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("App Permissions");

        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize permission switches
        setupPermissionSwitch(R.id.switchCamera, Manifest.permission.CAMERA, "Camera");
        setupPermissionSwitch(R.id.switchStorage, Manifest.permission.READ_EXTERNAL_STORAGE, "Storage");
        setupPermissionSwitch(R.id.switchLocation, Manifest.permission.ACCESS_FINE_LOCATION, "Location");
        setupPermissionSwitch(R.id.switchActivity, Manifest.permission.ACTIVITY_RECOGNITION, "Physical Activity");
        setupPermissionSwitch(R.id.switchSMS, Manifest.permission.SEND_SMS, "SMS");
    }

    private void setupPermissionSwitch(int switchId, String permission, String permissionName) {
        SwitchMaterial permissionSwitch = findViewById(switchId);
        boolean isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        permissionSwitch.setChecked(isGranted);

        permissionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isGranted) {
                requestPermission(permission);
            } else if (!isChecked && isGranted) {
                // Open app settings since permissions can't be revoked programmatically
                openAppSettings();
                Toast.makeText(this, 
                    "Please disable " + permissionName + " permission manually", 
                    Toast.LENGTH_LONG).show();
                // Reset switch state since we can't programmatically revoke permission
                permissionSwitch.setChecked(true);
            }
        });
    }

    private void requestPermission(String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // Show explanation why the permission is needed
            String message = getPermissionRationale(permission);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
    }

    private String getPermissionRationale(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "Camera permission is needed to take profile pictures";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Storage permission is needed to save health records and images";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Location permission is needed for emergency location sharing";
            case Manifest.permission.ACTIVITY_RECOGNITION:
                return "Physical Activity permission is needed to track your steps";
            case Manifest.permission.SEND_SMS:
                return "SMS permission is needed to send emergency messages";
            default:
                return "This permission is required for the app to function properly";
        }
    }

    private void openAppSettings() {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                boolean isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                
                // Find the corresponding switch and update its state
                int switchId = getSwitchIdForPermission(permission);
                
                if (switchId != -1) {
                    SwitchMaterial permissionSwitch = findViewById(switchId);
                    permissionSwitch.setChecked(isGranted);
                }

                if (!isGranted) {
                    Toast.makeText(this, 
                        "Permission denied. Some features may not work properly.", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private int getSwitchIdForPermission(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return R.id.switchCamera;
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return R.id.switchStorage;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return R.id.switchLocation;
            case Manifest.permission.ACTIVITY_RECOGNITION:
                return R.id.switchActivity;
            case Manifest.permission.SEND_SMS:
                return R.id.switchSMS;
            default:
                return -1;
        }
    }
} 