package com.example.healthcare;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class LogFragment extends Fragment {
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private RecyclerView logRecyclerView;
    private TextView totalStepsText;
    private TextView totalWaterText;
    private TextView avgSleepText;
    private static final String STEPS_KEY = "steps";
    private static final String WATER_KEY = "water";
    private static final String SLEEP_START_KEY = "sleep_start";
    private static final String SLEEP_END_KEY = "sleep_end";
    private static final int DAYS_TO_KEEP = 30; // Keep 30 days of history
    
    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private int currentFilterType = 0; // 0: week, 1: month, 2: custom

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Initialize views
        logRecyclerView = view.findViewById(R.id.logRecyclerView);
        totalStepsText = view.findViewById(R.id.totalStepsText);
        totalWaterText = view.findViewById(R.id.totalWaterText);
        avgSleepText = view.findViewById(R.id.avgSleepText);
        ChipGroup dateFilterChips = view.findViewById(R.id.dateFilterChips);
        
        logRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Set up date filter chips
        Chip chipWeek = view.findViewById(R.id.chipWeek);
        Chip chipMonth = view.findViewById(R.id.chipMonth);
        Chip chipCustom = view.findViewById(R.id.chipCustom);
        
        chipWeek.setOnClickListener(v -> {
            currentFilterType = 0;
            setWeekRange();
            updateLogDisplay();
        });
        
        chipMonth.setOnClickListener(v -> {
            currentFilterType = 1;
            setMonthRange();
            updateLogDisplay();
        });
        
        chipCustom.setOnClickListener(v -> {
            currentFilterType = 2;
            showDateRangePicker();
        });
        
        // Initialize with this week's data
        setWeekRange();
        
        // Update display
        updateLogDisplay();
        return view;
    }

    private void setWeekRange() {
        endDate = Calendar.getInstance();
        startDate = (Calendar) endDate.clone();
        startDate.add(Calendar.DAY_OF_YEAR, -7);
    }

    private void setMonthRange() {
        endDate = Calendar.getInstance();
        startDate = (Calendar) endDate.clone();
        startDate.add(Calendar.MONTH, -1);
    }

    private void showDateRangePicker() {
        DatePickerDialog startDatePicker = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                startDate.set(year, month, dayOfMonth);
                
                DatePickerDialog endDatePicker = new DatePickerDialog(
                    requireContext(),
                    (view2, year2, month2, dayOfMonth2) -> {
                        endDate.set(year2, month2, dayOfMonth2);
                        if (endDate.before(startDate)) {
                            Toast.makeText(requireContext(), 
                                "End date cannot be before start date", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updateLogDisplay();
                    },
                    endDate.get(Calendar.YEAR),
                    endDate.get(Calendar.MONTH),
                    endDate.get(Calendar.DAY_OF_MONTH)
                );
                endDatePicker.show();
            },
            startDate.get(Calendar.YEAR),
            startDate.get(Calendar.MONTH),
            startDate.get(Calendar.DAY_OF_MONTH)
        );
        startDatePicker.show();
    }

    public void updateLogDisplay() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please login to view logs", Toast.LENGTH_SHORT).show();
            return;
        }

        final String userId = mAuth.getCurrentUser().getUid();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        final List<LogEntry> logEntries = new ArrayList<>();
        final int[] totalSteps = {0};
        final int[] totalWater = {0};
        final long[] totalSleepMinutes = {0};
        final int[] daysWithSleep = {0};
        final int[] processedDays = {0};
        final int totalDays = getDaysBetween(startDate, endDate);
        
        Calendar currentDate = (Calendar) startDate.clone();
        while (!currentDate.after(endDate)) {
            final String dateKey = dateFormat.format(currentDate.getTime());
            final String displayDate = displayFormat.format(currentDate.getTime());

            DatabaseReference dateRef = databaseReference.child("patients")
                .child(userId)
                .child("logs")
                .child(dateKey);

            dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int steps = 0;
                    int water = 0;
                    long sleepStart = 0;
                    long sleepEnd = 0;

                    if (snapshot.exists()) {
                        steps = snapshot.child(STEPS_KEY).getValue(Integer.class) != null ? 
                            snapshot.child(STEPS_KEY).getValue(Integer.class) : 0;
                        water = snapshot.child(WATER_KEY).getValue(Integer.class) != null ? 
                            snapshot.child(WATER_KEY).getValue(Integer.class) : 0;
                        sleepStart = snapshot.child(SLEEP_START_KEY).getValue(Long.class) != null ? 
                            snapshot.child(SLEEP_START_KEY).getValue(Long.class) : 0;
                        sleepEnd = snapshot.child(SLEEP_END_KEY).getValue(Long.class) != null ? 
                            snapshot.child(SLEEP_END_KEY).getValue(Long.class) : 0;
                    }

                    totalSteps[0] += steps;
                    totalWater[0] += water;

                    String sleepDuration = "No sleep data";
                    if (sleepStart > 0 && sleepEnd > 0) {
                        long durationMinutes = (sleepEnd - sleepStart) / (1000 * 60);
                        totalSleepMinutes[0] += durationMinutes;
                        daysWithSleep[0]++;
                        long hours = durationMinutes / 60;
                        long minutes = durationMinutes % 60;
                        sleepDuration = String.format("%d hrs %d min", hours, minutes);
                    }

                    LogEntry entry = new LogEntry(displayDate, steps, water, sleepDuration);
                    logEntries.add(entry);
                    processedDays[0]++;

                    if (processedDays[0] == totalDays) {
                        updateUI(logEntries, totalSteps[0], totalWater[0], 
                            totalSleepMinutes[0], daysWithSleep[0]);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(requireContext(), 
                        "Error loading logs: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
            currentDate.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void updateUI(List<LogEntry> entries, int totalSteps, int totalWater, 
                         long totalSleepMinutes, int daysWithSleep) {
        totalStepsText.setText(String.format(Locale.getDefault(), "%,d", totalSteps));
        totalWaterText.setText(String.format(Locale.getDefault(), "%.1fL", totalWater / 1000.0));
        
        if (daysWithSleep > 0) {
            long avgSleepMinutes = totalSleepMinutes / daysWithSleep;
            avgSleepText.setText(String.format(Locale.getDefault(), "%.1fh", avgSleepMinutes / 60.0));
        } else {
            avgSleepText.setText("0h");
        }

        LogAdapter adapter = new LogAdapter(entries);
        logRecyclerView.setAdapter(adapter);
    }

    private int getDaysBetween(Calendar start, Calendar end) {
        int days = 0;
        Calendar current = (Calendar) start.clone();
        while (!current.after(end)) {
            days++;
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private static class LogEntry {
        String date;
        int steps;
        int water;
        String sleep;

        LogEntry(String date, int steps, int water, String sleep) {
            this.date = date;
            this.steps = steps;
            this.water = water;
            this.sleep = sleep;
        }
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private final List<LogEntry> entries;

        LogAdapter(List<LogEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_item, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            LogEntry entry = entries.get(position);
            holder.dateText.setText(entry.date);
            holder.stepsText.setText(String.format("Steps: %d", entry.steps));
            holder.waterText.setText(String.format("Water: %d ml", entry.water));
            holder.sleepText.setText(String.format("Sleep: %s", entry.sleep));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            final TextView dateText;
            final TextView stepsText;
            final TextView waterText;
            final TextView sleepText;

            LogViewHolder(View view) {
                super(view);
                dateText = view.findViewById(R.id.dateText);
                stepsText = view.findViewById(R.id.stepsText);
                waterText = view.findViewById(R.id.waterText);
                sleepText = view.findViewById(R.id.sleepText);
            }
        }
    }
}
