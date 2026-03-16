package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.PerfilUsuarioDto;
import com.alejo.parchaface.dto.UsuarioBusquedaDto;
import com.alejo.parchaface.dto.UsuarioResumenDto;
import com.alejo.parchaface.model.Seguimiento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.repository.SeguimientoRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SeguimientoRepository seguimientoRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              SeguimientoRepository seguimientoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.seguimientoRepository = seguimientoRepository;
    }

    @Override
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario getUsuarioById(Integer id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        return usuario.orElse(null);
    }

    @Override
    public Usuario saveUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    public void deleteUsuario(Integer id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    public Usuario getUsuarioPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo inválido");
        }

        String normalizado = correo.trim().toLowerCase();

        return usuarioRepository.findByCorreo(normalizado)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con correo: " + normalizado
                ));
    }

    @Override
    public PerfilUsuarioDto getPerfilUsuario(Integer idUsuarioPerfil, String correoUsuarioAutenticado) {
        Usuario usuarioPerfil = usuarioRepository.findById(idUsuarioPerfil)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con id: " + idUsuarioPerfil
                ));

        Usuario usuarioAutenticado = getUsuarioPorCorreo(correoUsuarioAutenticado);

        long totalSeguidores = seguimientoRepository.countBySeguido(usuarioPerfil);
        long totalSiguiendo = seguimientoRepository.countBySeguidor(usuarioPerfil);

        boolean seguidoPorMi = false;
        boolean esMiPerfil = usuarioAutenticado.getIdUsuario().equals(usuarioPerfil.getIdUsuario());

        if (!esMiPerfil) {
            seguidoPorMi = seguimientoRepository.existsBySeguidorAndSeguido(usuarioAutenticado, usuarioPerfil);
        }

        PerfilUsuarioDto dto = new PerfilUsuarioDto();
        dto.setIdUsuario(usuarioPerfil.getIdUsuario());
        dto.setNombre(usuarioPerfil.getNombre());
        dto.setCorreo(usuarioPerfil.getCorreo());
        dto.setFotoPerfil(usuarioPerfil.getFotoPerfil());
        dto.setFotoPortada(usuarioPerfil.getFotoPortada());
        dto.setAcercaDe(usuarioPerfil.getAcercaDe());
        dto.setCategoriasPreferidas(usuarioPerfil.getCategoriasPreferidas());
        dto.setTotalSeguidores(totalSeguidores);
        dto.setTotalSiguiendo(totalSiguiendo);
        dto.setSeguidoPorMi(seguidoPorMi);
        dto.setEsMiPerfil(esMiPerfil);

        return dto;
    }

    @Override
    public void seguirUsuario(Integer idUsuarioASeguir, String correoUsuarioAutenticado) {
        Usuario seguidor = getUsuarioPorCorreo(correoUsuarioAutenticado);

        Usuario seguido = usuarioRepository.findById(idUsuarioASeguir)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario a seguir no encontrado con id: " + idUsuarioASeguir
                ));

        if (seguidor.getIdUsuario().equals(seguido.getIdUsuario())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes seguirte a ti mismo"
            );
        }

        boolean yaExiste = seguimientoRepository.existsBySeguidorAndSeguido(seguidor, seguido);
        if (yaExiste) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya sigues a este usuario"
            );
        }

        Seguimiento seguimiento = new Seguimiento();
        seguimiento.setSeguidor(seguidor);
        seguimiento.setSeguido(seguido);

        seguimientoRepository.save(seguimiento);
    }

    @Override
    public void dejarDeSeguirUsuario(Integer idUsuarioASeguir, String correoUsuarioAutenticado) {
        Usuario seguidor = getUsuarioPorCorreo(correoUsuarioAutenticado);

        Usuario seguido = usuarioRepository.findById(idUsuarioASeguir)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con id: " + idUsuarioASeguir
                ));

        if (seguidor.getIdUsuario().equals(seguido.getIdUsuario())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes dejar de seguirte a ti mismo"
            );
        }

        Seguimiento seguimiento = seguimientoRepository.findBySeguidorAndSeguido(seguidor, seguido)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No sigues a este usuario"
                ));

        seguimientoRepository.delete(seguimiento);
    }

    @Override
    public List<UsuarioResumenDto> obtenerSeguidores(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con id: " + idUsuario
                ));

        List<Seguimiento> seguimientos = seguimientoRepository.findBySeguido(usuario);

        return seguimientos.stream()
                .map(Seguimiento::getSeguidor)
                .map(this::mapToUsuarioResumenDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsuarioResumenDto> obtenerSiguiendo(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con id: " + idUsuario
                ));

        List<Seguimiento> seguimientos = seguimientoRepository.findBySeguidor(usuario);

        return seguimientos.stream()
                .map(Seguimiento::getSeguido)
                .map(this::mapToUsuarioResumenDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsuarioBusquedaDto> buscarUsuarios(String q, String correoUsuarioAutenticado) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }

        String termino = q.trim();
        Usuario usuarioAutenticado = getUsuarioPorCorreo(correoUsuarioAutenticado);

        List<Usuario> usuarios =
                usuarioRepository.findTop10ByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseAndIdUsuarioNot(
                        termino,
                        termino,
                        usuarioAutenticado.getIdUsuario()
                );

        return usuarios.stream()
                .map(this::mapToUsuarioBusquedaDto)
                .collect(Collectors.toList());
    }

    private UsuarioResumenDto mapToUsuarioResumenDto(Usuario usuario) {
        UsuarioResumenDto dto = new UsuarioResumenDto();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setFotoPerfil(usuario.getFotoPerfil());
        dto.setAcercaDe(usuario.getAcercaDe());
        return dto;
    }

    private UsuarioBusquedaDto mapToUsuarioBusquedaDto(Usuario usuario) {
        UsuarioBusquedaDto dto = new UsuarioBusquedaDto();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setFotoPerfil(usuario.getFotoPerfil());
        dto.setAcercaDe(usuario.getAcercaDe());
        return dto;
    }
}