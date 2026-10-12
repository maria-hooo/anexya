package com.anexya.rfid_service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("aws")
class AwsDeploymentIT {

    private final String baseUrl = System.getProperty("baseUrl", "").replaceAll("/+$", "");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Test
    void healthCheckThroughLoadBalancer() throws Exception {
        assumeTrue(!baseUrl.isBlank(), "Pass -DbaseUrl=http://<alb-dns> to run AWS tests");

        HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void fullCrudAgainstRds() throws Exception {
        assumeTrue(!baseUrl.isBlank(), "Pass -DbaseUrl=http://<alb-dns> to run AWS tests");

        CrudScenario.run(http, baseUrl + "/api/v1/rfid-tags", "AWS-IT-" + System.currentTimeMillis());
    }
}
