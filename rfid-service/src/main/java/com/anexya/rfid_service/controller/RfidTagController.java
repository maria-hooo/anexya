package com.anexya.rfid_service.controller;

import com.anexya.rfid_service.dto.PageResponse;
import com.anexya.rfid_service.dto.RfidTagPatchRequest;
import com.anexya.rfid_service.dto.RfidTagRequest;
import com.anexya.rfid_service.dto.RfidTagResponse;
import com.anexya.rfid_service.dto.TagSearchCriteria;
import com.anexya.rfid_service.service.RfidTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/rfid-tags")
@Tag(name = "RFID tags", description = "Create, read, update and delete RFID tag reads")
public class RfidTagController {

    private final RfidTagService service;

    public RfidTagController(RfidTagService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record a new RFID read")
    public ResponseEntity<RfidTagResponse> create(@Valid @RequestBody RfidTagRequest request) {
        RfidTagResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one RFID read by its id")
    public RfidTagResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    @Operation(summary = "Search RFID reads by EPC, TID, site, location, time range or minimum RSSI (all optional, paged)")
    public PageResponse<RfidTagResponse> search(
            @RequestParam(required = false) String epc,
            @RequestParam(required = false) String tid,
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer minRssi,
            @PageableDefault(size = 20, sort = "readAt", direction = Sort.Direction.DESC) Pageable pageable) {
        TagSearchCriteria criteria = new TagSearchCriteria(epc, tid, siteName, location, from, to, minRssi);
        return PageResponse.of(service.search(criteria, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace all fields of an RFID read")
    public RfidTagResponse update(@PathVariable Long id, @Valid @RequestBody RfidTagRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change only the fields you send")
    public RfidTagResponse patch(@PathVariable Long id, @Valid @RequestBody RfidTagPatchRequest request) {
        return service.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an RFID read")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
