package com.danish.blog.post.repository;

import com.danish.blog.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByAuthorId(Integer authorId);

    List<Post> findByCategoryId(Integer categoryId);

    List<Post> findByTitleContainingIgnoreCase(String title);
}
