package com.example.healthcare;

public class PermissionItem {
    private String title;
    private String description;
    private String[] permissions;
    private int iconResId;
    private boolean isGranted;

    public PermissionItem(String title, String description, String[] permissions, int iconResId) {
        this.title = title;
        this.description = description;
        this.permissions = permissions;
        this.iconResId = iconResId;
        this.isGranted = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isGranted() {
        return isGranted;
    }

    public void setGranted(boolean granted) {
        isGranted = granted;
    }
} 