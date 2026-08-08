package com.danish.blog.services.impl;

import com.danish.blog.exceptions.ApiException;
import com.danish.blog.services.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Image file is required");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ApiException("Only JPEG, PNG and WebP images are supported");
        }

        String extension = extensionOf(file.getOriginalFilename());
        Path root = storageRoot(path);
        Files.createDirectories(root);
        String fileName = UUID.randomUUID() + extension;
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, root.resolve(fileName));
        }
        return fileName;
    }

    @Override
    public InputStream getResource(String path, String fileName) throws FileNotFoundException {
        Path root = storageRoot(path);
        Path resource = root.resolve(fileName).normalize();
        if (!resource.startsWith(root) || !Files.isRegularFile(resource)) {
            throw new FileNotFoundException("Image was not found");
        }

        try {
            return Files.newInputStream(resource);
        } catch (IOException ex) {
            FileNotFoundException notFound = new FileNotFoundException("Image was not found");
            notFound.initCause(ex);
            throw notFound;
        }
    }

    private Path storageRoot(String path) {
        return Paths.get(path).toAbsolutePath().normalize();
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            throw new ApiException("Image filename is required");
        }

        int extensionIndex = originalName.lastIndexOf('.');
        String extension = extensionIndex >= 0
                ? originalName.substring(extensionIndex).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException("Only JPEG, PNG and WebP images are supported");
        }
        return extension;
    }
}
