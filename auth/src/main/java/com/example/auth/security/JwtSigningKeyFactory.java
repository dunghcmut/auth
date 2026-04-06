package com.example.auth.security;

import com.example.auth.config.JwtProperties;
import com.example.auth.config.JwtSecretFormat;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtSigningKeyFactory {

    private static final int MINIMUM_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    public JwtSigningKeyFactory(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public SecretKey createSigningKey() {
        byte[] keyBytes = resolveKeyBytes();
        validateKeyLength(keyBytes);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveKeyBytes() {
        String secret = jwtProperties.secret().trim();
        JwtSecretFormat secretFormat = jwtProperties.secretFormat();

        return switch (secretFormat) {
            case RAW -> secret.getBytes(StandardCharsets.UTF_8);
            case BASE64 -> decodeSecret(secret, secretFormat, false);
            case BASE64URL -> decodeSecret(secret, secretFormat, true);
        };
    }

    private byte[] decodeSecret(String secret, JwtSecretFormat secretFormat, boolean urlSafe) {
        try {
            return urlSafe ? Decoders.BASE64URL.decode(secret) : Decoders.BASE64.decode(secret);
        } catch (DecodingException exception) {
            throw new IllegalStateException(
                "JWT secret is not valid " + secretFormat + ". Check JWT_SECRET and JWT_SECRET_FORMAT.",
                exception
            );
        }
    }

    private void validateKeyLength(byte[] keyBytes) {
        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            String lengthMessage = "JWT secret must be at least 32 bytes (256 bits). Current length: "
                + keyBytes.length
                + " bytes. Current format: "
                + jwtProperties.secretFormat()
                + ".";

            if (jwtProperties.secretFormat() == JwtSecretFormat.RAW) {
                throw new IllegalStateException(lengthMessage + " RAW is intended for local/dev only.");
            }

            throw new IllegalStateException(lengthMessage + " Generate at least 32 random bytes before encoding.");
        }
    }
}
