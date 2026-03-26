package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.PerfilUsuarioDto;
import com.alejo.parchaface.dto.UsuarioBusquedaDto;
import com.alejo.parchaface.dto.UsuarioResumenDto;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Usuario> obtenerTodos() {
        return usuarioService.getAllUsuarios();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Integer id) {
        Usuario u = usuarioService.getUsuarioById(id);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(u);
    }

    @GetMapping("/{id}/perfil")
    public ResponseEntity<PerfilUsuarioDto> obtenerPerfil(@PathVariable Integer id, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PerfilUsuarioDto perfil = usuarioService.getPerfilUsuario(id, principal.getName());
        return ResponseEntity.ok(perfil);
    }

    @PostMapping("/{id}/seguir")
    public ResponseEntity<?> seguirUsuario(@PathVariable Integer id, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        usuarioService.seguirUsuario(id, principal.getName());
        return ResponseEntity.ok("Ahora sigues a este usuario");
    }

    @DeleteMapping("/{id}/seguir")
    public ResponseEntity<?> dejarDeSeguirUsuario(@PathVariable Integer id, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        usuarioService.dejarDeSeguirUsuario(id, principal.getName());
        return ResponseEntity.ok("Has dejado de seguir a este usuario");
    }

    @GetMapping("/{id}/seguidores")
    public ResponseEntity<List<UsuarioResumenDto>> obtenerSeguidores(@PathVariable Integer id) {
        List<UsuarioResumenDto> seguidores = usuarioService.obtenerSeguidores(id);
        return ResponseEntity.ok(seguidores);
    }

    @GetMapping("/{id}/siguiendo")
    public ResponseEntity<List<UsuarioResumenDto>> obtenerSiguiendo(@PathVariable Integer id) {
        List<UsuarioResumenDto> siguiendo = usuarioService.obtenerSiguiendo(id);
        return ResponseEntity.ok(siguiendo);
    }

    @PostMapping
    public Usuario guardar(@RequestBody Usuario usuario) {
        return usuarioService.saveUsuario(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Usuario cambios) {
        Usuario existente = usuarioService.getUsuarioById(id);
        if (existente == null) return ResponseEntity.notFound().build();

        if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
            existente.setNombre(cambios.getNombre().trim());
        }

        if (cambios.getCorreo() != null && !cambios.getCorreo().isBlank()) {
            String nuevoCorreo = cambios.getCorreo().trim().toLowerCase();
            if (!nuevoCorreo.equalsIgnoreCase(existente.getCorreo())) {
                if (usuarioRepository.findByCorreo(nuevoCorreo).isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("El correo ya existe");
                }
                existente.setCorreo(nuevoCorreo);
            }
        }

        if (cambios.getAcercaDe() != null) {
            existente.setAcercaDe(cambios.getAcercaDe().trim());
        }

        if (cambios.getRedesSociales() != null) {
            existente.setRedesSociales(cambios.getRedesSociales());
        }

        Usuario actualizado = usuarioService.saveUsuario(existente);
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping(value = "/{id}/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirFotoPerfil(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file
    ) {
        Usuario actualizado = usuarioService.actualizarFotoPerfil(id, file);

        return ResponseEntity.ok(Map.of(
                "idUsuario", actualizado.getIdUsuario(),
                "fotoPerfil", actualizado.getFotoPerfilUrl(),
                "fotoPerfilPublicId", actualizado.getFotoPerfilPublicId()
        ));
    }

    @PostMapping(value = "/{id}/foto-portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirFotoPortada(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file
    ) {
        Usuario actualizado = usuarioService.actualizarFotoPortada(id, file);

        return ResponseEntity.ok(Map.of(
                "idUsuario", actualizado.getIdUsuario(),
                "fotoPortada", actualizado.getFotoPortadaUrl(),
                "fotoPortadaPublicId", actualizado.getFotoPortadaPublicId()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<UsuarioBusquedaDto>> buscarUsuarios(
            @RequestParam("q") String q,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UsuarioBusquedaDto> resultados = usuarioService.buscarUsuarios(q, principal.getName());
        return ResponseEntity.ok(resultados);
    }
}