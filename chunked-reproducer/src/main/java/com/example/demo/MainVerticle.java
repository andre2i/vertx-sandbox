package com.example.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {
    Future<Void> fillRedis() {
        Future<Void> future = Future.future();

        return future;
    }

    @Override
    public void start() throws Exception {
        vertx.createHttpServer().requestHandler(req -> {
              req.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from Vert.x!");
            }).listen(8080);
        System.out.println("HTTP server started on port 8080");
    }
}
