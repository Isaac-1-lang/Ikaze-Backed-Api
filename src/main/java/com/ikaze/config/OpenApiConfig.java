package com.ikaze.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ikaze API Documentation")
                        .version("1.0.0")
                        .description("REST API documentation for Ikaze application with authentication and user management")
                        .contact(new Contact()
                                .name("Ikaze Team")
                                .email("support@ikaze.com")
                                .url("https://ikaze.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:3000")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.ikaze.com")
                                .description("Production Server")
                ));
    }
}
