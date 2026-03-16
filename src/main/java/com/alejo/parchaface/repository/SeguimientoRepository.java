package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Seguimiento;
import com.alejo.parchaface.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Integer> {

    boolean existsBySeguidorAndSeguido(Usuario seguidor, Usuario seguido);

    Optional<Seguimiento> findBySeguidorAndSeguido(Usuario seguidor, Usuario seguido);

    long countBySeguido(Usuario seguido);

    long countBySeguidor(Usuario seguidor);

    List<Seguimiento> findBySeguido(Usuario seguido);

    List<Seguimiento> findBySeguidor(Usuario seguidor);

    void deleteBySeguidorAndSeguido(Usuario seguidor, Usuario seguido);
}