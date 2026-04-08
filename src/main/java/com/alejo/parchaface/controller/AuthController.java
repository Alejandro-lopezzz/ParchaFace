package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.VerifyResetCodeRequest;
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
import com.alejo.parchaface.dto.GoogleAuthRequest;
import com.alejo.parchaface.service.GoogleTokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.util.UUID;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetService passwordResetService;
  private final GoogleTokenService googleTokenService;

  public AuthController(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        PasswordResetService passwordResetService,
                        GoogleTokenService googleTokenService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordResetService = passwordResetService;
    this.googleTokenService = googleTokenService;
  }

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

    if (!esContrasenaFuerte(request.contrasena())) {
      return ResponseEntity.badRequest().body(Map.of(
        "error", "La contraseña debe tener mínimo 8 caracteres e incluir mayúscula, minúscula, número y símbolo."
      ));
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

  @PostMapping("/signin")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

    String correo = normalizeEmail(request.correo());

    Optional<Usuario> optUsuario = usuarioRepository.findByCorreo(correo);
    if (optUsuario.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Credenciales inválidas"));
    }

    Usuario usuario = optUsuario.get();

    if (usuario.getEstado() == Estado.CANCELADO) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "Esta cuenta ya no se encuentra disponible"));
    }

    if (!esContrasenaFuerte(request.contrasena())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "Tu contraseña no cumple la política. Restablécela para continuar."));
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
      usuario.getNombre()
    );

    return ResponseEntity.ok()
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .body(Map.of(
        "tokenType", "Bearer",
        "token", token,
        "correo", usuario.getCorreo(),
        "rol", usuario.getRol().name()
      ));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

    String correo = normalizeEmail(request.getCorreo());

    passwordResetService.enviarCodigo(correo);

    return ResponseEntity.ok(Map.of(
      "mensaje", "Si el correo existe, se envió un código."
    ));
  }

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

  @PostMapping("/verify-reset-code")
  public ResponseEntity<?> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {

    String correo = normalizeEmail(request.getCorreo());

    try {
      passwordResetService.validarCodigo(correo, request.getCodigo());
      return ResponseEntity.ok(Map.of("mensaje", "Código válido"));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", ex.getMessage()));
    }
  }

  private String normalizeEmail(String correo) {
    return correo == null ? "" : correo.trim().toLowerCase();
  }

  private boolean esContrasenaFuerte(String pass) {
    if (pass == null) return false;
    return pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
  }

  @PostMapping("/google")
  public ResponseEntity<?> googleAuth(@RequestBody GoogleAuthRequest request) {
    if (request == null || request.credential() == null || request.credential().isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Credential requerido"));
    }

    GoogleIdToken.Payload payload = googleTokenService.verify(request.credential());

    String googleSub = payload.getSubject();
    String correo = ((String) payload.getEmail()).trim().toLowerCase();
    String nombre = (String) payload.get("name");
    Boolean emailVerified = (Boolean) payload.getEmailVerified();

    if (emailVerified == null || !emailVerified) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "El correo de Google no está verificado"));
    }

    Usuario usuario = usuarioRepository.findByGoogleSub(googleSub).orElseGet(() -> {
      Optional<Usuario> existentePorCorreo = usuarioRepository.findByCorreo(correo);

      if (existentePorCorreo.isPresent()) {
        Usuario existente = existentePorCorreo.get();
        existente.setGoogleSub(googleSub);
        existente.setAuthProvider("GOOGLE");
        return existente;
      }

      Usuario nuevo = new Usuario();
      nuevo.setNombre(nombre != null && !nombre.isBlank() ? nombre : correo.split("@")[0]);
      nuevo.setCorreo(correo);
      nuevo.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
      nuevo.setRol(Rol.USUARIO);
      nuevo.setEstado(Estado.ACTIVO);
      nuevo.setGoogleSub(googleSub);
      nuevo.setAuthProvider("GOOGLE");
      nuevo.setPreferenciasCompletadas(false);
      nuevo.setCategoriasPreferidas(List.of());
      return nuevo;
    });

    if (usuario.getEstado() == Estado.CANCELADO) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "Esta cuenta ya no se encuentra disponible"));
    }

    usuarioRepository.save(usuario);

    List<String> roles = List.of(usuario.getRol().name());
    String token = JwtUtil.generateToken(
      usuario.getIdUsuario(),
      usuario.getCorreo(),
      roles,
      usuario.getNombre()
    );

    return ResponseEntity.ok()
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .body(Map.of(
        "tokenType", "Bearer",
        "token", token,
        "correo", usuario.getCorreo(),
        "rol", usuario.getRol().name(),
        "nombre", usuario.getNombre(),
        "preferenciasCompletadas", usuario.getPreferenciasCompletadas(),
        "categoriasPreferidas", usuario.getCategoriasPreferidas()
      ));
  }
}
