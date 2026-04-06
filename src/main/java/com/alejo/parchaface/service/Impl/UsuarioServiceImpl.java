package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.PerfilUsuarioDto;
import com.alejo.parchaface.dto.UsuarioBusquedaDto;
import com.alejo.parchaface.dto.UsuarioResumenDto;
import com.alejo.parchaface.model.Seguimiento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.repository.SeguimientoRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.UsuarioService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final SeguimientoRepository seguimientoRepository;
  private final Cloudinary cloudinary;

  public UsuarioServiceImpl(
    UsuarioRepository usuarioRepository,
    SeguimientoRepository seguimientoRepository,
    Cloudinary cloudinary
  ) {
    this.usuarioRepository = usuarioRepository;
    this.seguimientoRepository = seguimientoRepository;
    this.cloudinary = cloudinary;
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
    Usuario usuario = usuarioRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    usuario.setEstado(Estado.CANCELADO);
    usuarioRepository.save(usuario);
  }

  @Override
  public Usuario suspenderUsuario(Integer id) {
    Usuario usuario = usuarioRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    usuario.setEstado(Estado.SUSPENDIDO);
    return usuarioRepository.save(usuario);
  }

  @Override
  public Usuario activarUsuario(Integer id) {
    Usuario usuario = usuarioRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    usuario.setEstado(Estado.ACTIVO);
    return usuarioRepository.save(usuario);
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

    // Mantengo compatibilidad con el frontend actual
    dto.setFotoPerfil(usuarioPerfil.getFotoPerfilUrl());
    dto.setFotoPortada(usuarioPerfil.getFotoPortadaUrl());

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

  @Override
  public Usuario actualizarFotoPerfil(Integer idUsuario, MultipartFile file) {
    return actualizarImagenUsuario(idUsuario, file, true);
  }

  @Override
  public Usuario actualizarFotoPortada(Integer idUsuario, MultipartFile file) {
    return actualizarImagenUsuario(idUsuario, file, false);
  }

  private Usuario actualizarImagenUsuario(Integer idUsuario, MultipartFile file, boolean esPerfil) {
    try {
      if (file == null || file.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
      }

      String contentType = file.getContentType();
      if (contentType == null || !contentType.startsWith("image/")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes");
      }

      Usuario usuario = usuarioRepository.findById(idUsuario)
        .orElseThrow(() -> new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Usuario no encontrado con id: " + idUsuario
        ));

      String publicIdAnterior = esPerfil
        ? usuario.getFotoPerfilPublicId()
        : usuario.getFotoPortadaPublicId();

      if (publicIdAnterior != null && !publicIdAnterior.isBlank()) {
        cloudinary.uploader().destroy(
          publicIdAnterior,
          ObjectUtils.asMap("resource_type", "image")
        );
      }

      String folder = esPerfil
        ? "parchaface/usuarios/perfil"
        : "parchaface/usuarios/portada";

      Map<?, ?> uploadResult = cloudinary.uploader().upload(
        file.getBytes(),
        ObjectUtils.asMap(
          "folder", folder,
          "resource_type", "image"
        )
      );

      String secureUrl = (String) uploadResult.get("secure_url");
      String publicId = (String) uploadResult.get("public_id");

      if (secureUrl == null || secureUrl.isBlank()) {
        throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Cloudinary no devolvió una URL válida"
        );
      }

      if (esPerfil) {
        usuario.setFotoPerfilUrl(secureUrl);
        usuario.setFotoPerfilPublicId(publicId);
      } else {
        usuario.setFotoPortadaUrl(secureUrl);
        usuario.setFotoPortadaPublicId(publicId);
      }

      return usuarioRepository.save(usuario);

    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Error subiendo imagen a Cloudinary: " + ex.getMessage()
      );
    }
  }

  private UsuarioResumenDto mapToUsuarioResumenDto(Usuario usuario) {
    UsuarioResumenDto dto = new UsuarioResumenDto();
    dto.setIdUsuario(usuario.getIdUsuario());
    dto.setNombre(usuario.getNombre());
    dto.setFotoPerfil(usuario.getFotoPerfilUrl());
    dto.setAcercaDe(usuario.getAcercaDe());
    return dto;
  }

  private UsuarioBusquedaDto mapToUsuarioBusquedaDto(Usuario usuario) {
    UsuarioBusquedaDto dto = new UsuarioBusquedaDto();
    dto.setIdUsuario(usuario.getIdUsuario());
    dto.setNombre(usuario.getNombre());
    dto.setCorreo(usuario.getCorreo());
    dto.setFotoPerfil(usuario.getFotoPerfilUrl());
    dto.setAcercaDe(usuario.getAcercaDe());
    return dto;
  }

  @Override
  public Usuario eliminarFotoPerfil(Integer idUsuario) {
    try {
      Usuario usuario = usuarioRepository.findById(idUsuario)
        .orElseThrow(() -> new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Usuario no encontrado con id: " + idUsuario
        ));

      String publicIdAnterior = usuario.getFotoPerfilPublicId();

      if (publicIdAnterior != null && !publicIdAnterior.isBlank()) {
        cloudinary.uploader().destroy(
          publicIdAnterior,
          ObjectUtils.asMap("resource_type", "image")
        );
      }

      usuario.setFotoPerfilUrl(null);
      usuario.setFotoPerfilPublicId(null);

      return usuarioRepository.save(usuario);

    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Error eliminando foto de perfil: " + ex.getMessage()
      );
    }
  }

  @Override
  public void eliminarMiCuenta(String correo) {
    Usuario usuario = getUsuarioPorCorreo(correo);
    usuario.setEstado(Estado.CANCELADO);
    usuarioRepository.save(usuario);
  }
}
