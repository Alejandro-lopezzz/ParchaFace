package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Integer> {
  Optional<CommentLike> findByCommentIdAndUserCorreo(Integer commentId, String userCorreo);
  long countByCommentId(Integer commentId);
}
