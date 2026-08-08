package com.danish.blog.services.impl;

import com.danish.blog.exceptions.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceImplTest {

    private final FileServiceImpl fileService = new FileServiceImpl();
    private final Path storageDirectory = Path.of("target", "test-file-storage").toAbsolutePath();

    @Test
    void uploadImageStoresAllowedImageWithGeneratedName() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.PNG",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String fileName = fileService.uploadImage(storageDirectory.toString(), image);

        assertThat(fileName).endsWith(".png");
        try (InputStream storedImage = fileService.getResource(storageDirectory.toString(), fileName)) {
            assertThat(storedImage.readAllBytes()).containsExactly(1, 2, 3);
        }
    }

    @Test
    void uploadImageRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "payload.png",
                "application/octet-stream",
                new byte[]{1}
        );

        assertThatThrownBy(() -> fileService.uploadImage(storageDirectory.toString(), file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }

    @Test
    void uploadImageRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "payload.exe",
                "image/png",
                new byte[]{1}
        );

        assertThatThrownBy(() -> fileService.uploadImage(storageDirectory.toString(), file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }

    @Test
    void getResourceRejectsPathTraversal() {
        assertThatThrownBy(() -> fileService.getResource(storageDirectory.toString(), "../secret.png"))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("Image was not found");
    }
}
