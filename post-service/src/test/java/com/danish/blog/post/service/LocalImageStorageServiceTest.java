package com.danish.blog.post.service;

import com.danish.blog.post.error.ApiException;
import com.danish.blog.post.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageServiceTest {

    private static final byte[] PNG_IMAGE = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4
    };

    @TempDir
    Path storageDirectory;

    @Test
    void storeAndLoadAllowedImageWithGeneratedName() throws Exception {
        LocalImageStorageService imageStorage = imageStorage();
        MockMultipartFile image = new MockMultipartFile(
                "image", "post.PNG", "image/png", PNG_IMAGE
        );

        String imageName = imageStorage.store(image);

        assertThat(imageName).endsWith(".png");
        try (StoredImage stored = imageStorage.load(imageName)) {
            assertThat(stored.contentType()).isEqualTo("image/png");
            assertThat(stored.contentLength()).isEqualTo(PNG_IMAGE.length);
            assertThat(stored.content().readAllBytes()).containsExactly(PNG_IMAGE);
        }
    }

    @Test
    void storeRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "payload.png", "application/octet-stream", PNG_IMAGE
        );

        assertThatThrownBy(() -> imageStorage().store(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }

    @Test
    void storeRejectsContentThatDoesNotMatchDeclaredType() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "payload.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        assertThatThrownBy(() -> imageStorage().store(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void storeRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "payload.exe", "image/png", PNG_IMAGE
        );

        assertThatThrownBy(() -> imageStorage().store(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }

    @Test
    void loadRejectsPathTraversal() {
        assertThatThrownBy(() -> imageStorage().load("../secret.png"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid image name");
    }

    @Test
    void loadReportsMissingImage() {
        assertThatThrownBy(() -> imageStorage().load("missing.png"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Image");
    }

    @Test
    void deleteRemovesStoredImage() {
        LocalImageStorageService imageStorage = imageStorage();
        String imageName = imageStorage.store(new MockMultipartFile(
                "image", "post.png", "image/png", PNG_IMAGE
        ));

        imageStorage.delete(imageName);

        assertThat(Files.exists(storageDirectory.resolve(imageName))).isFalse();
    }

    private LocalImageStorageService imageStorage() {
        return new LocalImageStorageService(
                new ImageUploadValidator(10 * 1024 * 1024),
                storageDirectory.toString()
        );
    }
}
