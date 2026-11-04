# People API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** API REST con Spring Boot 4 y Java 17 que persiste una entidad `Person` (tabla `people`) en H2 vía JPA y expone `GET /api/people` y `GET /api/people/{id}`.

**Architecture:** Aplicación Spring Boot de un solo módulo Maven con capas `Controller -> Service -> Repository`. La entidad JPA `Person` se mapea a la tabla `people`; el controller devuelve un DTO `PersonResponse` (record) y los errores 404 se serializan como `ProblemDetail` (RFC 9457) desde un `@RestControllerAdvice`. La base H2 es en memoria, se crea con `ddl-auto=create-drop` y se siembra con `data.sql`.

**Tech Stack:** Java 17, Maven (Maven Wrapper), Spring Boot 4.1.2 (Spring Framework 7, Jackson 3), Spring Web MVC, Spring Data JPA (Hibernate), H2 2.x, JUnit 5, Mockito, AssertJ, MockMvc.

## Global Constraints

- Java: `17` (`<java.version>17</java.version>`). En la máquina hay `java 17.0.12`.
- Spring Boot: `4.1.2` (versión estable por defecto en start.spring.io al 2026-11-04).
- Build: **Maven 3.9.16**, instalado en `C:\Datos\Utiles\apache-maven-3.9.16` y configurado en las variables de entorno del usuario (`MAVEN_HOME` y `%MAVEN_HOME%\bin` en `Path`). Los comandos usan `mvn` directo. Si una terminal abierta antes de esa configuración no lo encuentra, abrir una nueva o usar el Maven Wrapper (`./mvnw` / `.\mvnw.cmd`) que también viene en el proyecto.
- Artefactos de Boot 4 (los nombres cambiaron respecto a Boot 3): `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-h2console`, y para test `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test`.
- Paquetes de anotaciones de test en Boot 4 (verificados en los jars 4.1.2):
  - `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
  - `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
  - `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`
  - `org.springframework.boot.test.context.SpringBootTest`
  - `org.springframework.test.context.bean.override.mockito.MockitoBean` (reemplaza a `@MockBean`)
- Paquete base: `com.example.peopleapi`. Entidad y todo lo relacionado en `com.example.peopleapi.person`.
- Nombres de atributos en inglés: `id`, `firstName`, `lastName`, `email`, `birthDate`.
- Endpoints: `GET /api/people` y `GET /api/people/{id}`. No se piden POST/PUT/DELETE: no agregarlos (YAGNI).
- Shell: los comandos abajo están en formato Git Bash; `mvn` funciona igual en PowerShell.
- Commits frecuentes, mensajes en inglés con prefijo convencional (`feat:`, `test:`, `chore:`, `docs:`).

---

## File Structure

```
.
├── pom.xml                                   # dependencias Boot 4.1.2, Java 17
├── mvnw / mvnw.cmd / .mvn/wrapper/           # Maven Wrapper (generado por Initializr)
├── .gitignore                                # generado por Initializr (target/, IDE)
├── README.md                                 # cómo correr y probar
├── docs/superpowers/plans/2026-11-04-people-api.md
└── src
    ├── main
    │   ├── java/com/example/peopleapi
    │   │   ├── PeopleApiApplication.java                 # main de Spring Boot
    │   │   ├── common/GlobalExceptionHandler.java        # mapea excepciones a ProblemDetail
    │   │   └── person
    │   │       ├── Person.java                           # entidad JPA, tabla "people"
    │   │       ├── PersonRepository.java                 # JpaRepository<Person, Long>
    │   │       ├── PersonResponse.java                   # DTO de salida (record)
    │   │       ├── PersonNotFoundException.java          # excepción de dominio -> 404
    │   │       ├── PersonService.java                    # findAll / findById
    │   │       └── PersonController.java                 # GET /api/people, GET /api/people/{id}
    │   └── resources
    │       ├── application.properties                    # H2 en memoria, consola, JPA
    │       └── data.sql                                  # 3 personas de ejemplo
    └── test/java/com/example/peopleapi
        ├── PeopleApiApplicationTests.java                # contextLoads (generado)
        ├── PeopleApiIntegrationTest.java                 # @SpringBootTest + MockMvc contra H2 real
        └── person
            ├── PersonRepositoryTest.java                 # @DataJpaTest
            ├── PersonServiceTest.java                    # Mockito puro
            └── PersonControllerTest.java                 # @WebMvcTest + @MockitoBean
```

