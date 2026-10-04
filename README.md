# Global exception handling — presentation demo

A standalone Java 21 + Spring Boot Product API with a live presentation screen.
Click a scenario, send a real HTTP request, and explain how one shared exception
handler produces a consistent error response across two controllers.

## Run

Requirements: Java 21 and Maven 3.6.3 or newer. The first build downloads dependencies.

```bash
cd "/Users/somnath/Documents/New project/examples/global-exception-demo"
mvn -Dmaven.repo.local=.m2 verify
java -jar target/global-exception-demo-1.0.0.jar
```

Open **http://127.0.0.1:8095** and share that browser window during your presentation.
The app uses a fixed product and stock count; no database, credentials, or other
workspace services are required. To stop the app, press Ctrl+C in its terminal.
If the port is busy, append `--server.port=8096` to the Java command and use that port.

## Six live scenarios

| Click | Request | Result | Explanation |
|---|---|---|---|
| Product found | `GET /api/products/1` | 200 | Normal return; no exception handler runs |
| Product not found | `GET /api/products/99` | 404 | Custom `ProductNotFoundException` |
| Invalid order quantity | `POST /api/orders/preview` with quantity `0` | 400 | `@Valid` rejects the request before the method runs |
| Insufficient stock | Same POST with quantity `10` | 409 | Business rule: only 5 units exist |
| Malformed JSON | Same POST with incomplete JSON | 400 | Framework exception retains its correct HTTP status |
| Unexpected failure | `GET /api/products/demo-failure` | 500 | Safe message; full exception logged with request ID |

The order endpoint previews an order without reserving or decrementing stock, so
every presentation run behaves the same way. `POST /api/orders/preview` with
`{"productId":99,"quantity":1}` demonstrates the same 404 handler from the second controller.

## Five-minute presenter script

**0:00–0:40 — The problem.**
“If every controller has its own try/catch blocks, we repeat error handling and
can return different error formats. We want one place to translate exceptions
into useful HTTP responses.”

**0:40–1:15 — Normal behavior.**
Click **Product found**. “The controller calls the service and returns the product.
Exception handling does not change this successful flow.”

**1:15–2:15 — The central idea.**
Click **Product not found**, then expand **The key code to explain**.
“The service throws `ProductNotFoundException`. Spring finds the matching method
in `@RestControllerAdvice`. That method returns HTTP 404 with a stable error code.
There is no try/catch in the controller.”

**2:15–3:15 — Two kinds of client errors.**
Click **Invalid order quantity**, then **Insufficient stock**.
“Zero quantity fails input validation, so we return 400 with the field name.
Ten units is valid input, but conflicts with the available inventory, so this
demo returns 409. Both controllers use the same advice.”

**3:15–3:45 — Framework failures.**
Click **Malformed JSON**. “Parsing fails before our controller method executes.
Extending `ResponseEntityExceptionHandler` preserves Spring's HTTP status and
headers, instead of turning every exception into a 500.”

**3:45–4:30 — Unexpected failures.**
Click **Unexpected failure**. “The client gets a safe 500 response. The internal
database failure message and stack trace stay in the server log. We can search
that log using the request ID returned to the client.” Show the terminal log
and point to the matching `requestId=` value.

**4:30–5:00 — Scope and takeaway.**
“Controllers focus on requests; services enforce business rules; advice maps
exceptions to HTTP responses. Global here means shared MVC request handling.
It does not automatically catch failures in background jobs or security filters.”

## Error response example

This is the response shape for the missing-product scenario; timestamp and ID vary.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product 99 was not found.",
  "instance": "/api/products/99",
  "code": "PRODUCT_NOT_FOUND",
  "timestamp": "2026-10-04T09:00:00Z",
  "requestId": "b734348d-5389-4e08-9d48-c6119ae2c801"
}
```

The HTTP status tells the caller the failure category, `code` is a stable value
clients can branch on, and `detail` is a human-readable message. Validation adds
an `errors` array. `X-Request-Id` in the response header matches `requestId` in the
body. The generated request ID is a local correlation ID, not a distributed trace.

## Code walkthrough order

1. `ProductController.java` — successful request; no local exception handling.
2. `ProductService.java` — throw a meaningful exception where the rule is known.
3. `ProductNotFoundException.java` — custom runtime exception.
4. `GlobalExceptionHandler.java` — map custom, validation, framework, and unexpected errors.
5. `OrderController.java` — `@Valid` and a second controller using the same advice.
6. `RequestIdFilter.java` and `ErrorResponseMetadata.java` — common response metadata.

All Java files are under `src/main/java/com/example/exceptions/`.
The live screen is `src/main/resources/static/index.html`.

## curl / Postman alternative

```bash
curl -i http://127.0.0.1:8095/api/products/1
curl -i http://127.0.0.1:8095/api/products/99
curl -i http://127.0.0.1:8095/api/orders/preview \
  -H 'Content-Type: application/json' -d '{"productId":1,"quantity":0}'
curl -i http://127.0.0.1:8095/api/orders/preview \
  -H 'Content-Type: application/json' -d '{"productId":1,"quantity":10}'
curl -i http://127.0.0.1:8095/api/orders/preview \
  -H 'Content-Type: application/json' -d '{"productId":'
curl -i http://127.0.0.1:8095/api/products/demo-failure
```

In Postman, select the same method and URL; for POST choose **Body → raw → JSON**
and paste the corresponding JSON above.

## Verification and scope

`mvn -Dmaven.repo.local=.m2 verify` runs integration checks for successful requests,
both controllers sharing the handler, validation, malformed JSON, bad path IDs,
405 status/Allow header preservation, repeated order previews, safe 500 responses,
request ID consistency, and the presentation page.

This is a localhost teaching app with a deliberate failure endpoint and fixed
in-memory product data. Its exception policy covers Spring MVC request handling;
filters, background threads, and errors after a response has been committed have
separate lifecycles. The fallback does not attempt to catch JVM `Error` types.

## Reading the test output in IntelliJ

The test `unexpectedFailureDoesNotLeakDetails` deliberately calls the failure
endpoint. The console therefore prints `Simulated inventory database connection
failure` and a stack trace from the global handler. This is expected: the test
checks that the HTTP response is 500 and does not expose those internal details.
Use the test results (12 passed, zero failures/errors) to determine success.

The Maven test dependency excludes `mockito-core` and `mockito-junit-jupiter`.
These integration tests use real application beans through Spring's MockMvc,
which does not require Mockito. Removing the unused dependencies avoids Mockito's
agent self-attachment warnings in both Maven and IntelliJ. If future tests need
Mockito mocks, restore those dependencies and configure its startup Java agent
when using inline mocking.

After pulling in this configuration, reload the Maven project in IntelliJ and
rerun `GlobalExceptionDemoTest`. To start the browser demo, run
`DemoApplication.main()`; running the test class alone does not start a persistent
HTTP server.

## Official references

- [Spring controller advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
- [Spring error responses and ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
