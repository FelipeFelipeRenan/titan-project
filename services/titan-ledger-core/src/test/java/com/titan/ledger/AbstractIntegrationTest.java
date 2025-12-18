package com.titan.ledger;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
// [CORREÇÃO] Removemos @Testcontainers para evitar que o JUnit reinicie o banco a cada teste
public abstract class AbstractIntegrationTest {

    // Instanciamos os containers como static final
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("titan_test")
            .withUsername("test")
            .withPassword("test");

    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    // [CORREÇÃO] Iniciamos manualmente para garantir que rodem como Singleton (uma vez por bateria de testes)
    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}