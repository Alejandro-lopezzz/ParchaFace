package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.PostRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRatingRepository extends JpaRepository<PostRating, Integer> {
  Optional<PostRating> findByPostIdAndUserCorreo(Integer postId, String userCorreo);
  List<PostRating> findByPostId(Integer postId);
  long countByPostId(Integer postId);
}
