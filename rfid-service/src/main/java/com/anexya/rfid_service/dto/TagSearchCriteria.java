package com.anexya.rfid_service.dto;

import java.time.Instant;

public record TagSearchCriteria(
        String epc,
        String tid,
        String siteName,
        String location,
        Instant from,
        Instant to,
        Integer minRssi
) {
}
