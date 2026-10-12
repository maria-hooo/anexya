package com.anexya.rfid_service.repository;

import com.anexya.rfid_service.dto.TagSearchCriteria;
import com.anexya.rfid_service.entity.RfidTag;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RfidTagSpecifications {

    private RfidTagSpecifications() {
    }

    public static Specification<RfidTag> matching(TagSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(c.epc())) {
                predicates.add(cb.equal(root.get("epc"), c.epc().trim().toUpperCase(Locale.ROOT)));
            }
            if (hasText(c.tid())) {
                predicates.add(cb.equal(root.get("tid"), c.tid().trim().toUpperCase(Locale.ROOT)));
            }
            if (hasText(c.siteName())) {
                predicates.add(cb.equal(root.get("siteName"), c.siteName().trim()));
            }
            if (hasText(c.location())) {
                predicates.add(cb.equal(root.get("location"), c.location().trim()));
            }
            if (c.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("readAt"), c.from()));
            }
            if (c.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("readAt"), c.to()));
            }
            if (c.minRssi() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("rssi"), c.minRssi()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
