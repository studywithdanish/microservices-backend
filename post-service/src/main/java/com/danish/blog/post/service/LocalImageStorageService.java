package com.danish.blog.post.service;

import com.danish.blog.post.error.ImageStorageException;
import com.danish.blog.post.error.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "app.image.storage.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalImageStorageService implements ImageStorageService {

    private final ImageUploadValidator validator;
    private final Path storageRoot;

    public LocalImageStorageService(
            ImageUploadValidator validator,
            @Value("${app.image.storage.local-path:${POST_IMAGE_PATH:images/}}") String storagePath
    ) {
        this.validator = validator;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile image) {
        ImageUploadValidator.ValidatedImage validated = validator.validate(image);
        String imageName = UUID.randomUUID() + validated.extension();
        try {
            Files.createDirectories(storageRoot);
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, storageRoot.resolve(imageName));
            }
            return imageName;
        } catch (IOException exception) {
            throw new ImageStorageException("Image could not be stored", exception);
        }
    }

    @Override
    public StoredImage load(String imageName) {
        Path resource = resolveExistingImage(imageName);
        try {
            return new StoredImage(
                    Files.newInputStream(resource),
                    validator.contentTypeFor(imageName),
                    Files.size(resource)
            );
        } catch (IOException exception) {
            throw new ImageStorageException("Image could not be read", exception);
        }
    }

    @Override
    public void delete(String imageName) {
        validator.validateStoredName(imageName);
        Path resource = storageRoot.resolve(imageName).normalize();
        if (!resource.startsWith(storageRoot)) {
            throw imageNotFound(imageName);
        }
        try {
            Files.deleteIfExists(resource);
        } catch (IOException exception) {
            throw new ImageStorageException("Image could not be deleted", exception);
        }
    }

    private Path resolveExistingImage(String imageName) {
        validator.validateStoredName(imageName);
        Path resource = storageRoot.resolve(imageName).normalize();
        if (!resource.startsWith(storageRoot) || !Files.isRegularFile(resource)) {
            throw imageNotFound(imageName);
        }
        return resource;
    }

    private ResourceNotFoundException imageNotFound(String imageName) {
        return new ResourceNotFoundException("Image", "name", imageName);
    }
}
