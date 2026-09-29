package com.danish.blog.post.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public record StoredImage(
        InputStream content,
        String contentType,
        long contentLength
) implements AutoCloseable {

    public StoredImage {
        Objects.requireNonNull(content, "Image content is required");
        Objects.requireNonNull(contentType, "Image content type is required");
        if (contentLength < 0) {
            throw new IllegalArgumentException("Image content length cannot be negative");
        }
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}
