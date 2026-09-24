package com.rpe.clientmanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API docs at /v3/api-docs, Swagger UI at /swagger-ui.html (both public, see SecurityConfig). Every operation
 * declares the bearer JWT, so "Authorize" in Swagger UI with the token from POST /api/v1/auth/login is enough.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("rpe-client-manager")
                        .version("v1")
                        .description("Customers (Portador Service). Log in with POST /api/v1/auth/login, then use "
                                + "the accessToken as a bearer token. Creating a customer requests card production."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
