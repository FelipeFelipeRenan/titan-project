package com.titan.ledger.adapter.in.web.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Titan Ledger API", version = "1.0", description = "Core Banking Ledger with Pessimistic Lock and Idempotency"))
public class OpenApiConfig {

}
