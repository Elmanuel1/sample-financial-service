package com.spherelabs;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import spock.lang.Specification;

abstract class AbstractSpecification extends Specification {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15")
            .asCompatibleSubstituteFor("postgres"))
            .withUsername("testuser")
            .withPassword("testpass")
            .withDatabaseName("testdb")

    @DynamicPropertySource
    static def registerPgProperties(DynamicPropertyRegistry registry) {
        postgres.start()
        registry.add("spring.datasource.url",
                () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.firstMappedPort, postgres.databaseName))
        registry.add("spring.datasource.username", () -> postgres.username)
        registry.add("spring.datasource.password", () -> postgres.password)
    }
}