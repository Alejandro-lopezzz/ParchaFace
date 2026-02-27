package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.alejo.parchaface.repository.UsuarioRepository;

import java.io.IOException;
import java.nio.file.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    private UsuarioRepository usuarioRepository;

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

    // ✅ FOTO DE PERFIL (multipart)
    @PostMapping(value = "/{id}/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirFotoPerfil(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file
    ) {
        return guardarImagen(id, file, true);
    }

    // ✅ FOTO DE PORTADA (multipart)
    @PostMapping(value = "/{id}/foto-portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirFotoPortada(
            @PathVariable Integer id,
            @RequestPart("file") MultipartFile file
    ) {
        return guardarImagen(id, file, false);
    }

    // ✅ MÉTODO PRIVADO PARA GUARDAR IMAGEN
    private ResponseEntity<?> guardarImagen(Integer id, MultipartFile file, boolean esPerfil) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío");
            }

            // (Opcional recomendado) validar que sea imagen
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

            // ✅ guarda ruta pública (recomendado con slash inicial)
            String rutaPublica = "/uploads/" + nombreArchivo;

            if (esPerfil) {
                usuario.setFotoPerfil(rutaPublica);
            } else {
                usuario.setFotoPortada(rutaPublica);
            }

            Usuario actualizado = usuarioService.saveUsuario(usuario);

            // ✅ importante: devolver usuario actualizado para que el front pinte
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
}
