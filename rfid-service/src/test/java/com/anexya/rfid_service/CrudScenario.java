package com.anexya.rfid_service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

final class CrudScenario {

    private CrudScenario() {
    }

    static void run(HttpClient http, String base, String uniqueSite) throws Exception {
        String readAt = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString();
        String body = """
                {"siteName":"%s","epc":"e2801170000002100f2a3b4c","tid":"E2003412ABCD0001",
                 "location":"Dock 3","rssi":-52,"readAt":"%s"}
                """.formatted(uniqueSite, readAt);

        HttpResponse<String> created = send(http, "POST", base, body);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"epc\":\"E2801170000002100F2A3B4C\"");
        String location = created.headers().firstValue("Location").orElseThrow();
        String id = location.substring(location.lastIndexOf('/') + 1);
        String itemUrl = base + "/" + id;

        try {
            HttpResponse<String> fetched = send(http, "GET", itemUrl, null);
            assertThat(fetched.statusCode()).isEqualTo(200);
            assertThat(fetched.body()).contains(uniqueSite);

            HttpResponse<String> search = send(http, "GET",
                    base + "?epc=e2801170000002100f2a3b4c&siteName=" + uniqueSite, null);
            assertThat(search.statusCode()).isEqualTo(200);
            assertThat(search.body()).contains("\"totalElements\":1");

            HttpResponse<String> patched = send(http, "PATCH", itemUrl, "{\"location\":\"Dock 4\",\"rssi\":-61}");
            assertThat(patched.statusCode()).isEqualTo(200);
            assertThat(patched.body()).contains("\"location\":\"Dock 4\"").contains("\"rssi\":-61");

            HttpResponse<String> replaced = send(http, "PUT", itemUrl, body.replace("Dock 3", "Gate B"));
            assertThat(replaced.statusCode()).isEqualTo(200);
            assertThat(replaced.body()).contains("\"location\":\"Gate B\"").contains("\"rssi\":-52");

            HttpResponse<String> invalid = send(http, "POST", base, body.replace("-52", "20"));
            assertThat(invalid.statusCode()).isEqualTo(400);
        } finally {
            HttpResponse<String> deleted = send(http, "DELETE", itemUrl, null);
            assertThat(deleted.statusCode()).isEqualTo(204);
        }

        assertThat(send(http, "GET", itemUrl, null).statusCode()).isEqualTo(404);
    }

    private static HttpResponse<String> send(HttpClient http, String method, String url, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url.replace(" ", "%20")));
        if (json == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json));
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
