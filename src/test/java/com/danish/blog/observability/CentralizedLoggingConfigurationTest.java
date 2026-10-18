package com.danish.blog.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CentralizedLoggingConfigurationTest {

    private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void lifecyclePolicyDeletesApplicationLogsAfterFourteenDays() throws IOException {
        JsonNode policy = readJson("deploy/k8s/logging/ilm-policy.json");

        assertThat(policy.at("/policy/phases/delete/min_age").asText()).isEqualTo("14d");
        assertThat(policy.at("/policy/phases/delete/actions/delete").isObject()).isTrue();
    }

    @Test
    void indexTemplateDefinesSearchableEcsFields() throws IOException {
        JsonNode template = readJson("deploy/k8s/logging/index-template.json");

        assertThat(template.at("/index_patterns/0").asText()).isEqualTo("blog-platform-logs-*");
        assertThat(template.at("/template/settings/index.lifecycle.name").asText())
                .isEqualTo("blog-platform-logs-14-days");
        assertThat(template.at("/template/mappings/properties/service/properties/name/type").asText())
                .isEqualTo("keyword");
        assertThat(template.at("/template/mappings/properties/log/properties/level/type").asText())
                .isEqualTo("keyword");
        assertThat(template.at("/template/mappings/properties/correlationId/type").asText())
                .isEqualTo("keyword");
    }

    @Test
    void collectorsDecodeStructuredLogsAndUseProjectIndex() throws IOException {
        for (String file : new String[]{
                "deploy/k8s/logging/filebeat.docker.yml",
                "deploy/k8s/logging/filebeat.kubernetes.yml"
        }) {
            String configuration = Files.readString(REPOSITORY_ROOT.resolve(file));
            assertThat(configuration)
                    .contains("decode_json_fields:")
                    .contains("dataset: blog-platform.application")
                    .contains("blog-platform-logs-%{+yyyy.MM.dd}")
                    .contains("setup.ilm.enabled: false");
        }
    }

    @Test
    void deploymentDescriptorsContainCompletePrivateLoggingStack() throws IOException {
        String localCompose = Files.readString(REPOSITORY_ROOT.resolve("docker-compose.yml"));
        String productionCompose = Files.readString(REPOSITORY_ROOT.resolve("deploy/docker-compose.prod.yml"));
        String kubernetes = Files.readString(REPOSITORY_ROOT.resolve("deploy/k8s/logging.yaml"));

        assertThat(localCompose)
                .contains("elasticsearch:")
                .contains("kibana:")
                .contains("filebeat:")
                .contains("LOGGING_STRUCTURED_FORMAT_CONSOLE: ecs");
        assertThat(productionCompose)
                .contains("127.0.0.1:${ELASTICSEARCH_PORT:-9200}:9200")
                .contains("127.0.0.1:${KIBANA_PORT:-5601}:5601")
                .contains("LOGGING_STRUCTURED_FORMAT_CONSOLE: ecs");
        assertThat(kubernetes)
                .contains("kind: StatefulSet")
                .contains("name: elasticsearch")
                .contains("name: kibana")
                .contains("kind: DaemonSet")
                .contains("name: filebeat");
    }

    private JsonNode readJson(String relativePath) throws IOException {
        return objectMapper.readTree(REPOSITORY_ROOT.resolve(relativePath).toFile());
    }
}
