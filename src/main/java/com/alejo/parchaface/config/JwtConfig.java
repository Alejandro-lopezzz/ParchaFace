package com.alejo.parchaface.config;

import com.alejo.parchaface.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:}")
    private String jwtSecretBase64;

    @PostConstruct
    public void init() {
        // Esto es lo que te faltaba: setear el SECRET_BASE64 estático
        JwtUtil.setSecretBase64(jwtSecretBase64);
        System.out.println(">>> JwtConfig LOADED (jwt.secret length=" + (jwtSecretBase64 == null ? 0 : jwtSecretBase64.trim().length()) + ")");
    }
}
