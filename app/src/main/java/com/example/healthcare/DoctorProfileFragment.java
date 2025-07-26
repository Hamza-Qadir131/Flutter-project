package com.example.healthcare;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DoctorProfileFragment extends Fragment {
    private static final String TAG = "DoctorProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final String PREF_NAME = "DoctorProfilePrefs";
    private static final String KEY_PROFILE_IMAGE = "profile_image_uri";
    
    private TextView tvDoctorName, tvDoctorEmail;
    private ImageButton btnEditName, btnChangePhoto;
    private MaterialButton btnLogout;
    private ShapeableImageView ivDoctorProfile;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private DoctorDatabaseHelper dbHelper;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;
    private Uri selectedImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_profile, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        dbHelper = new DoctorDatabaseHelper(requireContext());
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);

        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return view;
        }

        // Initialize views
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvDoctorEmail = view.findViewById(R.id.tvDoctorEmail);
        btnEditName = view.findViewById(R.id.btnEditName);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnLogout = view.findViewById(R.id.btnLogout);
        ivDoctorProfile = view.findViewById(R.id.ivDoctorProfile);

        // Set user email directly
        tvDoctorEmail.setText(currentUser.getEmail());

        // Load doctor name from SQLite
        String savedName = dbHelper.getDoctorName(currentUser.getUid());
        if (savedName != null && !savedName.isEmpty()) {
            tvDoctorName.setText("Dr. " + savedName);
        }

        // Load saved profile image
        loadProfileImage();

        // Change photo button click listener
        btnChangePhoto.setOnClickListener(v -> checkPermissionAndOpenImagePicker());

        // Edit button click listener
        btnEditName.setOnClickListener(v -> {
            // Create an AlertDialog with EditText for name input
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Name");

            // Create EditText for input
            final EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(tvDoctorName.getText().toString().replace("Dr. ", ""));
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Save", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    // Update the name in the database
                    dbHelper.saveDoctorName(currentUser.getUid(), newName);
                    // Update the UI
                    tvDoctorName.setText("Dr. " + newName);
                    Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        // Set up logout button
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void checkPermissionAndOpenImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot access images.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                ivDoctorProfile.setImageURI(selectedImageUri);
                saveProfileImage();
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), "Permission denied. Cannot access image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfileImage() {
        if (selectedImageUri != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_PROFILE_IMAGE, selectedImageUri.toString());
            editor.apply();
            Toast.makeText(requireContext(), "Profile image saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage() {
        String savedImageUri = sharedPreferences.getString(KEY_PROFILE_IMAGE, null);
        if (savedImageUri != null) {
            try {
                selectedImageUri = Uri.parse(savedImageUri);
                ivDoctorProfile.setImageURI(selectedImageUri);
            } catch (SecurityException e) {
                // If permission is not granted, show default image
                ivDoctorProfile.setImageResource(R.drawable.doctor_avatar);
            }
        } else {
            // Set default image if no saved image
            ivDoctorProfile.setImageResource(R.drawable.doctor_avatar);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
} 