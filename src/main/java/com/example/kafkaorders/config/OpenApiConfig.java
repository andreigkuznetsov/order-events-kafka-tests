package com.example.kafkaorders.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kafkaOrdersOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kafka Order Processing Service API")
                        .description("REST API for publishing order events to Kafka")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Andrey Kuznetsov")
                                .email("andreikuz@yahoo.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project repository")
                        .url("https://github.com/andreigkuznetsov/order-events-kafka-tests"));
    }
}
