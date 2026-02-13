package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.LoginRequest;
import com.alejo.parchaface.dto.RegisterRequest;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.model.enums.Rol;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ REGISTRO
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        String correo = normalizeEmail(request.correo());

        if (correo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo es obligatorio"));
        }

        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            // Mejor que 400: conflicto de recurso existente
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya existe"));
        }

        // Verificar que la contraseña y la confirmación coinciden
        if (request.contrasena() == null || !request.contrasena().equals(request.confirmarContrasena())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Las contraseñas no coinciden"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.usuario());           // según tu DTO actual
        usuario.setCorreo(correo);                      // normalizado
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(Rol.USUARIO);                    // rol fijo
        usuario.setEstado(Estado.ACTIVO);

        usuarioRepository.save(usuario);

        List<String> roles = List.of(usuario.getRol().name());
        String token = JwtUtil.generateToken(usuario.getCorreo(), roles, usuario.getNombre());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "tokenType", "Bearer",
                        "token", token,
                        "correo", usuario.getCorreo(),
                        "rol", usuario.getRol().name()
                ));
    }

    // ✅ LOGIN
    @PostMapping("/signin")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        String correo = normalizeEmail(request.correo());

        // Por seguridad, evita decir "no existe" vs "contraseña mala"
        Optional<Usuario> optUsuario = usuarioRepository.findByCorreo(correo);
        if (optUsuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        Usuario usuario = optUsuario.get();

        // (Opcional, recomendado) validar estado
        if (usuario.getEstado() != Estado.ACTIVO) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Usuario inactivo o bloqueado"));
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        // Roles como string (mismo formato que ya estabas usando)
        List<String> roles = List.of(usuario.getRol().name());
        String token = JwtUtil.generateToken(usuario.getCorreo(), roles, usuario.getNombre());

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "tokenType", "Bearer",
                        "token", token,
                        "correo", usuario.getCorreo(),
                        "rol", usuario.getRol().name()
                ));
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}
