package com.shadowbring.aggregator;

import com.shadowbring.aggregator.listener.StockListener;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * Standard entry point for any Spring Boot Application
 *
 * @author Dmytro Bezruk
 */
@SpringBootApplication
public class StockAggregatorApplication {

  /**
   * {@link Vertx} instance to run the verticle
   */
  @Autowired
  private Vertx vertx;

  /**
   * Verticle to be deployed
   */
  @Autowired
  private StockListener stockListener;

  /**
   * Application start
   *
   * @param args - command line args
   */
  public static void main(String[] args) {
    SpringApplication.run(StockAggregatorApplication.class, args);
  }

  /**
   * Deployment of the verticle
   */
  @PostConstruct
  public void deployVerticles() {
    vertx.deployVerticle(stockListener);
  }
}
