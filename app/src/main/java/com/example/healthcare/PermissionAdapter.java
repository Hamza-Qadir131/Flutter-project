package com.example.healthcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {

    private Context context;
    private List<PermissionItem> permissions;
    private PermissionCallback callback;

    public interface PermissionCallback {
        void onPermissionToggled(PermissionItem permission, boolean isChecked);
    }

    public PermissionAdapter(Context context, List<PermissionItem> permissions, PermissionCallback callback) {
        this.context = context;
        this.permissions = permissions;
        this.callback = callback;
    }

    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_permission, parent, false);
        return new PermissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        PermissionItem permission = permissions.get(position);
        
        holder.iconView.setImageResource(permission.getIconResId());
        holder.titleText.setText(permission.getTitle());
        holder.descriptionText.setText(permission.getDescription());
        holder.permissionSwitch.setChecked(permission.isGranted());

        holder.permissionSwitch.setOnClickListener(v -> {
            callback.onPermissionToggled(permission, holder.permissionSwitch.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return permissions.size();
    }

    static class PermissionViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleText;
        TextView descriptionText;
        Switch permissionSwitch;

        public PermissionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.permissionIcon);
            titleText = itemView.findViewById(R.id.permissionTitle);
            descriptionText = itemView.findViewById(R.id.permissionDescription);
            permissionSwitch = itemView.findViewById(R.id.permissionSwitch);
        }
    }
} 