---

### Task 1: Scaffold del proyecto Maven con Spring Initializr y configuración de H2

**Files:**
- Create: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`, `.gitignore`, `.gitattributes`, `HELP.md` (todos generados)
- Create: `src/main/java/com/example/peopleapi/PeopleApiApplication.java` (generado)
- Create: `src/test/java/com/example/peopleapi/PeopleApiApplicationTests.java` (generado)
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: nada.
- Produces: proyecto Maven compilable con las dependencias de las Global Constraints; propiedades de datasource H2 `jdbc:h2:mem:peopledb` con usuario `sa` y password vacío; `spring.jpa.defer-datasource-initialization=true` (necesario para que `data.sql` de la Task 2 corra después de que Hibernate cree las tablas).

- [ ] **Step 1: Descargar el esqueleto Maven desde start.spring.io a la raíz del repo**

Ejecutar desde la raíz del repo (está vacío, solo tiene `.git` y `docs/`):

```bash
curl -s "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.2&groupId=com.example&artifactId=people-api&name=people-api&packageName=com.example.peopleapi&javaVersion=17&dependencies=web,data-jpa,h2,validation" -o starter.zip
unzip -o -q starter.zip
rm starter.zip
ls
```

Expected: aparecen `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore`, `HELP.md`, `src/`.

- [ ] **Step 2: Verificar que el pom.xml tiene las dependencias esperadas de Boot 4**

```bash
grep -E "<version>4.1.2</version>|<java.version>17</java.version>|spring-boot-starter-webmvc|spring-boot-starter-data-jpa|spring-boot-h2console|<artifactId>h2</artifactId>" pom.xml
```

Expected: una línea por cada patrón (parent 4.1.2, java 17, `spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-jpa-test`, `spring-boot-h2console`, `h2`). Si falta alguna, el esqueleto no se generó bien: repetir el Step 1.

- [ ] **Step 3: Configurar H2 en memoria y JPA**

Reemplazar completamente el contenido de `src/main/resources/application.properties`:

```properties
spring.application.name=people-api

# H2 in-memory database
spring.datasource.url=jdbc:h2:mem:peopledb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
# run data.sql AFTER Hibernate creates the schema
spring.jpa.defer-datasource-initialization=true

# H2 web console -> http://localhost:8080/h2-console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- [ ] **Step 4: Correr el test generado para verificar que el contexto levanta**

```bash
mvn -q -B test
```

Expected: sin salida de error con `-q`, y en `target/surefire-reports/` un `PeopleApiApplicationTests.txt` con `Tests run: 1, Failures: 0, Errors: 0`. La primera corrida descarga dependencias y puede tardar varios minutos.

```bash
cat target/surefire-reports/com.example.peopleapi.PeopleApiApplicationTests.txt
```

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "chore: scaffold Spring Boot 4.1.2 Maven project with H2 and JPA"
```

---

### Task 2: Entidad `Person`, `PersonRepository` y datos de ejemplo

**Files:**
- Create: `src/main/java/com/example/peopleapi/person/Person.java`
- Create: `src/main/java/com/example/peopleapi/person/PersonRepository.java`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/peopleapi/person/PersonRepositoryTest.java`

**Interfaces:**
- Consumes: configuración de datasource de la Task 1.
- Produces:
  - `class Person` con `Long getId()`, `String getFirstName()`, `String getLastName()`, `String getEmail()`, `LocalDate getBirthDate()`; constructores `Person(String firstName, String lastName, String email, LocalDate birthDate)` y `Person(Long id, String firstName, String lastName, String email, LocalDate birthDate)` (este último para tests).
  - `interface PersonRepository extends JpaRepository<Person, Long>` (hereda `findAll()` y `findById(Long)`).
  - Tabla `people` con columnas `id`, `first_name`, `last_name`, `email`, `birth_date` y 3 filas sembradas con ids 1, 2, 3 (Ada Lovelace, Alan Turing, Grace Hopper).

