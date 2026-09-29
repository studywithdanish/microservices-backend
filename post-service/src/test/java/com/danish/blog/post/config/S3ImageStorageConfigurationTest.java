package com.danish.blog.post.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

class S3ImageStorageConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(S3ImageStorageConfiguration.class);

    @Test
    void createsS3ClientOnlyWhenS3ProviderIsSelected() {
        contextRunner
                .withPropertyValues(
                        "app.image.storage.provider=s3",
                        "app.image.storage.s3.bucket=portfolio-images",
                        "app.image.storage.s3.region=eu-west-1"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(S3Client.class);
                    assertThat(context).hasSingleBean(S3ImageStorageProperties.class);
                });
    }

    @Test
    void doesNotCreateS3ClientForLocalProvider() {
        contextRunner
                .withPropertyValues("app.image.storage.provider=local")
                .run(context -> assertThat(context).doesNotHaveBean(S3Client.class));
    }

    @Test
    void rejectsS3ProviderWithoutBucket() {
        contextRunner
                .withPropertyValues(
                        "app.image.storage.provider=s3",
                        "app.image.storage.s3.region=eu-west-1"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
