package com.titan.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableCaching
@SpringBootApplication
public class LedgerCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerCoreApplication.class, args);
		// Teste de conexão "tabajara"
		try (java.net.Socket socket = new java.net.Socket("jaeger", 4317)) {
			System.out.println(">>> CONEXÃO COM JAEGER 4317: SUCESSO!");
		} catch (Exception e) {
			System.err.println(">>> CONEXÃO COM JAEGER FALHOU: " + e.getMessage());
		}
	}

}
