package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.CommunityPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {

  @EntityGraph(attributePaths = {"usuario"})
  List<CommunityPost> findByUsuario_IdUsuarioOrderByCreatedAtDesc(Integer usuarioId);

  @EntityGraph(attributePaths = {"usuario"})
  List<CommunityPost> findAllByOrderByCreatedAtDesc();

  void deleteByUsuario_IdUsuario(Integer usuarioId);
}
