package com.example.auth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    @NotBlank(message = "JWT secret must be provided via app.jwt.secret or JWT_SECRET.")
    String secret,
    @NotNull
    @DefaultValue("BASE64")
    JwtSecretFormat secretFormat,
    @DefaultValue("false")
    boolean allowRawSecret,
    @NotBlank(message = "JWT issuer must not be blank.")
    String issuer,
    @NotNull Duration accessTokenExpiration,
    @NotNull Duration refreshTokenExpiration
) {

    @AssertTrue(message = "RAW JWT secret format is disabled by default. Use BASE64/BASE64URL for production or set app.jwt.allow-raw-secret=true for local/dev.")
    public boolean isRawSecretPolicyValid() {
        return secretFormat != JwtSecretFormat.RAW || allowRawSecret;
    }
}
