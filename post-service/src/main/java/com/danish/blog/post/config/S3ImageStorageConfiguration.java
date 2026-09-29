package com.danish.blog.post.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@ConditionalOnProperty(name = "app.image.storage.provider", havingValue = "s3")
@EnableConfigurationProperties(S3ImageStorageProperties.class)
public class S3ImageStorageConfiguration {

    @Bean
    public S3Client imageStorageS3Client(S3ImageStorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(properties.getApiCallTimeout())
                        .apiCallAttemptTimeout(properties.getApiCallAttemptTimeout())
                        .build())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build());

        if (properties.getEndpoint() != null
                && StringUtils.hasText(properties.getEndpoint().toString())) {
            builder.endpointOverride(properties.getEndpoint());
        }

        return builder.build();
    }
}
