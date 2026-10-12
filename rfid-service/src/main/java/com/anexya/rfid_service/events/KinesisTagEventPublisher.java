package com.anexya.rfid_service.events;

import com.anexya.rfid_service.entity.RfidTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "rfid.kinesis.enabled", havingValue = "true")
public class KinesisTagEventPublisher implements TagEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KinesisTagEventPublisher.class);

    private final KinesisClient kinesis;
    private final String streamName;

    public KinesisTagEventPublisher(@Value("${rfid.kinesis.stream-name}") String streamName) {
        this.kinesis = KinesisClient.create();
        this.streamName = streamName;
    }

    @Override
    public void publishCreated(RfidTag tag) {
        String json = toJson(tag);
        try {
            kinesis.putRecord(PutRecordRequest.builder()
                    .streamName(streamName)
                    .partitionKey(tag.getEpc())
                    .data(SdkBytes.fromString(json, StandardCharsets.UTF_8))
                    .build());
            log.info("Published tag id={} epc={} to Kinesis stream {}", tag.getId(), tag.getEpc(), streamName);
        } catch (RuntimeException ex) {
            log.error("Failed to publish tag id={} to Kinesis stream {}", tag.getId(), streamName, ex);
        }
    }

    private static String toJson(RfidTag t) {
        return "{"
                + "\"id\":" + t.getId() + ","
                + "\"siteName\":" + quote(t.getSiteName()) + ","
                + "\"epc\":" + quote(t.getEpc()) + ","
                + "\"tid\":" + quote(t.getTid()) + ","
                + "\"location\":" + quote(t.getLocation()) + ","
                + "\"rssi\":" + t.getRssi() + ","
                + "\"readAt\":" + quote(String.valueOf(t.getReadAt()))
                + "}";
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
