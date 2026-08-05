package com.vigilai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds a JWT "Authorize" button to Swagger UI so protected endpoints
 * can be tested directly from the browser: register/login via
 * /api/auth, paste the accessToken into Authorize, then call anything else.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI vigilAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vigil AI API")
                        .description("Accountability app backend — auth, workspaces, projects, tasks, files, notifications, analytics.")
                        .version("v0.3.0 (Stage 3)")
                        .contact(new Contact().name("Vigil AI")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
