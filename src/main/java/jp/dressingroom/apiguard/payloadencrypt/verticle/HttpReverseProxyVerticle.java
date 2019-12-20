package jp.dressingroom.apiguard.payloadencrypt.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jp.dressingroom.apiguard.payloadencrypt.ConfigKeyNames;
import jp.dressingroom.apiguard.payloadencrypt.entity.Base64Message;


public class HttpReverseProxyVerticle extends AbstractVerticle {
  WebClient client;
  String proxyHost;
  String proxyUserAgent;
  int proxyPort;
  Boolean proxyUseSsl;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig((json -> {
      try {
        JsonObject result = json.result();

        // setup proxy client
        proxyHost = result.getString(ConfigKeyNames.PAYLOAD_ENCRYPT_PROXY_HOSTNAME.value(), "localhost");
        proxyPort = result.getInteger(ConfigKeyNames.PAYLOAD_ENCRYPT_PROXY_PORT.value(), 8080);
        proxyUserAgent = result.getString(ConfigKeyNames.PAYLOAD_ENCRYPT_PROXY_USERAGENT.value(), "ApiGuard/PayloadEncrypt 1.0");
        proxyUseSsl = result.getBoolean(ConfigKeyNames.PAYLOAD_ENCRYPT_PROXY_USESSL.value(), false);

        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setUserAgent(proxyUserAgent);
        client = WebClient.create((Vertx) vertx, webClientOptions);

        // setup proxy listener
        Router router = Router.router(vertx);
        Route bodyLessRoute = router.route()
          .method(HttpMethod.GET)
          .method(HttpMethod.HEAD)
          .method(HttpMethod.OPTIONS)
          .method(HttpMethod.DELETE);
        bodyLessRoute.handler(bodyLessProxyHandler());

        Route bodiedRoute = router.route()
          .method(HttpMethod.POST)
          .method(HttpMethod.PUT)
          .method(HttpMethod.PATCH);
        bodiedRoute.handler(bodiedProxyHandler());

        Integer port = result.getInteger(ConfigKeyNames.PAYLOAD_ENCRYPT_SERVER_PORT.value());
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(port);

        startPromise.complete();
      } catch (Exception e) {
        startPromise.fail(e);
      }
    }));
  }

  private RequestOptions copyFromRequest(RoutingContext routingContext) {
    RequestOptions requestOptions = new RequestOptions();
    String uri = routingContext.request().uri();
    MultiMap headers = routingContext.request().headers();
    headers.entries().forEach(s -> requestOptions.addHeader(s.getKey(), s.getValue()));
    requestOptions.setHeaders(headers);
    requestOptions.setHost(proxyHost);
    requestOptions.setURI(uri);
    requestOptions.setSsl(proxyUseSsl);
    requestOptions.setPort(proxyPort);

    return requestOptions;
  }

  private Handler<RoutingContext> bodyLessProxyHandler() {
    return routingContext -> {
      HttpMethod method = routingContext.request().method();
      RequestOptions requestOptions = copyFromRequest(routingContext);

      client
        .request(method, requestOptions).ssl(false).send(
        ar -> {
          try {
            if (ar.succeeded()) {
              HttpResponse<Buffer> response = ar.result();
              HttpServerResponse proxyResponse = routingContext.response();

              System.out.println(response.body().toString());

              String plainResponseBody = new String(response.body().getBytes());
              EventBus eventBus = vertx.eventBus();
              Base64Message beforeEncrypt = new Base64Message(plainResponseBody.getBytes());

              eventBus.request(ApiguardEventBusNames.ENCRYPT.value(), beforeEncrypt, encrypt -> {
                if (encrypt.succeeded()) {
                  Base64Message encryptedBase64Message = (Base64Message) encrypt.result().body();
                  //proxyResponse.end(Buffer.buffer(encryptedBase64Message.getValue()));
                  proxyResponse.end(encryptedBase64Message.getValue());
                } else {
                  System.out.println("at 1");
                  sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                }
              });
            } else {
              System.out.println("at 2");
              sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
            }
          } catch (Exception e) {
            e.printStackTrace();;
            routingContext.fail(e);
          }
        }
      );
    };
  }


  private Handler<RoutingContext> bodiedProxyHandler() {
    return routingContext -> {
      EventBus eventBus = vertx.eventBus();

      Buffer buf = routingContext.getBody();
      eventBus.request(ApiguardEventBusNames.DECRYPT.value(), new Base64Message(buf.getBytes()), decrypt -> {
        if (decrypt.succeeded()) {
          Base64Message decryptedRequestBody = (Base64Message) decrypt.result().body();



          HttpMethod method = routingContext.request().method();
          RequestOptions requestOptions = copyFromRequest(routingContext);

          client
            .request(method, requestOptions).ssl(false).sendBuffer( buf,
            ar -> {
              try {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  HttpServerResponse proxyResponse = routingContext.response();

                  Base64Message plainResponseBody = new Base64Message(response.body().getBytes());

                  eventBus.request(ApiguardEventBusNames.ENCRYPT.value(), plainResponseBody, encrypt -> {
                    if (encrypt.succeeded()) {
                      Base64Message encryptedBase64Message = (Base64Message) encrypt.result().body();
                      proxyResponse.end(Buffer.buffer(encryptedBase64Message.getValue()));
                    } else {
                      sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                    }
                  });
                } else {
                  sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                }
              } catch (Exception e) {
                routingContext.fail(e);
              }
            }
          );

        } else {
          sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }

      });



/*
      routingContext.request().bodyHandler(bodiedProxyHandler -> {
          sendResponse(routingContext, HttpStatusCodes.OK, "called bodied with " + routingContext.request().method().name());
        }
      );
*/

/*
      routingContext.request().bodyHandler(bodyHandler -> {
        byte[] body = bodyHandler.getBytes();
        String id = routingContext.request().getParam("id");

        System.out.println("posted id: " + id + " body: " + new String(body));

        // build response body
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "text/plain");
        // Write to the response and end it
        response.end("postApiRoutingHandler received: " + new String(body));
        return;
      });
*/
    };
  }

  /**
   * send response to requester with status
   *
   * @param routingContext
   * @param status
   */
  private void sendResponse(RoutingContext routingContext, HttpStatusCodes status) {
    sendResponse(routingContext, status, null);
  }

  /**
   * @param routingContext
   * @param status
   * @param message
   */
  private void sendResponse(RoutingContext routingContext, HttpStatusCodes status, String message) {
    HttpServerResponse response = routingContext.response();
    response.setStatusCode(status.value());
    if (message == null) {
      response.end();
    } else {
      response.end(message);
    }
  }
}
