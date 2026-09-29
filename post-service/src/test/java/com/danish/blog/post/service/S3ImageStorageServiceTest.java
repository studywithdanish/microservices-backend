package com.danish.blog.post.service;

import com.danish.blog.post.config.S3ImageStorageProperties;
import com.danish.blog.post.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageServiceTest {

    private static final byte[] PNG_IMAGE = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4
    };

    @Mock
    private S3Client s3Client;

    private S3ImageStorageService imageStorage;

    @BeforeEach
    void setUp() {
        S3ImageStorageProperties properties = new S3ImageStorageProperties();
        properties.setBucket("portfolio-images");
        properties.setRegion("eu-west-1");
        properties.setKeyPrefix("post-images");
        imageStorage = new S3ImageStorageService(
                s3Client,
                properties,
                new ImageUploadValidator(10 * 1024 * 1024)
        );
    }

    @Test
    void storeUsesPrivateEncryptedObjectWithConfiguredPrefix() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile image = new MockMultipartFile(
                "image", "post.png", "image/png", PNG_IMAGE
        );

        String imageName = imageStorage.store(image);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(imageName).matches("[0-9a-f-]{36}\\.png");
        assertThat(request.getValue().bucket()).isEqualTo("portfolio-images");
        assertThat(request.getValue().key()).isEqualTo("post-images/" + imageName);
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
        assertThat(request.getValue().contentLength()).isEqualTo(PNG_IMAGE.length);
        assertThat(request.getValue().serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
        assertThat(request.getValue().acl()).isNull();
    }

    @Test
    void loadReturnsS3ContentAndMetadata() throws Exception {
        GetObjectResponse objectResponse = GetObjectResponse.builder()
                .contentType("text/html")
                .contentLength((long) PNG_IMAGE.length)
                .build();
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                objectResponse,
                AbortableInputStream.create(new ByteArrayInputStream(PNG_IMAGE))
        );
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        try (StoredImage stored = imageStorage.load("image.png")) {
            assertThat(stored.contentType()).isEqualTo("image/png");
            assertThat(stored.contentLength()).isEqualTo(PNG_IMAGE.length);
            assertThat(stored.content().readAllBytes()).containsExactly(PNG_IMAGE);
        }

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("portfolio-images");
        assertThat(request.getValue().key()).isEqualTo("post-images/image.png");
    }

    @Test
    void loadMapsS3NotFoundToDomainNotFound() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.builder()
                .statusCode(404)
                .message("Not found")
                .build());

        assertThatThrownBy(() -> imageStorage.load("missing.png"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Image");
    }

    @Test
    void deleteUsesConfiguredBucketAndPrefix() {
        imageStorage.delete("image.png");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("portfolio-images");
        assertThat(request.getValue().key()).isEqualTo("post-images/image.png");
    }
}
