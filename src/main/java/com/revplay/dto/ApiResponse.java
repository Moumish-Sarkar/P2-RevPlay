package com.revplay.dto;

/**
 * A standard Data Transfer Object (DTO) used for all API responses.
 * Instead of returning raw data or strings, every controller wraps its return
 * value
 * in this ApiResponse. This ensures the frontend always receives JSON in a
 * predictable format:
 * { "success": true/false, "message": "...", "data": {...} }
 */
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
