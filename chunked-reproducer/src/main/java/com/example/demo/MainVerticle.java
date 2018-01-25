package com.example.demo;

import java.net.HttpURLConnection;
import java.util.Random;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;

public class MainVerticle extends AbstractVerticle {
    private static final int KEY_COUNT = 10000;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String jksPath = classLoader.getResource("tls/server-keystore.jks").getFile();
        HttpServerOptions serverOptions = new HttpServerOptions().setSsl(true)
                .setKeyCertOptions(new JksOptions().setPath(jksPath).setPassword("wibble"));

        vertx.createHttpServer(serverOptions).requestHandler(req -> {
            System.out.println("Received a request");
            HttpServerResponse response = req.response().setChunked(true);

            response.write("[");
            for (int i = 0; i < KEY_COUNT; i++) {
                response.write(makePayload());
                response.write(",");
            }
            response.write(makePayload());
            response.write("]");

            response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json").setStatusCode(HttpURLConnection.HTTP_OK)
                    .end();
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
        int width = random.nextInt(3) + 1;
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
