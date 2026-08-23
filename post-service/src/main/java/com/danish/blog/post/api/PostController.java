package com.danish.blog.post.api;

import com.danish.blog.post.security.CurrentUserProvider;
import com.danish.blog.post.security.JwtPrincipal;
import com.danish.blog.post.service.FileService;
import com.danish.blog.post.service.PostService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final CurrentUserProvider currentUserProvider;
    private final String imagePath;

    public PostController(
            PostService postService,
            FileService fileService,
            CurrentUserProvider currentUserProvider,
            @Value("${app.image.storage-path}") String imagePath
    ) {
        this.postService = postService;
        this.fileService = fileService;
        this.currentUserProvider = currentUserProvider;
        this.imagePath = imagePath;
    }

    @PostMapping("/posts")
    public ResponseEntity<PostDto> create(
            @Valid @RequestBody PostCreateRequest request,
            Authentication authentication
    ) {
        JwtPrincipal actor = currentUserProvider.requireCurrentUser(authentication);
        return ResponseEntity.status(201).body(postService.create(request, actor.id()));
    }

    @PostMapping("/user/{userId}/category/{categoryId}/posts")
    public ResponseEntity<PostDto> createUsingLegacyRoute(
            @Valid @RequestBody PostCreateRequest request,
            @PathVariable Integer userId,
            @PathVariable Integer categoryId,
            Authentication authentication
    ) {
        JwtPrincipal actor = currentUserProvider.requireCurrentUser(authentication);
        if (!actor.id().equals(userId)) {
            throw new AccessDeniedException("The post author must match the authenticated user");
        }
        PostCreateRequest normalized = new PostCreateRequest(request.title(), request.content(), categoryId);
        return ResponseEntity.status(201).body(postService.create(normalized, userId));
    }

    @GetMapping("/user/{userId}/posts")
    public ResponseEntity<List<PostDto>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(postService.getByAuthor(userId));
    }

    @GetMapping("/category/{categoryId}/posts")
    public ResponseEntity<List<PostDto>> getByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(postService.getByCategory(categoryId));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<PostDto> getById(@PathVariable Integer postId) {
        return ResponseEntity.ok(postService.getById(postId));
    }

    @GetMapping("/posts")
    public ResponseEntity<PostResponse> getAll(
            @RequestParam(value = "pageNo", defaultValue = "0") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = "postId") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(postService.getAll(pageNo, pageSize, sortBy, sortDir));
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer postId, Authentication authentication) {
        postService.delete(postId, currentUserProvider.requireCurrentUser(authentication));
        return ResponseEntity.ok(new ApiResponse("Post deleted successfully", true));
    }

    @PutMapping("/post/{postId}")
    public ResponseEntity<PostDto> update(
            @Valid @RequestBody PostUpdateRequest request,
            @PathVariable Integer postId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(postService.update(
                request,
                postId,
                currentUserProvider.requireCurrentUser(authentication)
        ));
    }

    @GetMapping("/posts/search/{keywords}")
    public ResponseEntity<List<PostDto>> search(@PathVariable String keywords) {
        return ResponseEntity.ok(postService.search(keywords));
    }

    @PostMapping("/post/image/upload/{postId}")
    public ResponseEntity<PostDto> uploadImage(
            @PathVariable Integer postId,
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ) throws IOException {
        JwtPrincipal actor = currentUserProvider.requireCurrentUser(authentication);
        postService.verifyCanModify(postId, actor);
        String fileName = fileService.uploadImage(imagePath, image);
        return ResponseEntity.ok(postService.updateImage(postId, fileName, actor));
    }

    @GetMapping(value = "/post/image/{imageName}", produces = {
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    })
    public void downloadImage(@PathVariable String imageName, HttpServletResponse response) throws IOException {
        try (InputStream resource = fileService.getResource(imagePath, imageName)) {
            StreamUtils.copy(resource, response.getOutputStream());
        }
    }
}
