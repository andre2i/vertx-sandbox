package com.example.demo;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class MainVerticle extends AbstractVerticle {
    private static final int KEY_COUNT = 10000;
    private static final String HOST = "localhost";
    private static final int PORT = 433;
    private static final int REDIS_DB = 10;
    private static final String KEY_PREFIX = "chunked:reproducer:";
    private RedisClient redis;

    Future<Void> fillRedis() {
        Future<Void> future = Future.future();
        AtomicInteger keysLeft = new AtomicInteger(KEY_COUNT);
        System.out.println("Fillig Redis");
        redis.select(REDIS_DB, res -> {
            redis.flushdb(flush -> {
                for (int i = 0; i < KEY_COUNT; i++) {
                    redis.set(KEY_PREFIX + Integer.toString(i) + ":" + randomString(), makePayload(), result -> {
                        if (keysLeft.decrementAndGet() == 0) {
                            future.complete();
                        }
                    });
                }
            });
        });

        return future;
    }

    void getJsons(Handler<String> jsonHandler, Handler<Void> endHandler) {
        Future<JsonArray> keysFuture = Future.<JsonArray>future().setHandler(ar -> {
            jsonForKeys(ar.result(), jsonHandler, endHandler);
        });

        redis.keys(KEY_PREFIX + "*", keysFuture.completer());
    }

    void jsonForKeys(JsonArray keys, Handler<String> jsonHandler, Handler<Void> endHandler) {
        int keyCount = keys.size();
        System.out.format("Got %d keys \n", keyCount);
        if (keyCount == 0) {
            endHandler.handle(null);
            return;
        }

        AtomicInteger outstandingGetRequestCount = new AtomicInteger(keyCount);
        keys.stream().map(Object::toString).forEach(key -> {
            jsonForKey(key, ar -> {
                int remainingGets = outstandingGetRequestCount.decrementAndGet();
                if (ar.succeeded())
                    jsonHandler.handle(ar.result());
                if (remainingGets == 0)
                    endHandler.handle(null);
            });
        });
    }

    void jsonForKey(String key, Handler<AsyncResult<String>> handler) {
        redis.get(key, get -> {
            if (get.succeeded()) {
                handler.handle(Future.succeededFuture(get.result()));
            } else {
                handler.handle(Future.failedFuture(get.cause()));
            }
        });
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        redis = RedisClient.create(vertx, new RedisOptions());
        ClassLoader classLoader = getClass().getClassLoader();
        String jksPath = classLoader.getResource("tls/server-keystore.jks").getFile();
        HttpServerOptions serverOptions = new HttpServerOptions().setPort(PORT).setHost(HOST)
                .setKeyCertOptions(new JksOptions().setPath(jksPath).setPassword("wibble")).setSsl(true);

        Future<Void> start = fillRedis().compose(v -> {
            Future<Void> startWebServer = Future.future();
            vertx.createHttpServer().requestHandler(req -> {
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
            }).listen(8080, listen -> {
                startWebServer.complete();
            });

            return startWebServer;
        });
        start.setHandler(startFuture.completer());
        System.out.println("HTTP server started on port " + PORT);
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
        return UUID.randomUUID().toString().substring(random.nextInt(37));
    }
}
