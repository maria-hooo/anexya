package com.anexya.rfid_service.dto;

import com.anexya.rfid_service.entity.RfidTag;

import java.time.Instant;

public record RfidTagResponse(
        Long id,
        String siteName,
        String epc,
        String tid,
        String location,
        Integer rssi,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static RfidTagResponse from(RfidTag tag) {
        return new RfidTagResponse(
                tag.getId(),
                tag.getSiteName(),
                tag.getEpc(),
                tag.getTid(),
                tag.getLocation(),
                tag.getRssi(),
                tag.getReadAt(),
                tag.getCreatedAt(),
                tag.getUpdatedAt());
    }
}
