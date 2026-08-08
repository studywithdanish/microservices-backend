package com.danish.blog.controllers;

import com.danish.blog.payloads.ApiResponse;
import com.danish.blog.payloads.AppConstants;
import com.danish.blog.payloads.PostCreateRequest;
import com.danish.blog.payloads.PostDto;
import com.danish.blog.payloads.PostResponse;
import com.danish.blog.payloads.PostUpdateRequest;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.security.AuthenticatedUserProvider;
import com.danish.blog.services.FileService;
import com.danish.blog.services.PostService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final FileService fileService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Value("${project.image}")
    private String path;

    public PostController(
            PostService postService,
            FileService fileService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.postService = postService;
        this.fileService = fileService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/posts")
    public ResponseEntity<PostDto> createPost(
            @Valid @RequestBody PostCreateRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser actor = authenticatedUserProvider.getCurrentUser(authentication);
        return new ResponseEntity<>(postService.createPost(request, actor.id()), HttpStatus.CREATED);
    }

    @PostMapping("/user/{userId}/category/{categoryId}/posts")
    public ResponseEntity<PostDto> createPostUsingLegacyRoute(
            @Valid @RequestBody PostCreateRequest request,
            @PathVariable Integer userId,
            @PathVariable Integer categoryId,
            Authentication authentication
    ) {
        AuthenticatedUser actor = authenticatedUserProvider.getCurrentUser(authentication);
        if (!actor.id().equals(userId)) {
            throw new AccessDeniedException("The post author must match the authenticated user");
        }
        request.setCategoryId(categoryId);
        return new ResponseEntity<>(postService.createPost(request, actor.id()), HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}/posts")
    public ResponseEntity<List<PostDto>> getPostsByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(postService.getPostByUser(userId));
    }

    @GetMapping("/category/{categoryId}/posts")
    public ResponseEntity<List<PostDto>> getPostsByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(postService.getPostByCategory(categoryId));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable Integer postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @GetMapping("/posts")
    public ResponseEntity<PostResponse> getAllPosts(
            @RequestParam(value = "pageNo", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.SORT_DIR, required = false) String sortDir
    ) {
        return ResponseEntity.ok(postService.getAllPosts(pageNo, pageSize, sortBy, sortDir));
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<ApiResponse> deletePost(
            @PathVariable Integer postId,
            Authentication authentication
    ) {
        postService.deletePost(postId, authenticatedUserProvider.getCurrentUser(authentication));
        return ResponseEntity.ok(new ApiResponse("Post deleted successfully", true));
    }

    @PutMapping("/post/{postId}")
    public ResponseEntity<PostDto> updatePost(
            @Valid @RequestBody PostUpdateRequest request,
            @PathVariable Integer postId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(postService.updatePost(
                request,
                postId,
                authenticatedUserProvider.getCurrentUser(authentication)
        ));
    }

    @GetMapping("/posts/search/{keywords}")
    public ResponseEntity<List<PostDto>> searchPostByTitle(@PathVariable String keywords) {
        return ResponseEntity.ok(postService.searchPosts(keywords));
    }

    @PostMapping("/post/image/upload/{postId}")
    public ResponseEntity<PostDto> uploadPostImage(
            @PathVariable Integer postId,
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ) throws IOException {
        AuthenticatedUser actor = authenticatedUserProvider.getCurrentUser(authentication);
        postService.verifyCanModify(postId, actor);
        String fileName = fileService.uploadImage(path, image);
        return ResponseEntity.ok(postService.updatePostImage(postId, fileName, actor));
    }

    @GetMapping(value = "/post/image/{imageName}", produces = {
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    })
    public void downloadImage(
            @PathVariable String imageName,
            HttpServletResponse response
    ) throws IOException {
        try (InputStream resource = fileService.getResource(path, imageName)) {
            StreamUtils.copy(resource, response.getOutputStream());
        }
    }
}
