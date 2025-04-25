package com.ll.carjini.global.springDoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        io.swagger.v3.oas.models.security.SecurityScheme securityScheme = new io.swagger.v3.oas.models.security.SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8090");
        localServer.setDescription("carjini 로컬 서버입니다.");

        Server httpsServer = new Server();
        httpsServer.setUrl("https://api.carjini.shop");
        httpsServer.setDescription("carjini 배포 서버입니다.");

        return new OpenAPI()
                .info(new Info().title("CarJini REST API").version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(httpsServer, localServer));
    }
}
