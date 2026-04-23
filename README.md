# Smart Campus API

This project implements the `5COSC022W Client-Server Architectures` coursework brief as a pure JAX-RS REST API. It manages campus rooms, sensors, and nested historical sensor readings using in-memory Java collections only.

## Overview

- Technology: Java 17, Maven, JAX-RS with Jersey, Grizzly HTTP server
- Base URL: `http://localhost:8080/api/v1`
- Storage: `ConcurrentHashMap` and `CopyOnWriteArrayList`
- JSON support: Jackson via `jersey-media-json-jackson`
- No database and no Spring Boot are used

## Project Structure

- `ApplicationConfig` registers resources, filters, and exception mappers
- `CampusStore` provides thread-safe in-memory data management
- `RoomResource` handles `/rooms`
- `SensorResource` handles `/sensors`
- `SensorReadingResource` handles `/sensors/{sensorId}/readings`
- Custom exception mappers return consistent JSON error bodies
- `ApiLoggingFilter` logs every request and response

## Build And Run

### Standard Maven

```bash
mvn clean package
mvn exec:java
```

### Run The Shaded Jar

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

When the server starts, open:

```text
http://localhost:8080/api/v1
```

## API Endpoints

- `GET /api/v1`
- `GET /api/v1/rooms`
- `POST /api/v1/rooms`
- `GET /api/v1/rooms/{roomId}`
- `DELETE /api/v1/rooms/{roomId}`
- `GET /api/v1/sensors`
- `GET /api/v1/sensors?type=CO2`
- `POST /api/v1/sensors`
- `GET /api/v1/sensors/{sensorId}`
- `GET /api/v1/sensors/{sensorId}/readings`
- `POST /api/v1/sensors/{sensorId}/readings`
- `GET /api/v1/debug/crash`

## Sample curl Commands

```bash
curl http://localhost:8080/api/v1
```

```bash
curl -i -X POST http://localhost:8080/api/v1/rooms ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":120}"
```

```bash
curl http://localhost:8080/api/v1/rooms/LIB-301
```

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":415.2,\"roomId\":\"LIB-301\"}"
```

```bash
curl http://localhost:8080/api/v1/sensors?type=CO2
```

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings ^
  -H "Content-Type: application/json" ^
  -d "{\"value\":420.8}"
```

```bash
curl http://localhost:8080/api/v1/sensors/CO2-001/readings
```

