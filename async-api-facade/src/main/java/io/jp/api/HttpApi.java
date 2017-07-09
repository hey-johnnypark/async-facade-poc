package io.jp.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HttpApi extends AbstractVerticle {

  @Autowired
  private AppConfiguration appConfiguration;

  private static Logger LOG = LoggerFactory.getLogger(HttpApi.class);

  private Optional<HttpServer> server = Optional.empty();

  private Map<String, RoutingContext> mapping = new HashMap<>();

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);
    router.route("/").handler(this::processRequest);
    server = Optional.of(getVertx().createHttpServer().requestHandler(router::accept));
    server.get().listen(appConfiguration.httpPort(), res -> {
      if (res.succeeded()) {
        startFuture.complete();
        LOG.info("Started and listening to {}", appConfiguration.httpPort());
      } else {
        startFuture.fail(res.cause());
      }
    });
    vertx.eventBus().localConsumer(Constants.EB_ADDRESS_API).handler(this::handleAsyncResponse);
  }

  private void processRequest(RoutingContext ctx) {
    String uid = UUID.randomUUID().toString();
    mapping.put(uid, ctx);
    closeAfterTime(uid, 1000);
    String name = ctx.request().getParam("name");
    vertx.eventBus().send(Constants.EB_ADDRESS_DISPATCHER,
        new JsonObject().put("id", uid).put("name", name)
    );
  }

  private void closeAfterTime(final String uid, int i) {
    vertx.setTimer(i, e -> {
      Optional
          .ofNullable(mapping.remove(uid))
          .ifPresent(this::logAndClose);
    });
  }

  private void logAndClose(RoutingContext ctx) {
    LOG.info("Close request due to no response");
    ctx.response().setStatusCode(500).end();
  }

  private void handleAsyncResponse(Message<Object> response) {
    JsonObject obj = ((JsonObject) response.body());
    Optional
        .ofNullable(mapping.remove(obj.getString("id")))
        .ifPresent(ctx ->
        {
          LOG.info("Send {} to client", obj);
          ctx.response().end(obj.toString());
        });
  }

  public AppConfiguration getAppConfiguration() {
    return appConfiguration;
  }

  public void setAppConfiguration(AppConfiguration appConfiguration) {
    this.appConfiguration = appConfiguration;
  }

  @Override
  public void stop() throws Exception {
    server.ifPresent(server -> server.close());
  }
}
