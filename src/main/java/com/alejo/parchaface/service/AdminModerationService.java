package com.alejo.parchaface.service;

import com.alejo.parchaface.model.CommunityComment;
import com.alejo.parchaface.model.CommunityPost;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;

import java.util.List;

public interface AdminModerationService {
  List<Evento> listarEventosPendientes();
  Evento aprobarEvento(Integer idEvento);
  Evento rechazarEvento(Integer idEvento, String motivo);
  List<Usuario> listarUsuarios();
  Usuario suspenderUsuario(Integer idUsuario);
  Usuario activarUsuario(Integer idUsuario);
  void eliminarUsuario(Integer idUsuario);
  List<CommunityPost> listarPosts();
  List<CommunityComment> listarComentarios();
  void eliminarPost(Integer idPost);
  void eliminarComentario(Integer idComentario);
}
