package com.anexya.rfid_service.events;

import com.anexya.rfid_service.entity.RfidTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rfid.kinesis.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingTagEventPublisher implements TagEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingTagEventPublisher.class);

    @Override
    public void publishCreated(RfidTag tag) {
        log.debug("Streaming disabled; not publishing tag id={}", tag.getId());
    }
}
