package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.PasswordResetCode;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.repository.PasswordResetCodeRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.security.JwtUtil;
import com.alejo.parchaface.service.PasswordResetService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetCodeRepository codeRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;

    public PasswordResetServiceImpl(PasswordResetCodeRepository codeRepo,
                                    UsuarioRepository usuarioRepo,
                                    PasswordEncoder encoder,
                                    JavaMailSender mailSender) {
        this.codeRepo = codeRepo;
        this.usuarioRepo = usuarioRepo;
        this.encoder = encoder;
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCodigo(String correo) {
        String email = normalizeEmail(correo);

        // Por seguridad: aunque no exista, respondemos "ok"
        var opt = usuarioRepo.findByCorreo(email);
        if (opt.isEmpty()) return;

        String codigo = generarCodigo6();

        PasswordResetCode prc = new PasswordResetCode();
        prc.setCorreo(email);
        prc.setCodigoHash(encoder.encode(codigo));
        prc.setExpiraEn(LocalDateTime.now().plusMinutes(10));
        codeRepo.save(prc);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Código de restablecimiento - ParchaFace");
        msg.setText("Tu código es: " + codigo + "\nVence en 10 minutos.");
        mailSender.send(msg);
    }

    @Override
    public String restablecer(String correo, String codigo, String nuevaContrasena) {
        String email = normalizeEmail(correo);

        PasswordResetCode prc = codeRepo.findTopByCorreoOrderByCreadoEnDesc(email)
                .orElseThrow(() -> new RuntimeException("Código inválido"));

        if (prc.isUsado()) throw new RuntimeException("Código ya usado");
        if (LocalDateTime.now().isAfter(prc.getExpiraEn())) throw new RuntimeException("Código expirado");
        if (!encoder.matches(codigo, prc.getCodigoHash())) throw new RuntimeException("Código inválido");

        Usuario usuario = usuarioRepo.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        usuario.setContrasena(encoder.encode(nuevaContrasena));
        usuarioRepo.save(usuario);

        prc.setUsado(true);
        codeRepo.save(prc);

        // ✅ OPCIONAL: devolver token para que quede logueado
        List<String> roles = List.of(usuario.getRol().name());
        return JwtUtil.generateToken(usuario.getCorreo(), roles);
    }

    private String generarCodigo6() {
        int n = (int)(Math.random() * 900000) + 100000; // 100000-999999
        return String.valueOf(n);
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}
