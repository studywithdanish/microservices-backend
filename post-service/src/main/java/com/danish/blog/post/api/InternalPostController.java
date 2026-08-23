package com.danish.blog.post.api;

import com.danish.blog.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/posts")
public class InternalPostController {

    private final PostService postService;

    public InternalPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{postId}/reference")
    public ResponseEntity<PostReferenceResponse> getReference(@PathVariable Integer postId) {
        return ResponseEntity.ok(postService.getReference(postId));
    }
}
