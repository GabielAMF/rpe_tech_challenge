package com.rpe.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API docs at /v3/api-docs, Swagger UI at /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("rpe-catalog")
                .version("v1")
                .description("Card products (Produto Service). A product is ATIVO or CANCELADO; DELETE is a soft "
                        + "delete to CANCELADO and POST /{id}/activate reactivates it."));
    }
}
