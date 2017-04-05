package com.shadowbring.aggregator.configuration;

import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Java-based part of the application context's configuration
 *
 * @author Dmytro Bezruk
 */
@Configuration
public class ApplicationConfiguration {

  /**
   * Bean that provides Vert.x instance
   *
   * @return - {@link Vertx} instance
   */
  @Bean
  public Vertx vertx() {
    return Vertx.vertx();
  }
}
