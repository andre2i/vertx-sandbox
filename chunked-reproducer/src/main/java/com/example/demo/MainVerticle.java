package com.example.demo;

import java.net.HttpURLConnection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;

public class MainVerticle extends AbstractVerticle {
    private static final int KEY_COUNT = 10000;

    void getJsons(Handler<String> jsonHandler, Handler<Void> endHandler) {
        AtomicInteger outstandingGetRequestCount = new AtomicInteger(KEY_COUNT);
        for (int i = 0; i < KEY_COUNT; i++) {
            json(ar -> {
                int remainingGets = outstandingGetRequestCount.decrementAndGet();
                if (ar.succeeded())
                    jsonHandler.handle(ar.result());
                if (remainingGets == 0)
                    endHandler.handle(null);
            });
        }
    }

    void json(Handler<AsyncResult<String>> handler) {
        handler.handle(Future.succeededFuture(makePayload()));
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String jksPath = classLoader.getResource("tls/server-keystore.jks").getFile();
        HttpServerOptions serverOptions = new HttpServerOptions().setSsl(true)
                .setKeyCertOptions(new JksOptions().setPath(jksPath).setPassword("wibble"));

        vertx.createHttpServer(serverOptions).requestHandler(req -> {
            System.out.println("Received a request");

            HttpServerResponse response = req.response().setChunked(true);
            AtomicBoolean fresh = new AtomicBoolean(true);
            response.write("[");

            Handler<String> jsonHandler = json -> {
                if (fresh.get())
                    fresh.set(false);
                else
                    response.write(",");
                response.write(json);
            };

            Handler<Void> endHandler = end -> {
                response.write("]");
                response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setStatusCode(HttpURLConnection.HTTP_OK).end();
            };

            getJsons(jsonHandler, endHandler);
        }).exceptionHandler(exception -> {
            System.out.println("Server error >> " + exception);
        }).listen(8080);

        System.out.println("HTTP server started on port 8080");
    }

    private static Random random = new Random();

    private String makePayload() {
        return makePayload(random.nextInt(2) + 1).encode();
    }

    private JsonObject makePayload(int depth) {
        JsonObject root = randomJson();
        if (depth == 0)
            return root;
        int width = random.nextInt(20) + 1;
        for (int i = 1; i < width; i++) {
            root.put(randomString(), makePayload(depth - 1));
        }

        return root;
    }

    private JsonObject randomJson() {
        return new JsonObject().put(randomString(), randomString());
    }

    private String randomString() {
        StringBuilder sb = new StringBuilder();
        String candidateChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        int length = random.nextInt(50) + 1;
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }

        return sb.toString();
    }
}
