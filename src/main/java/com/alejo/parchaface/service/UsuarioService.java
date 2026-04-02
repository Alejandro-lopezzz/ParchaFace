package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.PerfilUsuarioDto;
import com.alejo.parchaface.dto.UsuarioBusquedaDto;
import com.alejo.parchaface.dto.UsuarioResumenDto;
import com.alejo.parchaface.model.Usuario;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UsuarioService {

  List<Usuario> getAllUsuarios();

  Usuario getUsuarioById(Integer id);

  Usuario saveUsuario(Usuario usuario);

  void deleteUsuario(Integer id);

  Usuario suspenderUsuario(Integer id);

  Usuario activarUsuario(Integer id);

  Usuario getUsuarioPorCorreo(String correo);

  PerfilUsuarioDto getPerfilUsuario(Integer idUsuarioPerfil, String correoUsuarioAutenticado);

  void seguirUsuario(Integer idUsuarioASeguir, String correoUsuarioAutenticado);

  void dejarDeSeguirUsuario(Integer idUsuarioASeguir, String correoUsuarioAutenticado);

  List<UsuarioResumenDto> obtenerSeguidores(Integer idUsuario);

  List<UsuarioResumenDto> obtenerSiguiendo(Integer idUsuario);

  List<UsuarioBusquedaDto> buscarUsuarios(String q, String correoUsuarioAutenticado);

  Usuario actualizarFotoPerfil(Integer idUsuario, MultipartFile file);

  Usuario actualizarFotoPortada(Integer idUsuario, MultipartFile file);

  Usuario eliminarFotoPerfil(Integer idUsuario);
}