- [ ] **Step 1: Escribir el test del repositorio (falla porque no existen las clases)**

Crear `src/test/java/com/example/peopleapi/person/PersonRepositoryTest.java`:

```java
package com.example.peopleapi.person;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PersonRepositoryTest {

    @Autowired
    private PersonRepository repository;

    @Test
    void findAll_returnsSeededPeople() {
        List<Person> people = repository.findAll();

        assertThat(people).hasSize(3);
        assertThat(people).extracting(Person::getEmail)
                .containsExactlyInAnyOrder("ada@example.com", "alan@example.com", "grace@example.com");
    }

    @Test
    void findById_returnsPersonWhenExists() {
        Optional<Person> person = repository.findById(1L);

        assertThat(person).isPresent();
        assertThat(person.get().getFirstName()).isEqualTo("Ada");
        assertThat(person.get().getLastName()).isEqualTo("Lovelace");
        assertThat(person.get().getBirthDate()).isEqualTo(LocalDate.of(1815, 12, 10));
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        assertThat(repository.findById(999L)).isEmpty();
    }

    @Test
    void save_assignsGeneratedId() {
        Person saved = repository.save(new Person("Linus", "Torvalds", "linus@example.com", LocalDate.of(1969, 12, 28)));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla por compilación**

```bash
mvn -q -B test -Dtest=PersonRepositoryTest
```

Expected: `COMPILATION ERROR` con `cannot find symbol ... class Person` / `PersonRepository`.

- [ ] **Step 3: Crear la entidad `Person`**

Crear `src/main/java/com/example/peopleapi/person/Person.java`:

```java
package com.example.peopleapi.person;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "people")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    private LocalDate birthDate;

    /** Required by JPA. */
    protected Person() {
    }

    public Person(String firstName, String lastName, String email, LocalDate birthDate) {
        this(null, firstName, lastName, email, birthDate);
    }

    /** Full constructor, mainly for tests that need a pre-assigned id. */
    public Person(Long id, String firstName, String lastName, String email, LocalDate birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}
```

- [ ] **Step 4: Crear `PersonRepository`**

Crear `src/main/java/com/example/peopleapi/person/PersonRepository.java`:

```java
package com.example.peopleapi.person;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
```

- [ ] **Step 5: Crear los datos de ejemplo**

Crear `src/main/resources/data.sql` (las columnas usan el snake_case que genera la estrategia de nombres por defecto de Spring Boot):

```sql
INSERT INTO people (first_name, last_name, email, birth_date) VALUES ('Ada', 'Lovelace', 'ada@example.com', '1815-12-10');
INSERT INTO people (first_name, last_name, email, birth_date) VALUES ('Alan', 'Turing', 'alan@example.com', '1912-06-23');
INSERT INTO people (first_name, last_name, email, birth_date) VALUES ('Grace', 'Hopper', 'grace@example.com', '1906-12-09');
```

- [ ] **Step 6: Correr el test y verificar que pasa**

```bash
mvn -q -B test -Dtest=PersonRepositoryTest
cat target/surefire-reports/com.example.peopleapi.person.PersonRepositoryTest.txt
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`.

Nota: `@DataJpaTest` reemplaza el datasource por un H2 embebido con nombre único, pero igual ejecuta `data.sql` gracias a `defer-datasource-initialization=true`. Si `findAll_returnsSeededPeople` falla con 0 filas, revisar que esa propiedad quedó en `application.properties` (Task 1, Step 3).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/peopleapi/person/Person.java src/main/java/com/example/peopleapi/person/PersonRepository.java src/main/resources/data.sql src/test/java/com/example/peopleapi/person/PersonRepositoryTest.java
git commit -m "feat: add Person entity, repository and seed data"
```

---

### Task 3: DTO, excepción 404, `PersonService` y manejador global de errores

**Files:**
- Create: `src/main/java/com/example/peopleapi/person/PersonResponse.java`
- Create: `src/main/java/com/example/peopleapi/person/PersonNotFoundException.java`
- Create: `src/main/java/com/example/peopleapi/person/PersonService.java`
- Create: `src/main/java/com/example/peopleapi/common/GlobalExceptionHandler.java`
- Test: `src/test/java/com/example/peopleapi/person/PersonServiceTest.java`

