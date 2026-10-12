package com.anexya.rfid_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RfidTagApiIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void fullCrudLifecycle() throws Exception {
        String base = "http://localhost:" + port + "/api/v1/rfid-tags";
        CrudScenario.run(http, base, "IT-SITE-" + System.nanoTime());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"status\":\"UP\"");
    }
}
