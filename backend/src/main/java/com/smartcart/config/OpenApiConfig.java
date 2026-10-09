package com.smartcart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    public static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI smartCartOpenApi() {
        return new OpenAPI()
                .info(new Info().title("SmartCart API").version("1.0.0")
                        .description("E-commerce order and inventory management API. "
                                + "Log in via POST /api/auth/login, then click Authorize and paste the token."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
