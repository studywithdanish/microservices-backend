package com.danish.blog.content.repository;

import com.danish.blog.content.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    List<Comment> findByPostIdOrderByIdAsc(Integer postId);
}
