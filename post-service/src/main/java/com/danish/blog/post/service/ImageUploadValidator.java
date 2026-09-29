package com.danish.blog.post.service;

import com.danish.blog.post.error.ApiException;
import com.danish.blog.post.error.ImageStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ImageUploadValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp"
    );

    private final long maxSizeBytes;

    public ImageUploadValidator(
            @Value("${app.image.storage.max-size-bytes:10485760}") long maxSizeBytes
    ) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Image file is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ApiException("Image must not exceed " + maxSizeBytes + " bytes");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw unsupportedImage();
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!contentType.equals(CONTENT_TYPE_BY_EXTENSION.get(extension))) {
            throw unsupportedImage();
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(12);
            if (!hasExpectedSignature(contentType, signature)) {
                throw new ApiException("Image content does not match its declared type");
            }
        } catch (IOException exception) {
            throw new ImageStorageException("Image could not be validated", exception);
        }

        return new ValidatedImage(extension, contentType);
    }

    public void validateStoredName(String imageName) {
        if (imageName == null
                || imageName.isBlank()
                || imageName.contains("/")
                || imageName.contains("\\")
                || imageName.contains("..")) {
            throw new ApiException("Invalid image name");
        }
        extensionOf(imageName);
    }

    public String contentTypeFor(String imageName) {
        validateStoredName(imageName);
        return CONTENT_TYPE_BY_EXTENSION.get(extensionOf(imageName));
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
            throw unsupportedImage();
        }
        return extension;
    }

    private boolean hasExpectedSignature(String contentType, byte[] signature) {
        return switch (contentType) {
            case "image/jpeg" -> signature.length >= 3
                    && unsigned(signature[0]) == 0xff
                    && unsigned(signature[1]) == 0xd8
                    && unsigned(signature[2]) == 0xff;
            case "image/png" -> signature.length >= 8
                    && unsigned(signature[0]) == 0x89
                    && signature[1] == 'P'
                    && signature[2] == 'N'
                    && signature[3] == 'G'
                    && unsigned(signature[4]) == 0x0d
                    && unsigned(signature[5]) == 0x0a
                    && unsigned(signature[6]) == 0x1a
                    && unsigned(signature[7]) == 0x0a;
            case "image/webp" -> signature.length >= 12
                    && signature[0] == 'R'
                    && signature[1] == 'I'
                    && signature[2] == 'F'
                    && signature[3] == 'F'
                    && signature[8] == 'W'
                    && signature[9] == 'E'
                    && signature[10] == 'B'
                    && signature[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private ApiException unsupportedImage() {
        return new ApiException("Only JPEG, PNG and WebP images are supported");
    }

    public record ValidatedImage(String extension, String contentType) {
    }
}
