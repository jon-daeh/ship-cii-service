/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.sintef.ocean.maritim.ciiservice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

        @Bean
        public OpenAPI customOpenAPI() throws IOException {

                Contact contact = new Contact()
                                .name("SINTEF Ocean")
                                .email("ocean@sintef.no")
                                .url("https://www.sintef.no/ocean/");
                Info info = new Info().contact(contact);

                Server localServer = new Server()
                                .description("Local server")
                                .url("http://localhost:8080");
                Server devServer = new Server()
                                .description("Development server")
                                .url("https://cii-service-1705926368281.azurewebsites.net");
                List<Server> servers = Arrays.asList(devServer, localServer);

                SecurityScheme securityScheme = new SecurityScheme()
                                .name("X-API-KEY")
                                .description("API key for CII calculator")
                                .in(SecurityScheme.In.HEADER)
                                .type(SecurityScheme.Type.APIKEY);

                return new OpenAPI().info(info).servers(servers).schemaRequirement("ApiKeyAuth", securityScheme);
        }
}
