package com.danish.blog.repositories;

import com.danish.blog.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepo extends JpaRepository<Comment, Integer> {

    List<Comment> findByPostIdOrderByIdAsc(Integer postId);
}
