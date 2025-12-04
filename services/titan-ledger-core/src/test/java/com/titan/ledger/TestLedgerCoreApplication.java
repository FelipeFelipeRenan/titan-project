package com.titan.ledger;

import org.springframework.boot.SpringApplication;

public class TestLedgerCoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(LedgerCoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
