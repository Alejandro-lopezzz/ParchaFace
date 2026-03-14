package com.alejo.parchaface.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtFilter jwtFilter;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  public SecurityConfig(
    JwtFilter jwtFilter,
    AuthenticationEntryPoint authenticationEntryPoint
  ) {
    this.jwtFilter = jwtFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .httpBasic(httpBasic -> httpBasic.disable())
      .formLogin(form -> form.disable())
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(authenticationEntryPoint)
      )
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/",
          "/index.html",
          "/favicon.ico",
          "/error",
          "/assets/**",
          "/uploads/**",
          "/*.js",
          "/*.css",
          "/*.png",
          "/*.jpg",
          "/*.jpeg",
          "/*.svg",
          "/*.webp",
          "/*.ico"
        ).permitAll()

        .requestMatchers(
          "/community",
          "/community/**",
          "/explore",
          "/explore/**",
          "/login",
          "/register",
          "/perfil",
          "/perfil/**",
          "/event-detail",
          "/event-detail/**"
        ).permitAll()

        .requestMatchers("/auth/**").permitAll()

        .requestMatchers(
          "/swagger-ui.html",
          "/swagger-ui/**",
          "/v3/api-docs/**"
        ).permitAll()

        .requestMatchers(HttpMethod.GET, "/api/community/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/eventos/public/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/eventos/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/comentarios-evento/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/clima/**").permitAll()

        .requestMatchers(HttpMethod.POST, "/api/community/**").authenticated()
        .requestMatchers(HttpMethod.PUT, "/api/community/**").authenticated()
        .requestMatchers(HttpMethod.DELETE, "/api/community/**").authenticated()

        .requestMatchers("/perfil/**").authenticated()
        .requestMatchers("/inscripciones/**").authenticated()
        .requestMatchers("/notificaciones/**").authenticated()
        .requestMatchers("/comentarios-evento/**").authenticated()

        .requestMatchers(HttpMethod.POST, "/eventos/**").authenticated()
        .requestMatchers(HttpMethod.PUT, "/eventos/**").authenticated()
        .requestMatchers(HttpMethod.DELETE, "/eventos/**").authenticated()

        .anyRequest().authenticated()
      )
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOriginPatterns(List.of(
      "http://localhost:4200",
      "http://127.0.0.1:4200"
    ));

    config.setAllowedMethods(Arrays.asList(
      "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));

    config.setAllowedHeaders(Arrays.asList(
      "Authorization",
      "Content-Type",
      "Accept",
      "Origin",
      "X-Requested-With"
    ));

    config.setExposedHeaders(List.of("Authorization"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(
    AuthenticationConfiguration authenticationConfiguration
  ) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
