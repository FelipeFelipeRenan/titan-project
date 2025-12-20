package com.titan.ledger.adapter.in.web.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracerDebug {

    @Bean
    public CommandLineRunner checkTracer(ApplicationContext ctx) {
        return args -> {
            System.out.println("========================================");
            System.out.println("🔍 DIAGNÓSTICO DO TRACING (OpenTelemetry)");
            
            boolean hasTracer = ctx.containsBean("tracer"); // Bean genérico do Micrometer
            boolean hasOtel = ctx.containsBean("otelTracer"); // Bean específico do OTel
            
            System.out.println("Existe bean 'tracer'? " + hasTracer);
            System.out.println("Existe bean 'otelTracer'? " + hasOtel);
            
            if (hasTracer) {
                Object tracer = ctx.getBean("tracer");
                System.out.println("Classe do Tracer Ativo: " + tracer.getClass().getName());
            } else {
                System.out.println("❌ O Spring NÃO carregou o Tracer!");
            }

            // Verifica se o exportador do Zipkin foi carregado
            boolean hasZipkinExporter = ctx.containsBean("zipkinSpanExporter");
            System.out.println("Exportador Zipkin Ativo? " + hasZipkinExporter);
            
            System.out.println("========================================");
        };
    }
}