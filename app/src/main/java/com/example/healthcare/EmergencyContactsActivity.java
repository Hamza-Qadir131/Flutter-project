package com.example.healthcare;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashSet;
import java.util.Set;

public class EmergencyContactsActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyContacts";
    private LinearLayout contactsContainer;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        initializeViews();
        loadEmergencyContacts();
    }

    private void initializeViews() {
        contactsContainer = findViewById(R.id.contactsContainer);
        FloatingActionButton addContactButton = findViewById(R.id.addContactButton);
        sharedPreferences = getSharedPreferences("HealthcareApp", Context.MODE_PRIVATE);

        addContactButton.setOnClickListener(v -> showAddContactDialog());
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);

        TextInputEditText nameInput = dialogView.findViewById(R.id.contactNameInput);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.contactPhoneInput);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        MaterialButton addButton = dialogView.findViewById(R.id.addButton);

        AlertDialog dialog = builder.setView(dialogView).create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        addButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            
            if (validateContact(name, phone)) {
                addEmergencyContact(name, phone);
                loadEmergencyContacts();
                dialog.dismiss();
                Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private boolean validateContact(String name, String phone) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter contact name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Basic phone number validation (10 digits)
        if (!phone.matches("^[0-9]{10}$")) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void addEmergencyContact(String name, String phone) {
        Set<String> contacts = new HashSet<>(getEmergencyContacts());
        String contactData = name + "|" + phone;
        contacts.add(contactData);
        Log.d(TAG, "Saving contact: " + contactData);

        // Use the same SharedPreferences instance
        SharedPreferences prefs = getSharedPreferences("HealthcareApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("emergency_contacts", contacts);
        editor.commit(); // Use commit() for immediate save
        
        // Verify the save
        Set<String> savedContacts = prefs.getStringSet("emergency_contacts", new HashSet<>());
        Log.d(TAG, "Verified saved contacts: " + savedContacts);
    }

    private void removeEmergencyContact(String contactData) {
        Set<String> contacts = new HashSet<>(getEmergencyContacts());
        contacts.remove(contactData);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("emergency_contacts", contacts);
        editor.apply();

        loadEmergencyContacts();
        Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
    }

    private Set<String> getEmergencyContacts() {
        SharedPreferences prefs = getSharedPreferences("HealthcareApp", Context.MODE_PRIVATE);
        Set<String> contacts = prefs.getStringSet("emergency_contacts", new HashSet<>());
        Log.d(TAG, "Getting emergency contacts: " + contacts);
        return contacts != null ? contacts : new HashSet<>();
    }

    private void loadEmergencyContacts() {
        contactsContainer.removeAllViews();
        Set<String> contacts = getEmergencyContacts();
        Log.d(TAG, "Loaded contacts: " + contacts);

        if (contacts.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No emergency contacts added yet.\nAdd contacts using the + button below.");
            emptyText.setTextSize(16);
            emptyText.setPadding(16, 32, 16, 32);
            emptyText.setGravity(View.TEXT_ALIGNMENT_CENTER);
            contactsContainer.addView(emptyText);
            return;
        }

        for (String contactData : contacts) {
            String[] parts = contactData.split("\\|");
            if (parts.length == 2) {
                addContactCard(parts[0], parts[1], contactData);
            } else {
                Log.e(TAG, "Invalid contact data format: " + contactData);
            }
        }
    }

    private void addContactCard(String name, String phone, String contactData) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.contact_card_item, null);

        TextView nameText = cardView.findViewById(R.id.contactName);
        TextView phoneText = cardView.findViewById(R.id.contactPhone);
        ImageButton callButton = cardView.findViewById(R.id.callButton);
        ImageButton messageButton = cardView.findViewById(R.id.messageButton);
        ImageButton deleteButton = cardView.findViewById(R.id.deleteContactButton);

        nameText.setText(name);
        phoneText.setText(phone);

        // Call button click handler
        callButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                    == PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + phone));
                startActivity(callIntent);
            } else {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + phone));
                startActivity(dialIntent);
            }
        });

        // Message button click handler
        messageButton.setOnClickListener(v -> {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            smsIntent.setData(Uri.parse("sms:" + phone));
            startActivity(smsIntent);
        });

        // Delete button click handler
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Remove Contact")
                    .setMessage("Are you sure you want to remove " + name + " from emergency contacts?")
                    .setPositiveButton("Remove", (dialog, which) -> removeEmergencyContact(contactData))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        contactsContainer.addView(cardView);
    }
}