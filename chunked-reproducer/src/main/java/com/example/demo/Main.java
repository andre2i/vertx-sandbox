package com.example.demo;

import com.example.demo.MainVerticle;
import io.vertx.core.Vertx;

public class Main {
    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(MainVerticle.class.getName());
    }
}
