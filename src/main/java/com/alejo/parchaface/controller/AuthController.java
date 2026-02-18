package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.ForgotPasswordRequest;
import com.alejo.parchaface.dto.LoginRequest;
import com.alejo.parchaface.dto.RegisterRequest;
import com.alejo.parchaface.dto.ResetPasswordRequest;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.model.enums.Rol;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.security.JwtUtil;
import com.alejo.parchaface.service.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordResetService passwordResetService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
    }

    // ✅ REGISTRO
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        String correo = normalizeEmail(request.correo());

        if (correo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo es obligatorio"));
        }

        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya existe"));
        }

        if (request.contrasena() == null || !request.contrasena().equals(request.confirmarContrasena())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Las contraseñas no coinciden"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.usuario());
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(Rol.USUARIO);
        usuario.setEstado(Estado.ACTIVO);
        usuario.setPreferenciasCompletadas(false);
        usuario.setCategoriasPreferidas(List.of());

        usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Usuario registrado",
                "correo", correo,
                "rol", usuario.getRol().name(),
                "preferenciasCompletadas", usuario.getPreferenciasCompletadas(),
                "categoriasPreferidas", usuario.getCategoriasPreferidas()
        ));
    }

    // ✅ LOGIN
    @PostMapping("/signin")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        String correo = normalizeEmail(request.correo());

        Optional<Usuario> optUsuario = usuarioRepository.findByCorreo(correo);
        if (optUsuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        Usuario usuario = optUsuario.get();

        if (usuario.getEstado() != Estado.ACTIVO) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Usuario inactivo o bloqueado"));
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        List<String> roles = List.of(usuario.getRol().name());

        String token = JwtUtil.generateToken(
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                roles,
                usuario.getNombre());


        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "tokenType", "Bearer",
                        "token", token,
                        "correo", usuario.getCorreo(),
                        "rol", usuario.getRol().name()
                ));
    }

    // ✅ Enviar código (si existe)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

        String correo = normalizeEmail(request.getCorreo());

        // Por seguridad: no revelar si existe o no
        passwordResetService.enviarCodigo(correo);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo existe, se envió un código."
        ));
    }

    // ✅ Validar código y actualizar contraseña (retorna token)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        String correo = normalizeEmail(request.getCorreo());

        try {
            String token = passwordResetService.restablecer(
                    correo,
                    request.getCodigo(),
                    request.getNuevaContrasena()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of(
                            "mensaje", "Contraseña actualizada",
                            "tokenType", "Bearer",
                            "token", token,
                            "correo", correo
                    ));

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}
