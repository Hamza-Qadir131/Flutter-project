package com.example.healthcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView policyText = findViewById(R.id.policyText);
        MaterialButton btnSupport = findViewById(R.id.btnSupport);

        String privacyPolicy = "Permission Usage Information:\n\n" +

            "1. Location Services\n" +
            "• Used ONLY during emergencies when you press the panic button\n" +
            "• Helps emergency contacts locate you quickly when needed\n" +
            "• Location data is not stored or tracked at any other time\n" +
            "• Used solely for sending your current location to emergency contacts\n\n" +

            "2. SMS Permission\n" +
            "• Used exclusively for sending emergency alerts\n" +
            "• Only activated when you press the panic button\n" +
            "• Sends your location and alert message to emergency contacts\n" +
            "• No promotional or automated messages are ever sent\n\n" +

            "3. Activity Recognition\n" +
            "• Required for step counting functionality\n" +
            "• Tracks your daily physical activity\n" +
            "• Data is stored only on your device\n" +
            "• Used to help you meet your fitness goals\n\n" +

            "4. Notifications\n" +
            "• Used for important health reminders\n" +
            "• Sends bedtime and wake-up alerts\n" +
            "• Provides water intake reminders\n" +
            "• Notifies you of achieved fitness goals\n\n" +

            "Data Protection:\n\n" +
            "• All your health data is stored locally on your device\n" +
            "• Emergency contact information is stored securely\n" +
            "• No data is shared with third parties\n" +
            "• Location data is only used during emergencies\n" +
            "• Step count and activity data stays on your device\n\n" +

            "Your Privacy:\n" +
            "• We never collect or sell your personal information\n" +
            "• Your data remains under your control\n" +
            "• Emergency features only activate when you initiate them\n" +
            "• You can delete your data at any time\n\n" +

            "Last Updated: " + new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        policyText.setText(privacyPolicy);

        // Setup support button
        btnSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@healthcare.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Privacy Policy Question");
            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (android.content.ActivityNotFoundException e) {
                // Handle case where no email app is available
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle("No Email App Found")
                       .setMessage("Please contact us at support@healthcare.com")
                       .setPositiveButton("OK", null)
                       .show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 