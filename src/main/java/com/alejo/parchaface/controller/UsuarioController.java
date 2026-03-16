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

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.util.List;

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

    // ✅ CORREGIDO: ahora devuelve UsuarioResumenDto

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
        return guardarImagen(id, file, true);
    }

    @PostMapping(value = "/{id}/foto-portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirFotoPortada(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file
    ) {
        return guardarImagen(id, file, false);
    }

    private ResponseEntity<?> guardarImagen(Integer id, MultipartFile file, boolean esPerfil) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Solo se permiten imágenes");
            }

            Usuario usuario = usuarioService.getUsuarioById(id);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }

            String original = file.getOriginalFilename() == null ? "imagen" : file.getOriginalFilename();
            String safeName = original.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String nombreArchivo = System.currentTimeMillis() + "_" + safeName;

            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            Path destino = uploadDir.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            String rutaPublica = "/uploads/" + nombreArchivo;

            if (esPerfil) {
                usuario.setFotoPerfil(rutaPublica);
            } else {
                usuario.setFotoPortada(rutaPublica);
            }

            Usuario actualizado = usuarioService.saveUsuario(usuario);
            return ResponseEntity.ok(actualizado);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir imagen: " + e.getMessage());
        }
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