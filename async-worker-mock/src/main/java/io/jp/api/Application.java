package io.jp.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import javax.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  @PostConstruct
  public void deployVerticle() {
    Vertx.vertx().deployVerticle(Dispatcher.class.getName());
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
