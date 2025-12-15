package com.titan.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class LedgerCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerCoreApplication.class, args);
	}

}
