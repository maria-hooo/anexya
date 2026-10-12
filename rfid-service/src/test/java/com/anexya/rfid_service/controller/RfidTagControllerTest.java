package com.anexya.rfid_service.controller;

import com.anexya.rfid_service.dto.RfidTagRequest;
import com.anexya.rfid_service.dto.RfidTagResponse;
import com.anexya.rfid_service.dto.TagSearchCriteria;
import com.anexya.rfid_service.exception.GlobalExceptionHandler;
import com.anexya.rfid_service.exception.TagNotFoundException;
import com.anexya.rfid_service.service.RfidTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RfidTagControllerTest {

    private static final Instant READ_AT = Instant.parse("2026-10-01T08:30:00Z");

    private static final String VALID_BODY = """
            {
              "siteName": "Beirut Warehouse",
              "epc": "E2801170000002100F2A3B4C",
              "tid": "E2003412ABCD0001",
              "location": "Dock 3",
              "rssi": -52,
              "readAt": "2026-10-01T08:30:00Z"
            }
            """;

    @Mock
    private RfidTagService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new RfidTagController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private static RfidTagResponse sampleResponse(long id) {
        return new RfidTagResponse(id, "Beirut Warehouse", "E2801170000002100F2A3B4C", "E2003412ABCD0001",
                "Dock 3", -52, READ_AT, READ_AT, READ_AT);
    }

    @Test
    void post_validBody_returns201WithLocation() throws Exception {
        when(service.create(any(RfidTagRequest.class))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/v1/rfid-tags").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/rfid-tags/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.epc").value("E2801170000002100F2A3B4C"))
                .andExpect(jsonPath("$.rssi").value(-52));
    }

    @Test
    void post_invalidFields_returns400WithFieldErrors_andNeverCallsService() throws Exception {
        String body = """
                {
                  "siteName": "",
                  "epc": "not-hex!",
                  "tid": "E2003412ABCD0001",
                  "location": "Dock 3",
                  "rssi": 15,
                  "readAt": "2026-10-01T08:30:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/rfid-tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.siteName").exists())
                .andExpect(jsonPath("$.errors.epc").exists())
                .andExpect(jsonPath("$.errors.rssi").exists());

        verifyNoInteractions(service);
    }

    @Test
    void post_rssiBelowMinimum_returns400() throws Exception {
        String body = VALID_BODY.replace("-52", "-121");

        mockMvc.perform(post("/api/v1/rfid-tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rssi").exists());
    }

    @Test
    void post_missingReadAt_returns400() throws Exception {
        String body = VALID_BODY.replace(",\n  \"readAt\": \"2026-10-01T08:30:00Z\"", "");

        mockMvc.perform(post("/api/v1/rfid-tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.readAt").exists());
    }

    @Test
    void post_malformedDate_returns400() throws Exception {
        String body = VALID_BODY.replace("2026-10-01T08:30:00Z", "yesterday");

        mockMvc.perform(post("/api/v1/rfid-tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_existingId_returns200() throws Exception {
        when(service.getById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/v1/rfid-tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("Beirut Warehouse"));
    }

    @Test
    void get_missingId_returns404ProblemDetail() throws Exception {
        when(service.getById(99L)).thenThrow(new TagNotFoundException(99L));

        mockMvc.perform(get("/api/v1/rfid-tags/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("RFID tag not found"));
    }

    @Test
    void search_passesQueryParamsToService() throws Exception {
        when(service.search(any(TagSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(1L))));

        mockMvc.perform(get("/api/v1/rfid-tags")
                        .param("epc", "E2801170000002100F2A3B4C")
                        .param("siteName", "Beirut Warehouse")
                        .param("from", "2026-10-01T00:00:00Z")
                        .param("minRssi", "-70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<TagSearchCriteria> captor = ArgumentCaptor.forClass(TagSearchCriteria.class);
        verify(service).search(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().epc()).isEqualTo("E2801170000002100F2A3B4C");
        assertThat(captor.getValue().siteName()).isEqualTo("Beirut Warehouse");
        assertThat(captor.getValue().from()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(captor.getValue().minRssi()).isEqualTo(-70);
        assertThat(captor.getValue().tid()).isNull();
    }

    @Test
    void put_validBody_returns200() throws Exception {
        when(service.update(any(), any(RfidTagRequest.class))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/v1/rfid-tags/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void patch_invalidRssi_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/rfid-tags/1").contentType(MediaType.APPLICATION_JSON).content("{\"rssi\": 5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rssi").exists());

        verifyNoInteractions(service);
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/rfid-tags/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void delete_missing_returns404() throws Exception {
        doThrow(new TagNotFoundException(2L)).when(service).delete(2L);

        mockMvc.perform(delete("/api/v1/rfid-tags/2"))
                .andExpect(status().isNotFound());
    }
}
