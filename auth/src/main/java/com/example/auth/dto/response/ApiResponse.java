package com.example.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Map<String, String> errors,
    OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static ApiResponse<Void> error(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, errors, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
