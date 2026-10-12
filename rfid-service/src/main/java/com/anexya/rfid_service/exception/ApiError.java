package com.anexya.rfid_service.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String title,
        String detail,
        Map<String, String> errors,
        Instant timestamp
) {
    public static ApiError of(int status, String title, String detail) {
        return new ApiError(status, title, detail, Map.of(), Instant.now());
    }
}
