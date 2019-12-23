package jp.dressingroom.apiguard.payloadencrypt;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.dressingroom.apiguard.httpresponder.HttpResponderMainVerticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {

    System.setProperty("server.port","18888");
    System.setProperty("payloadencrypt.server.port","18889");
    System.setProperty("payloadencrypt.proxy.port","18888");
    System.setProperty("payloadencrypt.iv.base64","MDEyMzQ1Njc4OWFiY2RlZg==");
    System.setProperty("payloadencrypt.psk.base64","MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

    vertx.deployVerticle(new HttpResponderMainVerticle(), testContext.succeeding(id->testContext.completeNow()));
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void checkHttpResponderDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18888, "localhost", "/hello")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.body().equals("Hello"));
        testContext.completeNow();
      })));
  }

  @Test
  void paylodEncryptDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18889, "localhost", "/")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 200);
        testContext.completeNow();
      })));
  }

  @Test
  void paylodEncryptGetHelloResponse(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18889, "localhost", "/hello")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.body().equals("NovdGON73TfXGIEY6h2GwQ==")); // this Base64 encoded string is "Hello".
        testContext.completeNow();
      })));
  }

  @Test
  void httpResponderGet404Response(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18889, "localhost", "/404")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 404);
        testContext.completeNow();
      })));
  }

  @Test
  void httpResponderGet404Response0(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18888, "localhost", "/404")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 404);
        testContext.completeNow();
      })));
  }

  @Test
  void httpResponderGet500Response(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18889, "localhost", "/500")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 500);
        testContext.completeNow();
      })));
  }
  @Test
  void httpResponderPost404Response(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.post(18889, "localhost", "/404")
      .as(BodyCodec.string())
      .sendBuffer(Buffer.buffer("NovdGON73TfXGIEY6h2GwQ=="),
        testContext.succeeding(response -> testContext.verify(() -> {
          assertTrue(response.statusCode() == 404);
          testContext.completeNow();
        })));
  }
  @Test
  void httpResponderPost500Response(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.post(18889, "localhost", "/500")
      .as(BodyCodec.string())
      .sendBuffer(Buffer.buffer("NovdGON73TfXGIEY6h2GwQ=="),
        testContext.succeeding(response -> testContext.verify(() -> {
          assertTrue(response.statusCode() == 500);
          testContext.completeNow();
        })));
  }

}
