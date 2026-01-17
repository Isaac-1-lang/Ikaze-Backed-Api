package com.ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;

/**
 * @author Isaac-1-lang
 * @version 1.0
 * @since 2026-01-17
 * OpenAPI configuration for the application.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Ikaze-Backend API", version = "1.0", description = "API for Ikaze platform", contact = @Contact(name = "Isaac-1-lang", url = "https://github.com/Isaac-1-lang/Ikaze-Backed-Api")), security = {
                @SecurityRequirement(name = "bearerAuth")
})
@SecuritySchemes({
                @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
})
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new io.swagger.v3.oas.models.info.Info()
                                                .title("Ikaze Backend API")
                                                .version("1.0")
                                                .description("API for Inter-commerce platform")
                                                .license(new License().name("Apache 2.0")
                                                                .url("https://springdoc.org")));
        }
}