package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.PasswordResetCode;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.repository.PasswordResetCodeRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.security.JwtUtil;
import com.alejo.parchaface.service.PasswordResetService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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

        // ✅ Intentar enviar email bonito en HTML con logo embebido (CID)
        try {
            sendResetEmailHtml(email, codigo, 10);
            return;
        } catch (Exception e) {
            // Si falla por cualquier cosa, caemos al correo básico (como lo tenías)
        }

        // ✅ Fallback: correo básico
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

        List<String> roles = List.of(usuario.getRol().name());
        return JwtUtil.generateToken(usuario.getCorreo(), roles);
    }

    private String generarCodigo6() {
        int n = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(n);
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    // =========================
    // ✅ Email HTML + Logo
    // =========================

    private void sendResetEmailHtml(String to, String codigo, int minutes) throws Exception {
        String html = buildResetHtml(codigo, minutes);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(to);
        helper.setSubject("Código de restablecimiento - ParchaFace");
        helper.setText(html, true);

        // Debe existir en: src/main/resources/static/email/logo.png
        ClassPathResource logo = new ClassPathResource("static/email/logo.png");
        if (logo.exists()) {
            // ✅ CID que coincide con el HTML: cid:logo
            helper.addInline("logo", logo, "image/png");
        }

        mailSender.send(message);
    }

    private String buildResetHtml(String codigo, int minutes) {
        return """
<!doctype html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <title>Código de restablecimiento</title>
</head>

<body style="margin:0;padding:0;background:#fbf4ea;font-family:Arial,Helvetica,sans-serif;">
  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#fbf4ea;padding:26px 12px;">
    <tr>
      <td align="center">

        <table role="presentation" width="600" cellpadding="0" cellspacing="0"
               style="max-width:600px;width:100%%;background:#ffffff;border-radius:22px;overflow:hidden;
                      box-shadow:0 18px 50px rgba(15, 23, 42, 0.12);
                      border:1px solid rgba(234,219,191,0.7);">

          <tr>
            <td style="padding:22px 22px 10px 22px;background:linear-gradient(180deg,#fff7ea 0%%, #ffffff 75%%);">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center">
                    <div style="width:96px;height:96px;background:#f6ead2;border-radius:18px;
                                display:inline-block;box-shadow:0 10px 30px rgba(199,146,43,0.18);
                                border:1px solid rgba(199,146,43,0.18);padding:10px;box-sizing:border-box;">
                      <img src="cid:logo" alt="ParchaFace"
                           style="width:76px;height:76px;object-fit:contain;display:block;margin:0 auto;">
                    </div>
                  </td>
                </tr>

                <tr>
                  <td align="center" style="padding-top:14px;">
                    <div style="font-size:28px;line-height:1.15;font-weight:800;color:#c7922b;letter-spacing:-0.02em;">
                      Verifica tu código
                    </div>
                    <div style="margin-top:6px;color:#6b7280;font-size:14px;line-height:1.6;">
                      Usa este código para restablecer tu contraseña en <b style="color:#1f2937;">ParchaFace</b>.
                    </div>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <tr>
            <td style="padding:18px 24px 10px 24px;color:#1f2937;">
              <div style="background:#fff7ea;border:1px solid #eadbbf;border-radius:16px;padding:16px;">
                <div style="color:#6b7280;font-size:12px;margin-bottom:8px;">
                  Tu código (válido por <b style="color:#1f2937;">%d min</b>)
                </div>

                <div style="background:#ffffff;border:1px solid #eadbbf;border-radius:14px;padding:16px;text-align:center;">
                  <div style="font-size:34px;letter-spacing:8px;font-weight:900;color:#1f2937;">
                    %s
                  </div>
                </div>

                <div style="margin-top:12px;color:#6b7280;font-size:12px;line-height:1.6;">
                  Si tú no solicitaste este cambio, ignora este correo. Tu cuenta seguirá segura.
                </div>
              </div>
            </td>
          </tr>

          <tr>
            <td style="padding:16px 24px 22px 24px;">
              <div style="border-top:1px solid #f1e5cf;padding-top:14px;color:#9ca3af;font-size:12px;line-height:1.6;text-align:center;">
                © ParchaFace · Correo automático, por favor no respondas.
              </div>
            </td>
          </tr>

        </table>

        <div style="height:14px;"></div>

        <div style="max-width:600px;color:#9ca3af;font-size:11px;line-height:1.5;text-align:center;">
          Este mensaje fue enviado para proteger tu cuenta.
        </div>

      </td>
    </tr>
  </table>
</body>
</html>
""".formatted(minutes, codigo);
    }
}