package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Integer> {
  List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Integer postId);
  long countByPostId(Integer postId);
}