**Interfaces:**
- Consumes: `Person` (ambos constructores y getters) y `PersonRepository` de la Task 2.
- Produces:
  - `record PersonResponse(Long id, String firstName, String lastName, String email, LocalDate birthDate)` con `static PersonResponse from(Person person)`.
  - `class PersonNotFoundException extends RuntimeException` con constructor `PersonNotFoundException(Long id)`; `getMessage()` devuelve `"Person with id <id> not found"`.
  - `class PersonService` con `List<PersonResponse> findAll()` y `PersonResponse findById(Long id)` (lanza `PersonNotFoundException`).
  - `GlobalExceptionHandler` (`@RestControllerAdvice`) que convierte `PersonNotFoundException` en `ProblemDetail` con status 404, `title = "Person not found"` y `detail = ex.getMessage()`.

- [ ] **Step 1: Escribir el test unitario del servicio (falla por compilación)**

Crear `src/test/java/com/example/peopleapi/person/PersonServiceTest.java`:

```java
package com.example.peopleapi.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository repository;

    @InjectMocks
    private PersonService service;

    private final Person ada = new Person(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10));
    private final Person alan = new Person(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23));

    @Test
    void findAll_mapsEveryPersonToResponse() {
        when(repository.findAll()).thenReturn(List.of(ada, alan));

        List<PersonResponse> result = service.findAll();

        assertThat(result).containsExactly(
                new PersonResponse(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10)),
                new PersonResponse(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23)));
    }

    @Test
    void findAll_returnsEmptyListWhenNoPeople() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findById_returnsResponseWhenExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(ada));

        PersonResponse result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Ada");
        assertThat(result.email()).isEqualTo("ada@example.com");
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessage("Person with id 999 not found");
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla por compilación**

```bash
mvn -q -B test -Dtest=PersonServiceTest
```

Expected: `COMPILATION ERROR` con `cannot find symbol ... PersonService` / `PersonResponse` / `PersonNotFoundException`.

- [ ] **Step 3: Crear el DTO `PersonResponse`**

Crear `src/main/java/com/example/peopleapi/person/PersonResponse.java`:

```java
package com.example.peopleapi.person;

import java.time.LocalDate;

public record PersonResponse(Long id, String firstName, String lastName, String email, LocalDate birthDate) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail(),
                person.getBirthDate());
    }
}
```

- [ ] **Step 4: Crear `PersonNotFoundException`**

Crear `src/main/java/com/example/peopleapi/person/PersonNotFoundException.java`:

```java
package com.example.peopleapi.person;

public class PersonNotFoundException extends RuntimeException {

    public PersonNotFoundException(Long id) {
        super("Person with id " + id + " not found");
    }
}
```

- [ ] **Step 5: Crear `PersonService`**

Crear `src/main/java/com/example/peopleapi/person/PersonService.java`:

```java
package com.example.peopleapi.person;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public List<PersonResponse> findAll() {
        return repository.findAll().stream()
                .map(PersonResponse::from)
                .toList();
    }

    public PersonResponse findById(Long id) {
        return repository.findById(id)
                .map(PersonResponse::from)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }
}
```

- [ ] **Step 6: Correr el test del servicio y verificar que pasa**

```bash
mvn -q -B test -Dtest=PersonServiceTest
cat target/surefire-reports/com.example.peopleapi.person.PersonServiceTest.txt
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 7: Crear el manejador global de excepciones**

Crear `src/main/java/com/example/peopleapi/common/GlobalExceptionHandler.java` (se testea vía HTTP en la Task 4):

```java
package com.example.peopleapi.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.peopleapi.person.PersonNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonNotFoundException.class)
    public ProblemDetail handlePersonNotFound(PersonNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Person not found");
        return problem;
    }
}
```

- [ ] **Step 8: Verificar que todo compila y la suite completa sigue en verde**

```bash
mvn -q -B test
```

