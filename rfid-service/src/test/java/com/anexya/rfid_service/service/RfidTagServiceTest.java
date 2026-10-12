package com.anexya.rfid_service.service;

import com.anexya.rfid_service.dto.RfidTagPatchRequest;
import com.anexya.rfid_service.dto.RfidTagRequest;
import com.anexya.rfid_service.dto.RfidTagResponse;
import com.anexya.rfid_service.dto.TagSearchCriteria;
import com.anexya.rfid_service.entity.RfidTag;
import com.anexya.rfid_service.events.TagEventPublisher;
import com.anexya.rfid_service.exception.TagNotFoundException;
import com.anexya.rfid_service.repository.RfidTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RfidTagServiceTest {

    private static final Instant READ_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private RfidTagRepository repository;

    @Mock
    private TagEventPublisher eventPublisher;

    @InjectMocks
    private RfidTagService service;

    private static RfidTagRequest validRequest() {
        return new RfidTagRequest("  Beirut Warehouse ", "e2801170000002100f2a3b4c", "e2003412abcd0001",
                "Dock 3", -52, READ_AT);
    }

    private static RfidTag existingTag(long id) {
        RfidTag tag = new RfidTag();
        tag.setId(id);
        tag.setSiteName("Beirut Warehouse");
        tag.setEpc("E2801170000002100F2A3B4C");
        tag.setTid("E2003412ABCD0001");
        tag.setLocation("Dock 3");
        tag.setRssi(-52);
        tag.setReadAt(READ_AT);
        return tag;
    }


    @Test
    void create_savesAllFields_normalizesHexAndTrims_andPublishesEvent() {
        when(repository.save(any(RfidTag.class))).thenAnswer(inv -> {
            RfidTag t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        RfidTagResponse response = service.create(validRequest());

        ArgumentCaptor<RfidTag> saved = ArgumentCaptor.forClass(RfidTag.class);
        verify(repository).save(saved.capture());
        RfidTag t = saved.getValue();
        assertThat(t.getSiteName()).isEqualTo("Beirut Warehouse");
        assertThat(t.getEpc()).isEqualTo("E2801170000002100F2A3B4C");
        assertThat(t.getTid()).isEqualTo("E2003412ABCD0001");
        assertThat(t.getLocation()).isEqualTo("Dock 3");
        assertThat(t.getRssi()).isEqualTo(-52);
        assertThat(t.getReadAt()).isEqualTo(READ_AT);

        assertThat(response.id()).isEqualTo(1L);
        verify(eventPublisher).publishCreated(t);
    }

    @Test
    void create_acceptsBoundaryRssiValues() {
        when(repository.save(any(RfidTag.class))).thenAnswer(inv -> inv.getArgument(0));

        RfidTagRequest weakest = new RfidTagRequest("S", "AAAAAAAA", "BBBBBBBB", "L", -120, READ_AT);
        RfidTagRequest strongest = new RfidTagRequest("S", "AAAAAAAA", "BBBBBBBB", "L", 0, READ_AT);

        assertThat(service.create(weakest).rssi()).isEqualTo(-120);
        assertThat(service.create(strongest).rssi()).isZero();
    }


    @Test
    void getById_returnsTag_whenItExists() {
        when(repository.findById(7L)).thenReturn(Optional.of(existingTag(7L)));

        RfidTagResponse response = service.getById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.epc()).isEqualTo("E2801170000002100F2A3B4C");
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(TagNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void search_passesPageableToRepository_andMapsResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<RfidTag> page = new PageImpl<>(List.of(existingTag(1L), existingTag(2L)), pageable, 2);
        when(repository.findAll(ArgumentMatchers.<Specification<RfidTag>>any(), eq(pageable))).thenReturn(page);

        TagSearchCriteria criteria = new TagSearchCriteria("e2801170000002100f2a3b4c", null, null, null, null, null, null);
        Page<RfidTagResponse> result = service.search(criteria, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(RfidTagResponse::id).containsExactly(1L, 2L);
    }


    @Test
    void update_replacesEveryField() {
        RfidTag tag = existingTag(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(tag));
        when(repository.save(tag)).thenReturn(tag);

        Instant newTime = Instant.parse("2026-10-02T10:00:00Z");
        RfidTagRequest request = new RfidTagRequest("Tripoli Store", "abcdef12", "12345678", "Gate A", -80, newTime);

        RfidTagResponse response = service.update(5L, request);

        assertThat(response.siteName()).isEqualTo("Tripoli Store");
        assertThat(response.epc()).isEqualTo("ABCDEF12");
        assertThat(response.tid()).isEqualTo("12345678");
        assertThat(response.location()).isEqualTo("Gate A");
        assertThat(response.rssi()).isEqualTo(-80);
        assertThat(response.readAt()).isEqualTo(newTime);
    }

    @Test
    void patch_changesOnlyProvidedFields() {
        RfidTag tag = existingTag(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(tag));
        when(repository.save(tag)).thenReturn(tag);

        RfidTagPatchRequest request = new RfidTagPatchRequest(null, null, null, "Dock 4", -60, null);

        RfidTagResponse response = service.patch(5L, request);

        assertThat(response.location()).isEqualTo("Dock 4");
        assertThat(response.rssi()).isEqualTo(-60);
        assertThat(response.siteName()).isEqualTo("Beirut Warehouse");
        assertThat(response.epc()).isEqualTo("E2801170000002100F2A3B4C");
        assertThat(response.readAt()).isEqualTo(READ_AT);
    }

    @Test
    void update_throwsNotFound_andSavesNothing_whenMissing() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(42L, validRequest())).isInstanceOf(TagNotFoundException.class);
        verify(repository, never()).save(any());
    }


    @Test
    void delete_removesTag_whenItExists() {
        when(repository.existsById(3L)).thenReturn(true);

        service.delete(3L);

        verify(repository).deleteById(3L);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(repository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(TagNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
