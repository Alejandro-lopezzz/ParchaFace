package com.alejo.parchaface.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

public class JwtUtil {

    // 1 día
    private static final long EXPIRATION_TIME_MS = 24L * 60 * 60 * 1000;

    // Importante: secret fijo (Base64) desde configuración
    private static String SECRET_BASE64;

    // Setter llamado desde configuración (ver clase JwtConfig abajo)
    public static void setSecretBase64(String secretBase64) {
        SECRET_BASE64 = secretBase64;
    }

    private static Key getSigningKey() {
        if (SECRET_BASE64 == null || SECRET_BASE64.isBlank()) {
            throw new IllegalStateException("jwt.secret no está configurado (Base64).");
        }
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_BASE64.trim());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateToken(String correo, List<String> roles) {
        return generateToken(correo, roles, null);
    }

    public static String generateToken(String correo, List<String> roles, String nombre) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles == null ? List.of() : roles);
        if (nombre != null && !nombre.isBlank()) {
            claims.put("nombre", nombre);
        }

        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_TIME_MS);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(correo)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =============================
    // Validar token
    // =============================
    public static boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Token inválido: " + e.getMessage());
            return false;
        }
    }

    // =============================
    // Extraer correo del token
    // =============================
    public static String getCorreoFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // =============================
    // Extraer roles del token
    // =============================
    public static Collection<GrantedAuthority> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);

        Object raw = claims.get("roles");
        if (raw == null) return List.of();

        // Soporta que venga como List<?> (lo normal)
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    // Opcional: si quieres prefijo ROLE_
                    // .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        // Si por alguna razón vino como String "USUARIO,ADMIN"
        String asString = raw.toString();
        if (asString.isBlank()) return List.of();

        return Arrays.stream(asString.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // =============================
    // Parser centralizado
    // =============================
    private static Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token.trim()) // trim por seguridad
                .getBody();
    }

    public static String generateToken(Integer idUsuario, String correo, List<String> roles, String nombre) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", idUsuario); // ✅ AQUI METES EL ID
        claims.put("roles", roles == null ? List.of() : roles);

        if (nombre != null && !nombre.isBlank()) {
            claims.put("nombre", nombre);
        }

        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_TIME_MS);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(correo)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }




}
