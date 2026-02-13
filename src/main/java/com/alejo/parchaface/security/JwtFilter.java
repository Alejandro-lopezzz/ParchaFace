package com.alejo.parchaface.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String sp = request.getServletPath(); // ej: /auth/signin
        String method = request.getMethod();

        // Debug
        System.out.println("[JwtFilter] " + method + " servletPath=" + sp + " uri=" + request.getRequestURI());

        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // IMPORTANTE: evita loop de /error protegido
        if ("/error".equals(sp)) return true;

        return sp.startsWith("/auth/")
                || sp.equals("/auth")
                || sp.startsWith("/swagger-ui")
                || sp.startsWith("/v3/api-docs")
                || sp.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("[JwtFilter] Authorization raw=[" + authHeader + "]");

        // Si no hay header -> deja que Spring Security decida (401 por entrypoint)
        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = authHeader.trim();

        // Normaliza "Bearer:" -> "Bearer "
        if (header.regionMatches(true, 0, "Bearer:", 0, 7)) {
            header = "Bearer " + header.substring(7).trim();
        }

        // Debe ser "Bearer <token>"
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token inválido -> responde 401 y corta
        if (!JwtUtil.validateToken(token)) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
            return;
        }

        // Si no hay auth aún, la seteamos
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String correo = JwtUtil.getCorreoFromToken(token);
            var roles = JwtUtil.getRolesFromToken(token);

            var authentication =
                    new UsernamePasswordAuthenticationToken(correo, null, roles);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
