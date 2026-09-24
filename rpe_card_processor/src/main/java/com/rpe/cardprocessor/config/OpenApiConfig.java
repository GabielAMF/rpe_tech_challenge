package com.rpe.cardprocessor.config;

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
                .title("rpe-card-processor")
                .version("v1")
                .description("Cards (Cartão Service). Cards are produced from rpe-client-manager's SQS messages; "
                        + "this API only reads them. Internal: no authentication."));
    }
}
