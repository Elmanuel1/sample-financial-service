package com.spherelabs.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
  @Bean
  public org.jooq.Configuration jooqConfiguration() {
    org.jooq.Configuration configuration = new DefaultConfiguration();
    configuration.set(SQLDialect.POSTGRES);
    return configuration;
  }

  @Bean
  public DSLContext dslContext(DataSource dataSource) {
    return DSL.using(dataSource, SQLDialect.POSTGRES);
  }

}
