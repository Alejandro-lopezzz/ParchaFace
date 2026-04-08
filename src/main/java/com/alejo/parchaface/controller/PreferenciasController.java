package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.PreferenciasRequest;
import com.alejo.parchaface.dto.PreferenciasResponse;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/preferencias")
public class PreferenciasController {

    private final UsuarioService usuarioService;

    public PreferenciasController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<PreferenciasResponse> getPreferencias(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(401).build();
        }
        Usuario usuario = usuarioService.getUsuarioPorCorreo(principal.getName().trim());
        return ResponseEntity.ok(new PreferenciasResponse(
            Boolean.TRUE.equals(usuario.getPreferenciasCompletadas()),
            usuario.getCategoriasPreferidas() != null ? usuario.getCategoriasPreferidas() : List.of()
        ));
    }

    @PutMapping
    public ResponseEntity<PreferenciasResponse> putPreferencias(
            Principal principal,
            @Valid @RequestBody PreferenciasRequest request) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(401).build();
        }
        List<String> categories = request.categories() != null ? request.categories() : List.of();
        Usuario usuario = usuarioService.getUsuarioPorCorreo(principal.getName().trim());
        usuario.setCategoriasPreferidas(categories);
        usuario.setPreferenciasCompletadas(true);
        usuarioService.saveUsuario(usuario);
        return ResponseEntity.ok(new PreferenciasResponse(true, categories));
    }
}
