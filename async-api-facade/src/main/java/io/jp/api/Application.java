package io.jp.api;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.eventbus.EventBusOptions;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by johnnypark on 07.07.17.
 */
@SpringBootApplication
public class Application {

  @Autowired
  AppConfiguration configuration;

  @Autowired
  HttpApi api;

  @Autowired
  Dispatcher dispatcher;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @PostConstruct
  public void deployVerticle() {

    Vertx.clusteredVertx(new VertxOptions(), res -> {
      Vertx vertx = res.result();
      vertx.deployVerticle(api, deploy -> {
        if (deploy.failed()) {
          vertx.close();
        }
      });
      vertx.deployVerticle(dispatcher);
    });
  }

}
