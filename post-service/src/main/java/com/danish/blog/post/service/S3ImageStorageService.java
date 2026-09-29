package com.danish.blog.post.service;

import com.danish.blog.post.config.S3ImageStorageProperties;
import com.danish.blog.post.error.ImageStorageException;
import com.danish.blog.post.error.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.image.storage.provider", havingValue = "s3")
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3ImageStorageProperties properties;
    private final ImageUploadValidator validator;

    public S3ImageStorageService(
            S3Client s3Client,
            S3ImageStorageProperties properties,
            ImageUploadValidator validator
    ) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.validator = validator;
    }

    @Override
    public String store(MultipartFile image) {
        ImageUploadValidator.ValidatedImage validated = validator.validate(image);
        String imageName = UUID.randomUUID() + validated.extension();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey(imageName))
                .contentType(validated.contentType())
                .contentLength(image.getSize())
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        try (InputStream inputStream = image.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, image.getSize()));
            return imageName;
        } catch (IOException | SdkException exception) {
            throw new ImageStorageException("Image could not be stored in S3", exception);
        }
    }

    @Override
    public StoredImage load(String imageName) {
        validator.validateStoredName(imageName);
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(imageName))
                    .build());
            long contentLength = response.response().contentLength() == null
                    ? 0
                    : response.response().contentLength();
            return new StoredImage(response, validator.contentTypeFor(imageName), contentLength);
        } catch (NoSuchKeyException exception) {
            throw imageNotFound(imageName);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw imageNotFound(imageName);
            }
            throw new ImageStorageException("Image could not be read from S3", exception);
        } catch (SdkException exception) {
            throw new ImageStorageException("Image could not be read from S3", exception);
        }
    }

    @Override
    public void delete(String imageName) {
        validator.validateStoredName(imageName);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey(imageName))
                    .build());
        } catch (SdkException exception) {
            throw new ImageStorageException("Image could not be deleted from S3", exception);
        }
    }

    private String objectKey(String imageName) {
        String prefix = properties.getKeyPrefix() == null
                ? ""
                : properties.getKeyPrefix().replaceAll("^/+|/+$", "");
        return prefix.isBlank() ? imageName : prefix + "/" + imageName;
    }

    private ResourceNotFoundException imageNotFound(String imageName) {
        return new ResourceNotFoundException("Image", "name", imageName);
    }
}
