package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findTopByCorreoOrderByCreadoEnDesc(String correo);
}
