package com.anexya.rfid_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RfidTagRequest(
        @NotBlank @Size(max = 100)
        String siteName,

        @NotBlank
        @Pattern(regexp = "^[0-9A-Fa-f]{8,64}$", message = "must be 8 to 64 hexadecimal characters")
        String epc,

        @NotBlank
        @Pattern(regexp = "^[0-9A-Fa-f]{8,64}$", message = "must be 8 to 64 hexadecimal characters")
        String tid,

        @NotBlank @Size(max = 100)
        String location,

        @NotNull @Min(-120) @Max(0)
        Integer rssi,

        @NotNull @PastOrPresent
        Instant readAt
) {
}