```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

```bash
curl -i http://localhost:8080/api/v1/debug/crash
```

## Video Demonstration Checklist

- Show `GET /api/v1` returning discovery metadata
- Show `POST /rooms` returning `201 Created` with `Location`
- Show `GET /rooms/{id}` for the room you created
- Show `POST /sensors` failing for a missing `roomId` and succeeding for a valid room
- Show `GET /sensors?type=...` changing results when the query parameter changes
- Show `/sensors/{id}/readings` nested under a sensor in Postman
- Show `POST /readings` updating the reading history and the parent sensor `currentValue`
- Show `DELETE /rooms/{id}` failing with `409 Conflict` when the room still has sensors
- Show a `403 Forbidden` by posting a reading to a sensor in `MAINTENANCE`
- Show `GET /debug/crash` returning a clean `500` JSON response with no stack trace

## Coursework Report Answers

### 1.1 Architecture And Config

By default, JAX-RS resource classes are request-scoped, which means the runtime typically creates a new resource instance for each incoming HTTP request rather than sharing one singleton controller instance across all requests. This default is safer because instance fields inside resource classes are not shared between concurrent clients. However, the in-memory data structures that hold the application state are shared application-wide, so they still need thread-safe handling.

In this implementation, the shared state is centralized inside `CampusStore`, which acts as the application data layer. It uses `ConcurrentHashMap` for top-level room and sensor collections and synchronized blocks around object-level mutations such as adding a sensor ID to a room or updating a sensor's `currentValue`. That combination prevents race conditions, lost updates, and inconsistent relationships between rooms, sensors, and readings when multiple requests arrive at the same time.

### 1.2 Discovery Endpoint

Hypermedia is a hallmark of advanced RESTful design because responses do not just return data, they also explain what the client can do next. A discovery document with links to `rooms` and `sensors` helps the API become self-describing. Clients can start at the API root and navigate from there instead of depending entirely on external static documentation.

This approach benefits client developers because the server can expose the current structure of the API directly in runtime responses. That reduces coupling, makes versioning easier, and helps tools or frontend clients discover valid resource paths without hardcoding every route manually.

### 2.1 Room Implementation

Returning only room IDs reduces payload size and saves bandwidth, which can matter when there are many rooms or when clients only need identifiers for follow-up calls. The trade-off is that clients then need extra requests to fetch room details, which increases round trips and shifts more work to the client.

Returning full room objects increases response size, but it is often more convenient because clients immediately receive the full metadata they need. For this coursework, returning the full room objects is the better default because it makes the API easier to consume and demonstrate while still keeping payload sizes manageable.

### 2.2 Deletion And Logic

The `DELETE` operation is idempotent in terms of server state. If a room exists and has no sensors, the first delete removes it. If the same delete request is sent again afterwards, the room is already gone, so the server returns `404 Not Found`, but the state of the system does not change any further. Repeating the same request therefore does not create additional side effects.

If a room still contains sensors, the delete request is rejected with `409 Conflict`. Sending the same request repeatedly still leaves the server in the same state because the room and its linked sensors remain unchanged until the conflict is resolved.

### 3.1 Sensor Integrity

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the method only accepts JSON request bodies. If a client sends the same endpoint request using `text/plain` or `application/xml`, the runtime will not find a compatible message body reader for that method and media type combination.

In practice, JAX-RS responds with `415 Unsupported Media Type`, which is the correct HTTP signal that the client used an unsupported payload format. This protects the API by enforcing a clear input contract and preventing accidental parsing of invalid body types.

### 3.2 Filtered Retrieval

Using `@QueryParam` is generally superior for filtering collections because it keeps the main resource URI stable while expressing the filter as an optional modifier. `GET /sensors?type=CO2` still targets the same collection, but narrows the result set according to the supplied search criteria.

By contrast, a design like `/sensors/type/CO2` makes the filter look like a different nested resource rather than a variation of the same collection request. Query parameters scale better when more filters are added later, such as `status`, `roomId`, or pagination options, because the URI structure remains clean and consistent.

### 4.1 Sub-Resource Locator

The Sub-Resource Locator pattern improves architecture by delegating nested responsibilities to focused classes. In this API, `SensorResource` handles the main `/sensors` collection, while `SensorReadingResource` is responsible only for a single sensor's reading history under `/sensors/{sensorId}/readings`.

This separation reduces controller bloat, improves readability, and makes the code easier to maintain as the API grows. Without sub-resource locators, one large class would need to manage rooms, sensors, readings, nested IDs, and business rules all together, which makes testing and future extension much harder.

### 5.1 Specific Exceptions

HTTP `422 Unprocessable Entity` is more semantically accurate than `404 Not Found` when the request body is syntactically valid JSON but contains a broken reference, such as a `roomId` that does not exist. The endpoint itself exists, and the payload format is correct, so the problem is not that the client requested a missing URL resource.

Instead, the problem is that the server understood the payload but could not process it because one of the referenced linked resources was invalid. That is exactly the kind of validation failure that `422` communicates more precisely than a generic `404`.

### 5.2 Global Safety Net

Exposing raw Java stack traces to outside consumers is dangerous because it leaks internal technical details that attackers can use for reconnaissance. A stack trace may reveal package names, class names, source file names, framework versions, internal paths, exception types, and the exact code path that failed.

That information helps attackers fingerprint the technology stack, identify weak libraries, infer business logic, and craft more targeted attacks. Returning a clean generic `500` JSON response is therefore much safer because it avoids disclosing internal implementation details while still signalling that the server encountered an unexpected problem.

### 5.3 Logging Filters

JAX-RS filters are better for cross-cutting concerns like logging because they centralize the behavior in one reusable place. Instead of adding `Logger.info()` lines manually inside every resource method, the filter automatically runs for every request and response across the whole API.

This improves consistency, reduces duplicated code, and makes the resource classes easier to read because they stay focused on business logic. It also means that new endpoints automatically inherit the same observability behavior without additional logging code being copied into each controller.
