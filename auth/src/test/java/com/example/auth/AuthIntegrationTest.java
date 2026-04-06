package com.example.auth;

import com.example.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterLoginRefreshLogoutAndFetchCurrentUserProfile() throws Exception {
        String registerRequest = """
            {
              "name": "Dung Nguyen",
              "email": "dung@example.com",
              "password": "Password123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value("dung@example.com"))
            .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty());

        String loginRequest = """
            {
              "email": "dung@example.com",
              "password": "Password123"
            }
            """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value("dung@example.com"))
            .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.path("data").path("tokens").path("accessToken").asText();
        String refreshToken = loginJson.path("data").path("tokens").path("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("dung@example.com"));

        String refreshRequest = """
            {
              "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshResponse)
            .path("data")
            .path("tokens")
            .path("refreshToken")
            .asText();

        assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(rotatedRefreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }
}
