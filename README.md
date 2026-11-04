# People API

API REST de ejemplo con **Spring Boot 4.1.2**, **Java 17** y **Maven** que persiste personas en una base **H2 en memoria** mediante **Spring Data JPA**.

## Requisitos

- JDK 17 (`java -version`)
- Maven 3.9+ (`mvn -version`). Si no lo tenés instalado, el proyecto incluye Maven Wrapper: usá `./mvnw` (Git Bash) o `.\mvnw.cmd` (PowerShell) en lugar de `mvn`.

## Correr la aplicación

```bash
mvn spring-boot:run
```

La app queda en `http://localhost:8080`. Al iniciar se crea la tabla `people` y se cargan 3 registros desde `src/main/resources/data.sql`.

## Endpoints

| Método | Ruta               | Descripción                 | Respuesta                      |
|--------|--------------------|-----------------------------|--------------------------------|
| GET    | `/api/people`      | Lista todas las personas    | `200` + array de `Person`      |
| GET    | `/api/people/{id}` | Obtiene una persona por id  | `200` + `Person`, o `404`      |

Ejemplo de `Person`:

```json
{
  "id": 1,
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "birthDate": "1815-12-10"
}
```

Ejemplo de error `404` (formato RFC 9457 `application/problem+json`):

```json
{
  "title": "Person not found",
  "status": 404,
  "detail": "Person with id 999 not found",
  "instance": "/api/people/999"
}
```

## Pruebas rápidas

```bash
curl http://localhost:8080/api/people
curl http://localhost:8080/api/people/1
curl -i http://localhost:8080/api/people/999
```

## Consola H2

Con la app corriendo: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:peopledb`
- User: `sa`
- Password: (vacío)

## Tests

```bash
mvn test
```

## Estructura

```
src/main/java/com/example/peopleapi
├── PeopleApiApplication.java
├── common/GlobalExceptionHandler.java   # excepciones -> ProblemDetail
└── person
    ├── Person.java                      # entidad JPA (tabla "people")
    ├── PersonRepository.java            # JpaRepository
    ├── PersonResponse.java              # DTO de salida
    ├── PersonNotFoundException.java
    ├── PersonService.java
    └── PersonController.java            # GET /api/people, GET /api/people/{id}
```
