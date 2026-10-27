package org.ouracademy.ovejafiel.shared;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info =
        @Info( //
            title = "API Ovejafiel", //
            description = "Contiene las API REST para Ovejafiel",
            contact =
                @Contact( //
                    email = "dev@ouracademy.org", //
                    name = "Equipo de desarrollo" //
                    )), //
    servers = { //
      @Server(url = "http://localhost:8080", description = "Local"),
      @Server(url = "https://api.srtommy.com", description = "Production"),
    })
public class OpenApiConfiguration {}
