package com.example.demo;

import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class MainVerticle extends AbstractVerticle {
    private static final int KEY_COUNT = 10000;

    Future<Void> fillRedis() {
        Future<Void> future = Future.future();
        AtomicInteger keysLeft = new AtomicInteger(KEY_COUNT);
        RedisClient redis = RedisClient.create(vertx, new RedisOptions());
        System.out.println("Fillig Redis");
        for (int i = 0; i < KEY_COUNT; i++) {
            redis.set(Integer.toString(i), "value", result -> {
                int left = keysLeft.decrementAndGet();
                if (left == 0) {
                    redis.close(result2 -> {
                        future.complete();
                    });
                }
            });
        }

        return future;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Future<Void> start = fillRedis().compose(v -> {
            Future<Void> startWebServer = Future.future();
            vertx.createHttpServer().requestHandler(req -> {
                req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
            }).listen(8080, listen -> {
                startWebServer.complete();
            });

            return startWebServer;
        });
        start.setHandler(startFuture.completer());
        System.out.println("HTTP server started on port 8080");
    }
}
