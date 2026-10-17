package com.danish.blog.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GrafanaDashboardConfigurationTest {

    private static final Path DASHBOARD_DIRECTORY = Path.of(
            "deploy", "k8s", "grafana", "dashboards");
    private static final Set<String> EXPECTED_DASHBOARDS = Set.of(
            "event-pipeline.json",
            "platform-overview.json",
            "service-detail.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void dashboardsAreValidVersionControlledPrometheusDashboards() throws IOException {
        assertThat(DASHBOARD_DIRECTORY).isDirectory();

        Set<String> dashboardFiles;
        try (var paths = Files.list(DASHBOARD_DIRECTORY)) {
            dashboardFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toSet());
        }
        assertThat(dashboardFiles).containsExactlyInAnyOrderElementsOf(EXPECTED_DASHBOARDS);

        Set<String> dashboardUids = new HashSet<>();
        Set<Integer> panelIds = new HashSet<>();

        for (String dashboardFile : dashboardFiles) {
            JsonNode dashboard = objectMapper.readTree(DASHBOARD_DIRECTORY.resolve(dashboardFile).toFile());

            assertThat(dashboard.path("uid").asText()).isNotBlank();
            assertThat(dashboard.path("title").asText()).startsWith("Blog Platform");
            assertThat(dashboard.path("schemaVersion").asInt()).isGreaterThanOrEqualTo(41);
            assertThat(dashboard.path("editable").asBoolean()).isFalse();
            assertThat(dashboard.path("panels").isArray()).isTrue();
            assertThat(dashboard.path("panels")).isNotEmpty();
            assertThat(dashboardUids.add(dashboard.path("uid").asText())).isTrue();

            panelIds.clear();
            for (JsonNode panel : dashboard.path("panels")) {
                assertThat(panel.path("id").asInt()).isPositive();
                assertThat(panelIds.add(panel.path("id").asInt())).isTrue();
                assertThat(panel.path("title").asText()).isNotBlank();
                assertThat(panel.path("type").asText()).isNotBlank();
                assertThat(panel.path("datasource").path("uid").asText()).isEqualTo("prometheus");
                assertThat(panel.path("targets").isArray()).isTrue();
                assertThat(panel.path("targets")).isNotEmpty();

                for (JsonNode target : panel.path("targets")) {
                    assertThat(target.path("expr").asText()).isNotBlank();
                    if (target.has("datasource")) {
                        assertThat(target.path("datasource").path("uid").asText()).isEqualTo("prometheus");
                    }
                }
            }
        }

        assertThat(dashboardUids).hasSameSizeAs(EXPECTED_DASHBOARDS);
    }
}
