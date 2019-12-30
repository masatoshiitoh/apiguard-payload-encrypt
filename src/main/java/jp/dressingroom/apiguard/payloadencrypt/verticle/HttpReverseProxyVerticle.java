package jp.dressingroom.apiguard.payloadencrypt.verticle;

import io.netty.handler.codec.base64.Base64Encoder;
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

import java.util.Base64;


public class HttpReverseProxyVerticle extends AbstractVerticle {
  WebClient client;
  String proxyHost;
  String proxyUserAgent;
  int proxyPort;
  Boolean proxyUseSsl;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig(json -> {
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
    });
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
    // stage1:
    // copy request headers from requester to origin
    // stage2:
    // copy response headers from origin to requester

    return requestorContext -> {
      HttpMethod method = requestorContext.request().method();
      RequestOptions requestOptions = copyFromRequest(requestorContext);

      client
        .request(method, requestOptions).ssl(requestOptions.isSsl()).send(
        originResponse -> {

          try {
            HttpServerResponse responseToRequestor = requestorContext.response();
            if (originResponse.succeeded()) {

              // Copy headers
              responseToRequestor.headers().setAll(originResponse.result().headers());

              if (originResponse.result().body() != null) {

                byte[] plainResponseBody = originResponse.result().body().getBytes();

                EventBus eventBus = vertx.eventBus();
                eventBus.request(ApiguardEventBusNames.ENCRYPT.value(), plainResponseBody, encrypt -> {

                  if (encrypt.succeeded()) {
                    byte[] encryptedBytes = (byte[]) encrypt.result().body();

                    responseToRequestor.headers().setAll(
                      originResponse.result().headers().remove("content-length")
                    );

                    responseToRequestor
                      .setStatusCode(originResponse.result().statusCode())
                      .end(Base64.getEncoder().encodeToString(encryptedBytes)); // << HERE
                  } else {
                    responseToRequestor.headers().setAll(originResponse.result().headers());
                    responseToRequestor
                      .setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR.value())
                      .end("Encrypt failed.");
                  }
                });
              } else {
                responseToRequestor.headers().setAll(originResponse.result().headers());
                responseToRequestor
                  .setStatusCode(originResponse.result().statusCode())
                  .end();
              }
            } else {
              System.out.println("0002: ");
              requestorContext.response()
                .setStatusCode(originResponse.result().statusCode())
                .end("Origin request failed.");
            }
          } catch (Exception e) {
            e.printStackTrace();
            requestorContext.fail(e);
          }
        }
      );
    };
  }


  private Handler<RoutingContext> bodiedProxyHandler() {
    //
    // 1. receive encrypted payload
    // 2. decrypt payload and forward request to proxy server.
    // 3. receive response from proxy server.
    // 4. encrypt response
    // 5. return encrypted payload to caller.
    //
    return requestorContext -> {
      requestorContext.request().bodyHandler(body -> {
        HttpMethod method = requestorContext.request().method();
        RequestOptions requestOptions = copyFromRequest(requestorContext);
        // System.out.println("bodiedProxyHandler decryptor: request received as:" + body.toString());
        EventBus eventBus = vertx.eventBus();
        try {
          byte[] decoded = Base64.getDecoder().decode(body.getBytes());
                        eventBus.request(ApiguardEventBusNames.DECRYPT.value(), decoded , decodeReplyHandler -> {
                          if (decodeReplyHandler.succeeded()) {
                            byte[] decrypted = (byte[])decodeReplyHandler.result().body();

                            client
                              .request(method, requestOptions).ssl(requestOptions.isSsl()).sendBuffer(Buffer.buffer(decrypted),
                              originRequest -> {
                                try {
                                  if (originRequest.succeeded()) {
                                    HttpResponse<Buffer> responseFromOrigin = originRequest.result();
                                    HttpServerResponse responseToRequestor = requestorContext.response();
                                    if (originRequest.result().body() != null ) {

                                      responseToRequestor.headers().setAll(
                          responseFromOrigin.headers().remove("content-length")
                        );

                        byte[] beforeEncrypt = originRequest.result().body().getBytes();

                        eventBus.request(ApiguardEventBusNames.ENCRYPT.value(), beforeEncrypt, encrypt -> {
                          if (encrypt.succeeded()) {
                            byte[] encryptedMessage = (byte[]) encrypt.result().body();
                            responseToRequestor.end(Base64.getEncoder().encodeToString(encryptedMessage));
                          } else {
                            sendResponse(requestorContext, HttpStatusCodes.INTERNAL_SERVER_ERROR, "Encrypt failed.");
                          }
                        });
                      } else {
                        responseToRequestor.headers().setAll(responseFromOrigin.headers());
                        responseToRequestor
                          .setStatusCode(originRequest.result().statusCode())
                          .end();
                      }
                    } else {
                      sendResponse(requestorContext, HttpStatusCodes.INTERNAL_SERVER_ERROR, "Origin request failed.");
                    }
                  } catch (Exception e) {
                    requestorContext.response()
                      .setStatusCode(originRequest.result().statusCode())
                      .end();
                    e.printStackTrace();
                    requestorContext.fail(e);
                  }
                }
              );
            } else {
              sendResponse(requestorContext, HttpStatusCodes.INTERNAL_SERVER_ERROR, "request payload decrypt failed.");
            }
          });
        } catch (IllegalArgumentException e) {
          // base64 decode failed.
          sendResponse(requestorContext, HttpStatusCodes.BAD_REQUEST, "Payload decrypt failed.");
        }
      });
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
