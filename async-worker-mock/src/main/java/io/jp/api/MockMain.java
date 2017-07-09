package io.jp.api;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

/**
 * Created by johnnypark on 06/03/2017.
 */
public class MockMain extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.deployVerticle(Dispatcher.class.getName());
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(MockMain.class.getName());
    }
}
