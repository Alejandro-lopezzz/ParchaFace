package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.CommunityComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Integer> {

  @EntityGraph(attributePaths = {"usuario"})
  List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Integer postId);

  long countByPostId(Integer postId);

  @EntityGraph(attributePaths = {"usuario"})
  List<CommunityComment> findByUsuario_IdUsuarioOrderByCreatedAtDesc(Integer usuarioId);

  @EntityGraph(attributePaths = {"usuario"})
  List<CommunityComment> findAllByOrderByCreatedAtDesc();

  void deleteByUsuario_IdUsuario(Integer usuarioId);
  void deleteByPostId(Integer postId);
}
