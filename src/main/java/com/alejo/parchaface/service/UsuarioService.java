package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Usuario;
import java.util.List;

public interface UsuarioService {
    List<Usuario> getAllUsuarios();
    Usuario getUsuarioById(Integer id);
    Usuario saveUsuario(Usuario usuario);
    void deleteUsuario(Integer id);
}

