package com.scarlxrd.catalog_service.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${app.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .servers(buildServers(gatewayUrl))
                .info(buildInfo())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildComponents())
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do projeto")
                        .url("https://github.com/Sc4rlxrd/Catalog-Service"));
    }

    private List<Server> buildServers(String gatewayUrl) {
        return List.of(
                new Server()
                        .url(gatewayUrl)
                        .description("Gateway")
        );
    }

    private Info buildInfo() {
        return new Info()
                .title("Catalog-Service API")
                .description("""
                        Serviço responsável pelo gerenciamento do catálogo de livros do BookCommerce.
                        
                        **Endpoints disponíveis:**
                        - Cadastro de livros
                        - Listagem paginada
                        - Busca por título e autor
                        - Filtro por faixa de preço
                        - Controle de estoque
                        - Favoritos do usuário
                        
                        **Comunicação via RabbitMQ:**
                        - Consome: `book.validate.queue` → valida disponibilidade e preço
                        - Publica: `book.validated` → retorna resultado ao order-service
                        """)
                .version("v1")
                .contact(new Contact()
                        .name("Scarlxrd")
                        .url("https://github.com/Sc4rlxrd")
                        .email("contato@exemplo.com"));
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}