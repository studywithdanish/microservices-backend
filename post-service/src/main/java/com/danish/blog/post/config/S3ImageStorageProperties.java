package com.danish.blog.post.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.image.storage.s3")
public class S3ImageStorageProperties {

    @NotBlank
    private String bucket;

    @NotBlank
    private String region = "eu-west-1";

    private String keyPrefix = "post-images";

    private URI endpoint;

    private boolean pathStyleAccess;

    @NotNull
    private Duration apiCallTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration apiCallAttemptTimeout = Duration.ofSeconds(5);

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public Duration getApiCallTimeout() {
        return apiCallTimeout;
    }

    public void setApiCallTimeout(Duration apiCallTimeout) {
        this.apiCallTimeout = apiCallTimeout;
    }

    public Duration getApiCallAttemptTimeout() {
        return apiCallAttemptTimeout;
    }

    public void setApiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
        this.apiCallAttemptTimeout = apiCallAttemptTimeout;
    }
}
