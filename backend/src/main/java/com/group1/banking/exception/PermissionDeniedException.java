package com.group1.banking.exception;

public class PermissionDeniedException extends RuntimeException {
    private final String permission;
    public PermissionDeniedException(String permission) {
        super("Missing required permission: " + permission);
        this.permission = permission;
    }
    public String getPermission() { return permission; }
}
