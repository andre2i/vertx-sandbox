# Chunked encoding error reproducer

```
$ ./gradlew run

HTTP server started on port 8080
```

```
$ curl -k --raw https://localhost:8080

(...) some JSON chunks (...)
curl: (56) Malformed encoding found in chunked-encoding
```

The output of the `./gradlew run` should now be filled with numerous instances of `java.nio.channels.ClosedChannelException`.

```
SEVERE: java.nio.channels.ClosedChannelException
Jan 24, 2018 9:58:30 AM io.vertx.core.net.impl.ConnectionBase
SEVERE: java.nio.channels.ClosedChannelException
Jan 24, 2018 9:58:30 AM io.vertx.core.net.impl.ConnectionBase
SEVERE: java.nio.channels.ClosedChannelException
```

Removing `serverOptions` from the `vertx.createHttpServer` call will create a plain http server (no SSL) which does not reproduce the issue when queried at `http://localhost:8080`.
