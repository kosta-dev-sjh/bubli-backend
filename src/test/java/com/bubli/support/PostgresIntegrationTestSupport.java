package com.bubli.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
		"spring.profiles.active=test",
		"spring.profiles.include=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.flyway.enabled=true",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
		"jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
		"jwt.access-token-expire-ms=1800000",
		"jwt.refresh-token-expire-ms=2592000000",
		"storage.type=local",
		"storage.local.base-path=./build/test-storage"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTestSupport {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
			.withDatabaseName("bubli_test")
			.withUsername("bubli")
			.withPassword("bubli1234");

	@DynamicPropertySource
	static void registerDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}
}
