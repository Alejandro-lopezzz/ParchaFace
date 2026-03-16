package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Buscar un usuario por su correo (para autenticación JWT)
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findTop10ByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseAndIdUsuarioNot(
            String nombre,
            String correo,
            Integer idUsuario
    );

    Optional<Usuario> findByGoogleSub(String googleSub);
}


