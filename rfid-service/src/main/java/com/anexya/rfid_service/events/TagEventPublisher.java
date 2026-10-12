package com.anexya.rfid_service.events;

import com.anexya.rfid_service.entity.RfidTag;

public interface TagEventPublisher {

    void publishCreated(RfidTag tag);
}
