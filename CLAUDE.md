# OneDesK

Aplicación web de gestión de un negocio de cultivo indoor: catálogo y compras para los usuarios,
panel de tareas para los empleados de cada indoor, y administración con aprobaciones.

Este archivo describe cómo está hecho el proyecto y las convenciones que hay que respetar.
Sirve además como plantilla: los proyectos nuevos arrancan copiando estas decisiones.

---

## Stack

| Qué | Con qué |
|---|---|
| Lenguaje | Java 17 |
| Build | Maven (sin wrapper: se usa el `mvn` de la máquina) |
| Framework | Spring Boot 3.5.15 — starters `data-jpa`, `web`, `thymeleaf` |
| Base de datos | MySQL 8 (`mysql-connector-j`) |
| Vistas | Thymeleaf + Bootstrap 5.3 y bootstrap-icons por CDN |
| Tests | JUnit 5 + `spring-boot-starter-test` |

No se usa Spring Security, ni Lombok, ni DTOs, ni mappers. La sesión, los roles y la validación
están escritos a mano, y las entidades viajan directo a las vistas.

## Comandos

```bash
mvn test
```

```bash
mvn spring-boot:run
```

La aplicación queda en `http://localhost:8080`. Para correr un solo test:
`mvn test -Dtest=CompraServiceImplTest`.

## Estructura

```
src/main/java/com/OneDesK/
  App.java            arranque de Spring Boot
  modelo/             entidades JPA con las reglas de negocio adentro
  evento/             Evento y sus subclases (luz, regado, ventilador)
  repositories/       interfaces JpaRepository
  services/           interfaz + Impl por cada servicio
  web/                controllers, sesión, roles, manejo de errores
  excepciones/        una RuntimeException por regla de negocio
  helpers/            utilidades sin estado (ValidationUtils)
  config/             runners y tareas que corren al arrancar
src/main/resources/
  application.properties
  database/           scripts SQL del esquema
  templates/          vistas Thymeleaf
  static/css, static/img
```

---

## Convenciones

### Idioma

**Todo se escribe en español**: clases, métodos, variables, mensajes de excepción, textos de la UI,
comentarios y mensajes de commit. Los comentarios en el código Java van **sin tildes**; los textos
que ve la persona usuaria, con tildes y bien escritos.

### Comentarios

Un comentario explica **por qué**, nunca **qué**. Si la línea se entiende sola, no lleva comentario.
Lo que se comenta es la decisión difícil: por qué no se llama a `save()`, por qué el naming strategy
es el estándar, por qué la base de test se configura con `spring.factories` y no con un properties.

No se usan `HashMap` ni mapas en general: cuando hace falta asociar datos se arma una `List` de una
clase chica con nombre (como `LineaCompra`), que dice qué es cada cosa.

Los Javadoc son de una línea y describen la intención de negocio, no la firma del método:

```java
/** El administrador acepta la solicitud de registro y le asigna el tope de credito en el mismo paso. */
public void aprobar(int topeCredito) { ... }
```

### Commits

En español, en infinitivo, sin prefijos tipo `feat:` ni emojis. Describen el efecto, no los archivos:

```
Agregar la capa web con login, circuitos completos y aprobaciones
Sacar la contraseña de la base del repositorio
Cerrar huecos de reglas de negocio encontrados en la revision
```

---

## Cómo está resuelto cada problema

### Modelo: las reglas viven en las entidades

Las entidades no son estructuras de datos: validan en los setters y exponen métodos de negocio.
Los servicios coordinan y persisten, pero la regla la hace cumplir el objeto.

- `Persistible` es un `@MappedSuperclass` con el `id` (`IDENTITY`) y solo su getter.
- La jerarquía de personas usa `@Inheritance(strategy = JOINED)`.
- Un setter inválido tira `IllegalArgumentException`; una operación de negocio inválida tira la
  excepción específica del paquete `excepciones`.
- Las colecciones se devuelven con `Collections.unmodifiableList`: agregar o sacar pasa por los
  métodos que además recalculan lo que corresponda.
- Constructor sin argumentos con visibilidad de paquete, solo para Hibernate.

### Base de datos: el esquema se escribe a mano

