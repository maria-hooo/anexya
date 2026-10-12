package com.anexya.rfid_service.service;

import com.anexya.rfid_service.dto.RfidTagPatchRequest;
import com.anexya.rfid_service.dto.RfidTagRequest;
import com.anexya.rfid_service.dto.RfidTagResponse;
import com.anexya.rfid_service.dto.TagSearchCriteria;
import com.anexya.rfid_service.entity.RfidTag;
import com.anexya.rfid_service.events.TagEventPublisher;
import com.anexya.rfid_service.exception.TagNotFoundException;
import com.anexya.rfid_service.repository.RfidTagRepository;
import com.anexya.rfid_service.repository.RfidTagSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class RfidTagService {

    private static final Logger log = LoggerFactory.getLogger(RfidTagService.class);

    private final RfidTagRepository repository;
    private final TagEventPublisher eventPublisher;

    public RfidTagService(RfidTagRepository repository, TagEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public RfidTagResponse create(RfidTagRequest request) {
        RfidTag tag = new RfidTag();
        applyAll(tag, request);
        RfidTag saved = repository.save(tag);
        log.info("Created RFID tag id={} epc={} tid={} site={} rssi={}",
                saved.getId(), saved.getEpc(), saved.getTid(), saved.getSiteName(), saved.getRssi());
        eventPublisher.publishCreated(saved);
        return RfidTagResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RfidTagResponse getById(Long id) {
        RfidTag tag = findOrThrow(id);
        log.info("Fetched RFID tag id={}", id);
        return RfidTagResponse.from(tag);
    }

    @Transactional(readOnly = true)
    public Page<RfidTagResponse> search(TagSearchCriteria criteria, Pageable pageable) {
        Page<RfidTag> page = repository.findAll(RfidTagSpecifications.matching(criteria), pageable);
        log.info("Search {} returned {} of {} entries", criteria, page.getNumberOfElements(), page.getTotalElements());
        return page.map(RfidTagResponse::from);
    }

    public RfidTagResponse update(Long id, RfidTagRequest request) {
        RfidTag tag = findOrThrow(id);
        applyAll(tag, request);
        RfidTag saved = repository.save(tag);
        log.info("Updated (full) RFID tag id={}", id);
        return RfidTagResponse.from(saved);
    }

    public RfidTagResponse patch(Long id, RfidTagPatchRequest request) {
        RfidTag tag = findOrThrow(id);
        if (request.siteName() != null) tag.setSiteName(request.siteName().trim());
        if (request.epc() != null) tag.setEpc(normalizeHex(request.epc()));
        if (request.tid() != null) tag.setTid(normalizeHex(request.tid()));
        if (request.location() != null) tag.setLocation(request.location().trim());
        if (request.rssi() != null) tag.setRssi(request.rssi());
        if (request.readAt() != null) tag.setReadAt(request.readAt());
        RfidTag saved = repository.save(tag);
        log.info("Updated (partial) RFID tag id={} with {}", id, request);
        return RfidTagResponse.from(saved);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new TagNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Deleted RFID tag id={}", id);
    }

    private RfidTag findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
    }

    private static void applyAll(RfidTag tag, RfidTagRequest r) {
        tag.setSiteName(r.siteName().trim());
        tag.setEpc(normalizeHex(r.epc()));
        tag.setTid(normalizeHex(r.tid()));
        tag.setLocation(r.location().trim());
        tag.setRssi(r.rssi());
        tag.setReadAt(r.readAt());
    }

    private static String normalizeHex(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
