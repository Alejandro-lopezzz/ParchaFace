package com.alejo.parchaface.service;

public interface PasswordResetService {
    void enviarCodigo(String correo);
    String restablecer(String correo, String codigo, String nuevaContrasena); // retorna token opcional
}
