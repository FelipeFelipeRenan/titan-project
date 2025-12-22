package com.titan.ledger.adapter.in.web.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

@Configuration
public class ObservabilityConfig {

    @Value("${management.otlp.tracing.endpoint:http://jaeger:4317}")
    private String jaegerEndpoint;

    @Bean
    public OtlpGrpcSpanExporter otlpHttpSpanExporter() {
        // Log para provar que o bean subiu
        System.out.println(">>> FORÇANDO INICIALIZAÇÃO DO EXPORTADOR JAEGER (gRPC) PARA: " + jaegerEndpoint);
        
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(jaegerEndpoint)
                .setTimeout(Duration.ofSeconds(30)) // Timeout generoso
                .build();
    }
}