package com.example.auth.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldRejectRawSecretWhenNotExplicitlyAllowed() {
        JwtProperties jwtProperties = new JwtProperties(
            "this_is_a_raw_secret_with_more_than_32_bytes_123456789",
            JwtSecretFormat.RAW,
            false,
            "auth-service-test",
            Duration.ofMinutes(15),
            Duration.ofDays(7)
        );

        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(jwtProperties);

        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("RAW JWT secret format is disabled by default. Use BASE64/BASE64URL for production or set app.jwt.allow-raw-secret=true for local/dev.");
    }
}
