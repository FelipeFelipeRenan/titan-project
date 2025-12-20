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
            System.out.println("🔍 DIAGNÓSTICO DO TRACING");
            
            boolean hasTracer = ctx.containsBean("tracer");
            boolean hasBrave = ctx.containsBean("braveTracer");
            
            System.out.println("Existe bean 'tracer'? " + hasTracer);
            System.out.println("Existe bean 'braveTracer'? " + hasBrave);
            
            if (hasTracer) {
                Object tracer = ctx.getBean("tracer");
                System.out.println("Classe do Tracer: " + tracer.getClass().getName());
            } else {
                System.out.println("❌ O Spring NÃO carregou nenhum Tracer!");
                System.out.println("Possíveis causas: Dependência faltando ou Versão incompatível.");
            }
            System.out.println("========================================");
        };
    }
}