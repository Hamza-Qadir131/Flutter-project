package com.example.healthcare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PermissionsAdapter extends RecyclerView.Adapter<PermissionsAdapter.ViewHolder> {

    private final List<PermissionsActivity.PermissionItem> permissions;

    public PermissionsAdapter(List<PermissionsActivity.PermissionItem> permissions) {
        this.permissions = permissions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PermissionsActivity.PermissionItem item = permissions.get(position);
        holder.iconView.setImageResource(item.iconResId);
        holder.titleView.setText(item.title);
        holder.descriptionView.setText(item.description);
    }

    @Override
    public int getItemCount() {
        return permissions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleView;
        TextView descriptionView;

        ViewHolder(View view) {
            super(view);
            iconView = view.findViewById(R.id.permissionIcon);
            titleView = view.findViewById(R.id.permissionTitle);
            descriptionView = view.findViewById(R.id.permissionDescription);
        }
    }
} 