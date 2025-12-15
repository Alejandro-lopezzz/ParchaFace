package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.LoginRequest;
import com.alejo.parchaface.dto.RegisterRequest;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.model.enums.Rol;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ REGISTRO
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        if (usuarioRepository.findByCorreo(request.correo()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El correo ya existe"));
        }

        // Verificar que la contraseña y la confirmación coinciden
        if (!request.contrasena().equals(request.confirmarContrasena())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Las contraseñas no coinciden"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.usuario());
        usuario.setCorreo(request.correo());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(Rol.USUARIO); // El rol es fijo
        usuario.setEstado(Estado.ACTIVO);

        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Usuario registrado"));
    }

    // ✅ LOGIN
    @PostMapping("/signin")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(
                request.contrasena(),
                usuario.getContrasena()
        )) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        String token = JwtUtil.generateToken(usuario.getCorreo());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol()
        ));
    }
}
