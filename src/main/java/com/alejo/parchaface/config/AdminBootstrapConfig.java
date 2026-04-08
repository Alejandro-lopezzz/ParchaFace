package com.alejo.parchaface.config;

import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.model.enums.Rol;
import com.alejo.parchaface.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class AdminBootstrapConfig {

  @Bean
  CommandLineRunner seedAdminUsuario(
    UsuarioRepository usuarioRepository,
    PasswordEncoder passwordEncoder,
    @Value("${app.admin.nombre:Administrador General}") String nombreAdmin,
    @Value("${app.admin.correo:admin@parchaface.com}") String correoAdmin,
    @Value("${app.admin.contrasena:Admin123*}") String contrasenaAdmin
  ) {
    return args -> {
      String correoNormalizado = correoAdmin.trim().toLowerCase();

      Usuario admin = usuarioRepository.findByCorreo(correoNormalizado).orElseGet(Usuario::new);
      admin.setNombre(nombreAdmin);
      admin.setCorreo(correoNormalizado);
      admin.setContrasena(passwordEncoder.encode(contrasenaAdmin));
      admin.setRol(Rol.ADMINISTRADOR);
      admin.setEstado(Estado.ACTIVO);
      admin.setPreferenciasCompletadas(true);
      admin.setCategoriasPreferidas(List.of());
      if (admin.getAuthProvider() == null || admin.getAuthProvider().isBlank()) {
        admin.setAuthProvider("LOCAL");
      }

      usuarioRepository.save(admin);
    };
  }
}
