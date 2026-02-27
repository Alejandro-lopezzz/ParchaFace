package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    // lo de antes
    Optional<PasswordResetCode> findTopByCorreoOrderByCreadoEnDesc(String correo);

    // ✅ NUEVO: trae SOLO el último código que todavía sirve
    Optional<PasswordResetCode> findFirstByCorreoAndUsadoFalseAndExpiraEnAfterOrderByCreadoEnDesc(
            String correo, LocalDateTime ahora
    );
}