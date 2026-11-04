> 🇪🇸 Versión en español: [README_es.md](README_es.md)

# People API

Sample REST API built with **Spring Boot 4.1.2**, **Java 17** and **Maven** that persists people in an **in-memory H2** database through **Spring Data JPA**.

## Requirements

- JDK 17 (`java -version`)
- Maven 3.9+ (`mvn -version`). If you don't have Maven installed, the project ships with the Maven Wrapper: use `./mvnw` (Git Bash / Linux / macOS) or `.\mvnw.cmd` (PowerShell) instead of `mvn`.

## Running the application

The API requires an API key. Set it in the `PEOPLE_API_KEY` environment variable before starting (the app refuses to start without it):

Git Bash / Linux / macOS:

```bash
export PEOPLE_API_KEY=change-me
mvn spring-boot:run
```

PowerShell:

```powershell
$env:PEOPLE_API_KEY = "change-me"
mvn spring-boot:run
```

The app listens on `http://localhost:8080`. On startup it creates the `people` table and loads 3 rows from `src/main/resources/data.sql`.

## Authentication

Every request to the API must send the key in the `X-API-Key` header (everything except the H2 console is protected):

Git Bash / Linux / macOS:

```bash
curl -H "X-API-Key: $PEOPLE_API_KEY" http://localhost:8080/api/people
```

PowerShell:

```powershell
curl.exe -H "X-API-Key: $env:PEOPLE_API_KEY" http://localhost:8080/api/people
```

| Situation                          | Response           |
|------------------------------------|--------------------|
| Header missing or blank            | `401 Unauthorized` |
| Header present but key not valid   | `403 Forbidden`    |

Both errors are `application/problem+json`; the `401` also carries a `WWW-Authenticate: ApiKey realm="people-api"` header:

```json
{
  "title": "API key missing",
  "status": 401,
  "detail": "Missing X-API-Key header",
  "instance": "/api/people"
}
```

The key is compared in constant time and is never logged. This is a single shared key meant for a demo API; roles and JWT will come later.

The key must be a printable ASCII string without leading or trailing spaces or line breaks (a trailing newline from a secrets file makes every request fail with 403).

The H2 console (`/h2-console`) is the only path that is NOT protected by the API key. It only accepts connections from localhost by default (`spring.h2.console.settings.web-allow-others=false`), but anyone with access to it can read all data and even the environment variables of the process, including `PEOPLE_API_KEY`, so it is for local development only and must not be enabled in a shared or production environment.

## Endpoints

| Method | Path                               | Description                              | Response                              |
|--------|------------------------------------|------------------------------------------|---------------------------------------|
| GET    | `/api/people`                      | Lists all people                         | `200` + array of `Person`             |
| GET    | `/api/people/{id}`                 | Gets one person by id                    | `200` + `Person`, or `404`            |
| GET    | `/api/people/page?n=&numpage=`     | Lists people page by page (see below)    | `200` + page object, or `400`         |

`Person` example:

```json
{
  "id": 1,
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "birthDate": "1815-12-10"
}
```

`404` error example (RFC 9457 `application/problem+json`):

```json
{
  "title": "Person not found",
  "status": 404,
  "detail": "Person with id 999 not found",
  "instance": "/api/people/999"
}
```

### Pagination

`GET /api/people/page` takes two required query parameters:

- `n`: items per page (`1` to `100`)
- `numpage`: page number, starting at `1`

People are ordered by `id`. A page past the last one returns an empty `content` with the same totals. `n` above 100, or a `numpage` so large that the offset overflows, also returns `400` as problem+json. `page` and `size` echo the request, so `size` is the requested page size even when the last page has fewer items.

Git Bash / Linux / macOS:

```bash
curl -H "X-API-Key: $PEOPLE_API_KEY" "http://localhost:8080/api/people/page?n=2&numpage=1"
```

PowerShell:

```powershell
curl.exe -H "X-API-Key: $env:PEOPLE_API_KEY" "http://localhost:8080/api/people/page?n=2&numpage=1"
```

```json
{
  "content": [
    { "id": 1, "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com", "birthDate": "1815-12-10" },
    { "id": 2, "firstName": "Alan", "lastName": "Turing", "email": "alan@example.com", "birthDate": "1912-06-23" }
  ],
  "page": 1,
  "size": 2,
  "totalPages": 2,
  "totalItems": 3
}
```

`n` or `numpage` below `1` returns `400` as `application/problem+json`:

```json
{
  "title": "Invalid page request",
  "status": 400,
  "detail": "n must be >= 1 but was 0",
  "instance": "/api/people/page"
}
```

A missing or non-numeric parameter also returns `400` (Spring's default error body).

## Quick checks

Git Bash / Linux / macOS:

```bash
curl -H "X-API-Key: $PEOPLE_API_KEY" http://localhost:8080/api/people
curl -H "X-API-Key: $PEOPLE_API_KEY" http://localhost:8080/api/people/1
curl -i -H "X-API-Key: $PEOPLE_API_KEY" http://localhost:8080/api/people/999
curl -i http://localhost:8080/api/people   # no key: expect 401
```

PowerShell:

```powershell
curl.exe -H "X-API-Key: $env:PEOPLE_API_KEY" http://localhost:8080/api/people
curl.exe -H "X-API-Key: $env:PEOPLE_API_KEY" http://localhost:8080/api/people/1
curl.exe -i -H "X-API-Key: $env:PEOPLE_API_KEY" http://localhost:8080/api/people/999
curl.exe -i http://localhost:8080/api/people   # no key: expect 401
```

## H2 console

With the app running: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:peopledb`
- User: `sa`
- Password: (empty)

## Tests

```bash
mvn test
```

## Project layout

```
src/main/java/com/example/peopleapi
├── PeopleApiApplication.java
├── common/GlobalExceptionHandler.java   # exceptions -> ProblemDetail
└── person
    ├── Person.java                      # JPA entity (table "people")
    ├── PersonRepository.java            # JpaRepository
    ├── PersonResponse.java              # response DTO
    ├── PersonNotFoundException.java
    ├── PersonService.java
    └── PersonController.java            # GET /api/people, GET /api/people/{id}
```