Los scripts están en `src/main/resources/database/` (`create_db.sql`, `drop_db.sql`,
`crear_base_test.sql`). Hibernate **nunca** modifica el esquema:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
```

El naming strategy estándar es obligatorio: el de Spring Boot convierte a `snake_case` aunque el
nombre esté puesto a mano en `@Column`, y `fechaCompra` pasaba a buscarse como `fecha_compra`.

Al cambiar una entidad hay que actualizar el script SQL en el mismo commit: si no coinciden, la
aplicación no arranca (que es justamente para lo que está `validate`).

### Credenciales: fuera del repositorio

`db-local.properties` vive en la raíz, está en `.gitignore` y se carga con:

```properties
spring.config.import=optional:file:db-local.properties
```

Ahí van la contraseña de la base y las credenciales del primer administrador. Nunca se escribe una
credencial en `application.properties` ni en el código.

### Acceso: sesión y roles propios

- `enum Rol` (`USUARIO`, `EMPLEADO`, `ADMINISTRADOR`), cada uno con su página de inicio, y un
  `Rol.de(Persona)` que lo deduce del tipo.
- `Sesion` es un helper estático sobre `HttpSession`. Guarda **el id y no la entidad**, así cada
  pedido lee los datos actualizados de la base.
- `ControlDeAcceso` es un `HandlerInterceptor` registrado en `ConfiguracionWeb` solo sobre las rutas
  privadas: sin sesión manda al login, con el rol equivocado manda a la página de inicio de su rol.
  El login, el registro, el CSS y las imágenes quedan abiertos.
- La sesión viaja solo en la cookie (`server.servlet.session.tracking-modes=cookie`), para que el id
  no aparezca en la URL.
- Salir va por POST, para que un link suelto no cierre la sesión.

### Errores: un solo lugar

`ManejoDeErrores` es un `@ControllerAdvice` que convierte las excepciones de negocio en un flash
`error` y redirige a la página anterior. **Los controllers no llevan try/catch.** Del `Referer` se
usa solo el path, nunca el dominio, así no se puede redirigir a otro sitio.

Las vistas muestran el resultado con el fragmento `avisos` (`exito` en verde, `error` en rosa).

### Controllers

`@Controller` de Thymeleaf, nunca `@RestController`. Cada `@RequestParam` lleva su nombre escrito
(`@RequestParam("email") String email`): sin eso el nombre depende de que se compile con `-parameters`,
y hay un test que lo verifica (`NombresDeParametrosTest`). Después de un POST siempre se redirige.

### Servicios

Interfaz + `Impl`. La implementación lleva `@Service`, y cada método público que toca la base,
`@Transactional`. Las dependencias se inyectan con `@Autowired` sobre el campo.

Sobre una entidad ya gestionada **no se llama a `save()`**: el cambio se guarda solo al cerrar la
transacción. `save()` sobre algo ya guardado hace merge, y el merge rompe los borrados posteriores.

### Tareas de arranque y programadas

Van en `config/`, como `ApplicationRunner` (con `@Order` cuando importa la secuencia) o escuchando
`ApplicationReadyEvent`. Cada una lleva un flag en properties para poder apagarla —
`onedesk.eventos.activo`, `onedesk.demo.activo` — y los tests las apagan.

---

## Tests

Los tests corren **contra MySQL de verdad**, no contra H2: la base `onedesk_test`, separada de la
base real. Se crea una sola vez con `crear_base_test.sql` (como root) y después con `create_db.sql`.

La URL se fuerza con `BaseDeTest`, un `ContextCustomizerFactory` registrado en
`src/test/resources/META-INF/spring.factories`. **No se usa un `application.properties` de test**:
Eclipse carga los recursos de test al correr `App.java` y le cambiaba la base a la aplicación real.

Nada de mocks ni de tiempo simulado: los tests usan los services reales contra la base. Cada `@Test`
lleva arriba un comentario `//` que dice qué verifica.

Otras piezas:

- `DatosDePrueba` genera los valores que tienen que ser distintos en cada corrida (la base no acepta
  repetidos), con un sufijo aleatorio.
- `Navegador` (en `web/`) hace de navegador: pedidos HTTP reales contra la aplicación levantada,
  guardando la cookie de sesión y siguiendo las redirecciones. Cada persona del test tiene el suyo,
  así cada una tiene su sesión. Con eso están escritos los circuitos de punta a punta
  (`CircuitosWebTest`, `FlujoCompletoTest`).
- Hay tests que verifican las restricciones de la base (`RestriccionesDeLaBaseTest`) y el mapeo de
  las relaciones (`MapeoRelacionesTest`), no solo la lógica.

Todos los tests piden la misma configuración para que Spring reutilice el contexto entre clases.

## Vistas

`fragmentos.html` tiene lo que se repite: la cabeza, los dos menús (usuario y panel), el botón de
salir y los avisos. Cada página los incluye con `th:replace`.

El CSS (`static/css/onedesk.css`) **pisa las variables de Bootstrap**, no sus reglas, así el HTML
sigue usando las clases del framework. La identidad está declarada como variables en `:root`.

`spring.jpa.open-in-view=true` está puesto a propósito y declarado explícitamente: deja abierta la
sesión de Hibernate mientras se arma la vista, para que Thymeleaf pueda recorrer las relaciones.
