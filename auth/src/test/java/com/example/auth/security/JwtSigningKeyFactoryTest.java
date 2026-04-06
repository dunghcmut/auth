package com.example.auth.security;

import com.example.auth.config.JwtProperties;
import com.example.auth.config.JwtSecretFormat;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSigningKeyFactoryTest {

    @Test
    void shouldCreateSigningKeyFromBase64Secret() {
        String base64Secret = Base64.getEncoder()
            .encodeToString("this-is-a-valid-base64-secret-with-more-than-32-bytes".getBytes(StandardCharsets.UTF_8));

        SecretKey signingKey = buildFactory(
            new JwtProperties(base64Secret, JwtSecretFormat.BASE64, false, "auth-service-test", Duration.ofMinutes(15), Duration.ofDays(7))
        ).createSigningKey();

        assertThat(signingKey).isNotNull();
    }

    @Test
    void shouldCreateSigningKeyFromBase64UrlSecret() {
        String base64UrlSecret = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("this-is-a-valid-base64url-secret-with-more-than-32-bytes".getBytes(StandardCharsets.UTF_8));

        SecretKey signingKey = buildFactory(
            new JwtProperties(base64UrlSecret, JwtSecretFormat.BASE64URL, false, "auth-service-test", Duration.ofMinutes(15), Duration.ofDays(7))
        ).createSigningKey();

        assertThat(signingKey).isNotNull();
    }

    @Test
    void shouldCreateSigningKeyFromRawSecretForLocalDev() {
        SecretKey signingKey = buildFactory(
            new JwtProperties(
                "this_is_a_local_dev_raw_secret_with_more_than_32_bytes_123456789",
                JwtSecretFormat.RAW,
                true,
                "auth-service-test",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
            )
        ).createSigningKey();

        assertThat(signingKey).isNotNull();
    }

    @Test
    void shouldFailForInvalidBase64Secret() {
        JwtSigningKeyFactory jwtSigningKeyFactory = buildFactory(
            new JwtProperties("invalid_base64_secret", JwtSecretFormat.BASE64, false, "auth-service-test", Duration.ofMinutes(15), Duration.ofDays(7))
        );

        assertThatThrownBy(jwtSigningKeyFactory::createSigningKey)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not valid BASE64");
    }

    @Test
    void shouldFailForShortSecret() {
        JwtSigningKeyFactory jwtSigningKeyFactory = buildFactory(
            new JwtProperties("short-secret", JwtSecretFormat.RAW, true, "auth-service-test", Duration.ofMinutes(15), Duration.ofDays(7))
        );

        assertThatThrownBy(jwtSigningKeyFactory::createSigningKey)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 bytes");
    }

    private JwtSigningKeyFactory buildFactory(JwtProperties jwtProperties) {
        return new JwtSigningKeyFactory(jwtProperties);
    }
}
