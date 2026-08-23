package com.danish.blog.post.service;

import com.danish.blog.post.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest {

    private final FileService fileService = new FileService();

    private final Path storageDirectory = Path.of("target", "test-file-storage").toAbsolutePath();

    @Test
    void uploadStoresAllowedImageWithGeneratedName() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "post.PNG", "image/png", new byte[]{1, 2, 3}
        );

        String fileName = fileService.uploadImage(storageDirectory.toString(), image);

        assertThat(fileName).endsWith(".png");
        try (InputStream stored = fileService.getResource(storageDirectory.toString(), fileName)) {
            assertThat(stored.readAllBytes()).containsExactly(1, 2, 3);
        }
    }

    @Test
    void uploadRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "payload.png", "application/octet-stream", new byte[]{1}
        );

        assertThatThrownBy(() -> fileService.uploadImage(storageDirectory.toString(), file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "payload.exe", "image/png", new byte[]{1}
        );

        assertThatThrownBy(() -> fileService.uploadImage(storageDirectory.toString(), file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }

    @Test
    void resourceReadRejectsPathTraversal() {
        assertThatThrownBy(() -> fileService.getResource(storageDirectory.toString(), "../secret.png"))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("Image was not found");
    }
}
