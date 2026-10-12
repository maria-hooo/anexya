package com.anexya.rfid_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RfidTagPatchRequest(
        @Size(min = 1, max = 100)
        String siteName,

        @Pattern(regexp = "^[0-9A-Fa-f]{8,64}$", message = "must be 8 to 64 hexadecimal characters")
        String epc,

        @Pattern(regexp = "^[0-9A-Fa-f]{8,64}$", message = "must be 8 to 64 hexadecimal characters")
        String tid,

        @Size(min = 1, max = 100)
        String location,

        @Min(-120) @Max(0)
        Integer rssi,

        @PastOrPresent
        Instant readAt
) {
}
