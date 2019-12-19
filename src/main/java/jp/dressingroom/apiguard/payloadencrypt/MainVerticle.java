package jp.dressingroom.apiguard.payloadencrypt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;


public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.deployVerticle("jp.dressingroom.apiguard.payloadencrypt.verticle.HttpReverseProxyVerticle", res -> {
      if (res.failed()) {
        startPromise.fail("HttpReverseProxyVerticle start failed: " + res.cause());
      } else {
        vertx.deployVerticle("jp.dressingroom.apiguard.payloadencrypt.verticle.CryptoVerticle", res2 -> {
          if (res2.failed()) {
            startPromise.fail("CryptoVerticle start failed: " + res2.cause());
          } else {
            startPromise.complete();
          }
        });
      }
    });
  }
}
