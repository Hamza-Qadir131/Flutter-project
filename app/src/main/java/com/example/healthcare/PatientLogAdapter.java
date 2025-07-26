package com.example.healthcare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PatientLogAdapter extends RecyclerView.Adapter<PatientLogAdapter.PatientLogViewHolder> {
    private List<PatientLog> patientLogs = new ArrayList<>();

    @NonNull
    @Override
    public PatientLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_log, parent, false);
        return new PatientLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientLogViewHolder holder, int position) {
        PatientLog log = patientLogs.get(position);
        holder.tvPatientName.setText(log.getPatientName());
        holder.tvDate.setText(log.getDate());
        holder.tvSteps.setText(String.format("%,d steps", log.getSteps()));
        holder.tvWaterIntake.setText(String.format("%,d ml", log.getWaterIntake()));
        holder.tvSleepDuration.setText(log.getSleepDuration());
    }

    @Override
    public int getItemCount() {
        return patientLogs.size();
    }

    public void setLogs(List<PatientLog> logs) {
        this.patientLogs = logs;
        notifyDataSetChanged();
    }

    static class PatientLogViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvDate, tvSteps, tvWaterIntake, tvSleepDuration;

        PatientLogViewHolder(View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSteps = itemView.findViewById(R.id.tvSteps);
            tvWaterIntake = itemView.findViewById(R.id.tvWaterIntake);
            tvSleepDuration = itemView.findViewById(R.id.tvSleepDuration);
        }
    }
} 