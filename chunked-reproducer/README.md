# Chunked encoding error reproducer

## Prerequisites

`redis` must be installed and running

## Local state changes

The project will take over redis database `10`, flush it and fill it with random JSON data

## Running the example

```
$ ./gradlew run

Fillig Redis
HTTP server started on port 8080
```

```
$ curl -k --raw https://localhost:8080

(...) some JSON chunks (...)
curl: (56) Malformed encoding found in chunked-encoding
```

Removing `serverOptions` from the `vertx.createHttpServer` call will create a plain http server (no SSL) which does not reproduce the issue when queried at `http://localhost:8080`.