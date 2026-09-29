package com.danish.blog.post.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    String store(MultipartFile image);

    StoredImage load(String imageName);

    void delete(String imageName);
}
