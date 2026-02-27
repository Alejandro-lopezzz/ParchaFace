package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.EventoComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoCommentRepository extends JpaRepository<EventoComment, Integer> {

    Page<EventoComment> findByEvento_IdEventoOrderByCreatedAtDesc(Integer eventoId, Pageable pageable);

    Optional<EventoComment> findByIdEventoCommentAndUsuario_IdUsuario(Integer commentId, Integer usuarioId);
}