package com.example.healthcare;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import androidx.annotation.NonNull;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static final String PREF_NOTIFICATIONS = "notifications_enabled";
    private static final String PREF_PROFILE_IMAGE = "profile_image";
    private static final int PERMISSION_REQUEST_CODE = 123;

    private CircleImageView imgProfilePic;
    private TextView tvName, tvEmail;
    private MaterialButton btnLogout;
    private FloatingActionButton btnChangePic;
    private LinearLayout btnEmergencyContacts;
    private LinearLayout btnPrivacyPolicy;
    private SwitchMaterial switchNotifications;
    private SharedPreferences sharedPreferences;

    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> galleryLauncher;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("HealthcareApp", Activity.MODE_PRIVATE);
        
        // Register gallery launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    try {
                        Uri selectedImage = result.getData().getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            requireActivity().getContentResolver(), 
                            selectedImage
                        );
                        saveProfileImage(bitmap);
                        imgProfilePic.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image: " + e.getMessage());
                        Toast.makeText(requireContext(), 
                            "Failed to load image", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        imgProfilePic = view.findViewById(R.id.imgProfilePic);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePic = view.findViewById(R.id.btnChangePic);
        btnEmergencyContacts = view.findViewById(R.id.btnEmergencyContacts);
        btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy);
        switchNotifications = view.findViewById(R.id.switchNotifications);

        // Setup click listeners
        btnLogout.setOnClickListener(v -> handleLogout());
        btnChangePic.setOnClickListener(v -> handleProfilePicChange());
        btnEmergencyContacts.setOnClickListener(v -> openEmergencyContacts());
        btnPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());

        // Add permissions button click listener
        LinearLayout btnAppPermissions = view.findViewById(R.id.btnAppPermissions);
        btnAppPermissions.setOnClickListener(v -> openAppPermissions());

        // Load user data
        loadUserData();
        loadProfileImage();
        loadNotificationPreference();

        return view;
    }

    private void initializeViews(View view) {
        try {
            imgProfilePic = view.findViewById(R.id.imgProfilePic);
            tvName = view.findViewById(R.id.tvName);
            tvEmail = view.findViewById(R.id.tvEmail);
            btnLogout = view.findViewById(R.id.btnLogout);
            btnChangePic = view.findViewById(R.id.btnChangePic);
            btnEmergencyContacts = view.findViewById(R.id.btnEmergencyContacts);
            btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy);
            switchNotifications = view.findViewById(R.id.switchNotifications);

            // Set initial notification switch state
            boolean notificationsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true);
            switchNotifications.setChecked(notificationsEnabled);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e;
        }
    }

    private void saveProfileImage(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PREF_PROFILE_IMAGE, encodedImage);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile image: " + e.getMessage());
        }
    }

    private void loadProfileImage() {
        try {
            String encodedImage = sharedPreferences.getString(PREF_PROFILE_IMAGE, null);
            if (encodedImage != null) {
                byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imgProfilePic.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile image: " + e.getMessage());
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvName.setText(currentUser.getDisplayName() != null ? 
                currentUser.getDisplayName() : "User");
            tvEmail.setText(currentUser.getEmail());
        }
    }

    private void loadNotificationPreference() {
        try {
            boolean notificationsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true);
            if (switchNotifications != null) {
                switchNotifications.setChecked(notificationsEnabled);
                
                // Set listener for changes
                switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(PREF_NOTIFICATIONS, isChecked);
                    editor.apply();
                    
                    Toast.makeText(requireContext(), 
                        isChecked ? "Notifications enabled" : "Notifications disabled", 
                        Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading notification preference: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        } catch (Exception e) {
            Log.e(TAG, "Error during logout: " + e.getMessage());
            Toast.makeText(requireContext(), 
                "Error logging out", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void handleProfilePicChange() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, 
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery: " + e.getMessage());
            Toast.makeText(requireContext(), 
                "Error opening gallery", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmergencyContacts() {
        try {
            Intent intent = new Intent(getActivity(), EmergencyContactsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening emergency contacts: " + e.getMessage());
            Toast.makeText(requireContext(), 
                "Error opening emergency contacts", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void openPrivacyPolicy() {
        try {
            Intent intent = new Intent(getActivity(), PrivacyPolicyActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening privacy policy: " + e.getMessage());
            Toast.makeText(requireContext(), 
                "Error opening privacy policy", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppPermissions() {
        try {
            Intent intent = new Intent(getActivity(), AppPermissionsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening app permissions: " + e.getMessage());
            Toast.makeText(requireContext(), 
                "Error opening app permissions", 
                Toast.LENGTH_SHORT).show();
        }
    }

}