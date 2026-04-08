package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.service.GoogleTokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleTokenServiceImpl implements GoogleTokenService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenServiceImpl(@Value("${google.client.id}") String googleClientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Override
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Token de Google inválido");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String issuer = payload.getIssuer();
            if (!"accounts.google.com".equals(issuer) &&
                    !"https://accounts.google.com".equals(issuer)) {
                throw new RuntimeException("Issuer inválido");
            }

            return payload;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("No se pudo verificar el token de Google", e);
        }
    }
}