Expected: sin errores; `target/surefire-reports/` muestra `Failures: 0, Errors: 0` en los tres archivos (`PeopleApiApplicationTests`, `PersonRepositoryTest`, `PersonServiceTest`).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/example/peopleapi/person/PersonResponse.java src/main/java/com/example/peopleapi/person/PersonNotFoundException.java src/main/java/com/example/peopleapi/person/PersonService.java src/main/java/com/example/peopleapi/common/GlobalExceptionHandler.java src/test/java/com/example/peopleapi/person/PersonServiceTest.java
git commit -m "feat: add PersonService, response DTO and 404 problem-detail handler"
```

---

### Task 4: `PersonController` con `GET /api/people` y `GET /api/people/{id}`

**Files:**
- Create: `src/main/java/com/example/peopleapi/person/PersonController.java`
- Test: `src/test/java/com/example/peopleapi/person/PersonControllerTest.java`

**Interfaces:**
- Consumes: `PersonService.findAll()`, `PersonService.findById(Long)`, `PersonResponse`, `PersonNotFoundException`, `GlobalExceptionHandler` de la Task 3.
- Produces:
  - `GET /api/people` → 200, JSON array de `PersonResponse`.
  - `GET /api/people/{id}` → 200 con un `PersonResponse`, o 404 `application/problem+json` con `title = "Person not found"` y `detail = "Person with id <id> not found"`.
  - `birthDate` se serializa como string ISO (`"1815-12-10"`), comportamiento por defecto de Jackson 3 en Boot 4.

- [ ] **Step 1: Escribir el test del controller (falla por compilación)**

Crear `src/test/java/com/example/peopleapi/person/PersonControllerTest.java`:

```java
package com.example.peopleapi.person;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonService service;

    private final PersonResponse ada = new PersonResponse(1L, "Ada", "Lovelace", "ada@example.com", LocalDate.of(1815, 12, 10));
    private final PersonResponse alan = new PersonResponse(2L, "Alan", "Turing", "alan@example.com", LocalDate.of(1912, 6, 23));

    @Test
    void getAll_returnsListOfPeople() throws Exception {
        when(service.findAll()).thenReturn(List.of(ada, alan));

        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ada"))
                .andExpect(jsonPath("$[0].lastName").value("Lovelace"))
                .andExpect(jsonPath("$[0].email").value("ada@example.com"))
                .andExpect(jsonPath("$[0].birthDate").value("1815-12-10"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].firstName").value("Alan"));
    }

    @Test
    void getAll_returnsEmptyArrayWhenNoPeople() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getOne_returnsPersonWhenExists() throws Exception {
        when(service.findById(1L)).thenReturn(ada);

        mockMvc.perform(get("/api/people/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1815-12-10"));
    }

    @Test
    void getOne_returns404ProblemDetailWhenMissing() throws Exception {
        when(service.findById(999L)).thenThrow(new PersonNotFoundException(999L));

        mockMvc.perform(get("/api/people/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Person not found"))
                .andExpect(jsonPath("$.detail").value("Person with id 999 not found"));
    }

    @Test
    void getOne_returns400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/people/abc"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla por compilación**

```bash
mvn -q -B test -Dtest=PersonControllerTest
```

Expected: `COMPILATION ERROR` con `cannot find symbol ... class PersonController`.

- [ ] **Step 3: Crear `PersonController`**

Crear `src/main/java/com/example/peopleapi/person/PersonController.java`:

```java
package com.example.peopleapi.person;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    @GetMapping
    public List<PersonResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PersonResponse getOne(@PathVariable Long id) {
        return service.findById(id);
    }
}
```

- [ ] **Step 4: Correr el test y verificar que pasa**

```bash
mvn -q -B test -Dtest=PersonControllerTest
cat target/surefire-reports/com.example.peopleapi.person.PersonControllerTest.txt
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`.

Si `getOne_returns404ProblemDetailWhenMissing` falla con 500 en vez de 404: `@WebMvcTest` sí incluye los beans `@RestControllerAdvice`; verificar que `GlobalExceptionHandler` está dentro del paquete `com.example.peopleapi` (escaneo por paquete base). Si `birthDate` sale como array `[1815,12,10]`: no debería ocurrir en Boot 4 (Jackson 3 ya escribe fechas ISO); confirmar que no se agregó configuración custom de Jackson.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/peopleapi/person/PersonController.java src/test/java/com/example/peopleapi/person/PersonControllerTest.java
git commit -m "feat: add PersonController with getAll and getOne endpoints"
```

---

### Task 5: Test de integración end-to-end, verificación manual y README

**Files:**
- Test: `src/test/java/com/example/peopleapi/PeopleApiIntegrationTest.java`
- Create: `README.md`
- Delete: `HELP.md` (generado por Initializr, redundante con el README)

**Interfaces:**
- Consumes: toda la aplicación (Tasks 1–4) y los datos de `data.sql`.
- Produces: suite verde completa y documentación de cómo correr y probar la API.

- [ ] **Step 1: Escribir el test de integración contra la H2 real**

Crear `src/test/java/com/example/peopleapi/PeopleApiIntegrationTest.java`:

```java
package com.example.peopleapi;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PeopleApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAll_returnsSeededPeopleFromDatabase() throws Exception {
        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].email").value(
                        containsInAnyOrder("ada@example.com", "alan@example.com", "grace@example.com")));
    }

    @Test
    void getOne_returnsSeededPerson() throws Exception {
        mockMvc.perform(get("/api/people/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1815-12-10"));
    }

    @Test
    void getOne_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/people/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Person not found"))
                .andExpect(jsonPath("$.detail").value("Person with id 999 not found"));
    }
}
```

- [ ] **Step 2: Correr toda la suite y verificar que pasa**

```bash
mvn -q -B test
grep -h "Tests run" target/surefire-reports/*.txt
```

Expected: 5 archivos, todos con `Failures: 0, Errors: 0`. Total: 1 + 4 + 4 + 5 + 3 = 17 tests.

- [ ] **Step 3: Levantar la app y probar manualmente los endpoints**

En una terminal (dejarla corriendo):

```bash
mvn -q -B spring-boot:run
```

Expected en el log: `Tomcat started on port 8080` y `H2 console available at '/h2-console'`.

En otra terminal:

```bash
curl -s http://localhost:8080/api/people
echo
curl -s http://localhost:8080/api/people/1
echo
curl -s -i http://localhost:8080/api/people/999 | head -1
```

Expected:
- Primer comando: array JSON con 3 objetos (`Ada`, `Alan`, `Grace`).
- Segundo: `{"id":1,"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","birthDate":"1815-12-10"}`.
- Tercero: `HTTP/1.1 404`.

Opcional: abrir `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:peopledb`, usuario `sa`, password vacío, y ejecutar `SELECT * FROM people;`. Luego detener la app con `Ctrl+C`.

- [ ] **Step 4: Escribir el README y borrar HELP.md**

Crear `README.md`:

````markdown
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
  "type": "about:blank",
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
````

Borrar el archivo de ayuda generado:

```bash
git rm -q HELP.md
```

- [ ] **Step 5: Commit final**

```bash
git add README.md src/test/java/com/example/peopleapi/PeopleApiIntegrationTest.java
git commit -m "test: add end-to-end integration test and project README"
```

---

## Self-Review (hecho al escribir el plan)

- **Cobertura del pedido:** Spring Boot 4 ✔ (4.1.2, Task 1) · Java 17 ✔ (Task 1) · Maven ✔ (Maven Wrapper, Task 1) · H2 ✔ (Task 1 config, Task 2 datos) · entidad `people` con JPA y atributos en inglés ✔ (Task 2: `Person` → tabla `people`, `firstName`, `lastName`, `email`, `birthDate`) · `getAll` ✔ y `getOne by id` ✔ (Task 4) · verificación end-to-end y docs (Task 5).
- **Placeholders:** ninguno; todo el código está inline.
- **Consistencia de tipos:** `Person(Long, String, String, String, LocalDate)` usado en Task 3 está definido en Task 2; `PersonResponse.from(Person)` definido en Task 3 y usado en `PersonService`; `PersonNotFoundException(Long)` con mensaje `"Person with id <id> not found"` es el mismo en Tasks 3, 4 y 5; `GlobalExceptionHandler` con `title = "Person not found"` coincide con las aserciones de Tasks 4 y 5; los paquetes de test (`WebMvcTest`, `AutoConfigureMockMvc`, `DataJpaTest`, `MockitoBean`) coinciden con los verificados en los jars 4.1.2.
