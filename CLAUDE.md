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
| Vistas | Thymeleaf + `thymeleaf-layout-dialect` + Bootstrap 5.3 y bootstrap-icons por CDN |
| Desarrollo | `spring-boot-devtools` (reinicia al compilar y no cachea plantillas ni mensajes) |
| Tests | JUnit 5 + `spring-boot-starter-test` |

El acceso lo maneja **Spring Security**: login, contraseñas encriptadas con BCrypt, autorización por
permisos y, en la API, tokens **JWT** (`jjwt`). Ver "Acceso".

No se usa Lombok, ni Bean Validation. La validación de los formularios está escrita a mano, y las
entidades viajan directo a las vistas de Thymeleaf. Por la API, en cambio, viajan DTOs: ver
"API REST".

Las dos dependencias que no vienen en un starter llevan su versión escrita, porque Spring Boot 3 ya
no administra la del layout dialect. Al sumar una dependencia hay que regenerar el `.classpath` de
Eclipse: `mvn eclipse:clean eclipse:eclipse` y F5 en el proyecto.

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
  enums/              Rol y Permiso: quien es cada persona y que puede hacer
  seguridad/          login, permisos y tokens JWT (lo propio de Spring Security)
  repositories/       interfaces JpaRepository
  services/           interfaz + Impl por cada servicio
  web/                controllers, sesión, roles, manejo de errores
    formularios/      una clase por formulario, con sus campos y nada más
    validadores/      un Validator de Spring por formulario con reglas
    rest/             las APIs: un @RestController por recurso
  dtos/               lo que viaja por la API: datos sueltos, sin reglas ni relaciones
  excepciones/        una RuntimeException por regla de negocio, con su clave de mensaje
  helpers/            utilidades sin estado (ValidationUtils)
  config/             runners y tareas que corren al arrancar
src/main/resources/
  application.properties
  messages.properties, messages_en.properties   todos los textos, en los dos idiomas
  database/           scripts SQL del esquema
  templates/          layout.html, fragmentos.html y una página por pantalla
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

Los scripts están en `src/main/resources/database/` y van **versionados**: `create_db_1.0.0.sql` es
el esquema original y cada cambio agrega una versión nueva con **solo lo que cambia**, más su
`drop_db_1.0.X.sql` (que lo deshace) y, si hace falta, su `init_db_1.0.X.sql` (los datos que esa
versión necesita, y la migración de los que ya estaban). Para armar la base se corren en orden:

```
create_db_1.0.0.sql → create_db_1.0.1.sql → init_db_1.0.1.sql → ...
```

`crear_base_test.sql` queda aparte: crea la base de los tests y se corre una sola vez, como root.

Hibernate **nunca** modifica el esquema:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
```

El naming strategy estándar es obligatorio: el de Spring Boot convierte a `snake_case` aunque el
nombre esté puesto a mano en `@Column`, y `fechaCompra` pasaba a buscarse como `fecha_compra`.

Al cambiar una entidad hay que escribir la versión nueva del script en el mismo commit, y correrla
en las dos bases (`onedesk` y `onedesk_test`): si el esquema no coincide con las entidades, la
aplicación no arranca (que es justamente para lo que está `validate`).

### Credenciales: fuera del repositorio

`db-local.properties` vive en la raíz, está en `.gitignore` y se carga con:

```properties
spring.config.import=optional:file:db-local.properties
```

Ahí van la contraseña de la base, las credenciales del primer administrador y la clave con la que se
firman los tokens JWT (`onedesk.jwt.secreto`). Nunca se escribe una credencial en
`application.properties` ni en el código.

### Acceso: Spring Security con roles y permisos propios

Quién puede entrar a cada cosa está en `config/ConfiguracionDeSeguridad`, con **dos reglas
separadas** porque son dos mundos: la API no tiene sesión ni formulario (cada pedido trae su token)
y la web sí. Las piezas propias viven en `seguridad/`.

Las zonas **no se autorizan por rol sino por permiso**: la página pide lo que hay que saber hacer
para verla, y qué rol tiene ese permiso lo decide el enum `Rol`.

```java
.requestMatchers(bajoLaRuta(AdminController.ADMIN_URL)).hasAuthority(Permiso.ADMINISTRAR_NEGOCIO.name())
```

**La web**

- `formLogin` sobre las páginas propias: `loginPage` es `/` y el formulario va a `/ingresar`, con
  los campos `email` y `contrasenia`. El controller ya no recibe ese POST, solo muestra la página.
- `DetallesDePersonaService` (un `UserDetailsService`) busca por email en `Persona`, y
  `DetallesDePersona` traduce cada rol a dos cosas: el rol con el prefijo `ROLE_` que Spring espera,
  y cada uno de sus permisos, que son con los que se autorizan las páginas.
- Que un cliente sin aprobar no entre **lo decide el modelo**: `Persona.estaHabilitada()` es `true` y
  `Usuario` la responde con `isAprobado()`. Spring lo rechaza con `DisabledException`, y
  `FalloAlIngresar` manda a `/?pendiente`; cualquier otro fallo va a `/?error`, igual para el email
  que no existe y para la contraseña equivocada, para no decir si esa cuenta está.
- `ExitoAlIngresar` guarda en la sesión el id y el nombre (lo que las páginas releen en cada pedido)
  y manda a la página de inicio del rol.
- `AccesoDenegado`: a quien ya entró y pide una zona que no le toca no se le muestra un 403, se lo
  manda a su página de inicio. La excepción es un pedido sin token CSRF, que sí se rechaza con 403.
- **CSRF activo**: Thymeleaf agrega el campo `_csrf` en cada `<form th:action=...>`, así que las
  páginas no cambian. El `Navegador` de los tests lo lee del HTML y lo manda, como un navegador.

**La API**

- `POST /api/login` (en `AutenticacionAPI`) devuelve el token; de ahí en más va en la cabecera
  `Authorization: Bearer ...`. Solo ese login y el registro (`/api/usuarios/register`) son públicos.
- `FiltroJwt` lee el token de cada pedido y, si es válido, deja dicho quién lo mandó; no rechaza a
  nadie, de eso se encarga la regla de autorización. Sin token la API contesta **401**, con token
  pero sin el permiso, **403**: no hay redirecciones, del otro lado no hay navegador.
- `JwtService` firma y revisa. La clave sale de `onedesk.jwt.secreto` (en `db-local.properties`); si
  no hay ninguna se genera una al arrancar y los tokens duran hasta el próximo reinicio.
- Los errores de la API los traduce `ManejoDeErroresApi`, un `@RestControllerAdvice` solo para
  `web/rest`: el motivo en texto y el código que corresponde, no una redirección.

**Las contraseñas**

- Se guardan encriptadas con BCrypt. El bean está en `config/ConfiguracionDeContrasenias`, aparte de
  la seguridad web, para que los tests de `@DataJpaTest` lo importen sin levantar toda la web.
- Encripta **la entidad**: `persona.encriptarContrasenia(encriptador)`, que los services llaman
  apenas la crean. Si ya está encriptada no hace nada, así no se encripta dos veces.
- `MigracionDeContrasenias` encripta al arrancar las que quedaron en claro de antes
  (`onedesk.contrasenias.migrar`). Es idempotente: puede quedar prendida.

**Lo de antes que sigue igual**

- En `enums/`, `Rol` (`USUARIO`, `EMPLEADO`, `ADMINISTRADOR`) y `Permiso` (`COMPRAR`,
  `CUIDAR_PLANTAS`, `ADMINISTRAR_NEGOCIO`): cada rol se declara con los permisos que tiene y
  contesta `puede(Permiso)`; nadie recorre sus permisos desde afuera. El rol también lleva escrito
  su `id`, que es el de la tabla `Rol`: así reordenar el enum no cambia lo que ya está guardado.
- Los roles están en la base (`Rol` y `Persona_Rol`) y los toma **cada persona al construirse**
  (`tomarRol(...)` en el constructor de `Usuario`, `EmpleadoIndoor` y `Administrador`), no un
  `instanceof` de afuera. `RolComoId` es el `AttributeConverter` que los guarda por su id.
- A qué página entra cada rol lo dice `PaginaDeInicio`, que vive en `web/` porque es una dirección.
- `Sesion` es un helper estático sobre `HttpSession`. Guarda **el id y no la entidad**, así cada
  pedido lee los datos actualizados de la base. La llena `ExitoAlIngresar` y nadie más.
- La sesión viaja solo en la cookie (`server.servlet.session.tracking-modes=cookie`), para que el id
  no aparezca en la URL.
- **Al ingresar, la sesión cambia de id** (`sessionFixation().changeSessionId()`): si alguien le fijó
  uno a la persona antes de que entrara, ese id deja de servir. Lo que ya estaba guardado, como el
  idioma, se conserva.
- Se cierra sola después de media hora sin usarla (`server.servlet.session.timeout=30m`).
- Salir va por POST, para que un link suelto no cierre la sesión.

### Errores: un solo lugar

`ManejoDeErrores` es un `@ControllerAdvice` que convierte las excepciones de negocio en un flash
`error` y redirige a la página anterior. **Los controllers no llevan try/catch.** Del `Referer` se
usa solo el path, nunca el dominio, así no se puede redirigir a otro sitio.

Las excepciones no llevan el texto del error sino **su clave y sus datos**, y `ManejoDeErrores` las
traduce al idioma de quien las va a leer:

```java
throw new OperacionInvalidaException("error.indoor.lleno", nombre, plantasEnCultivo(), capacidad);
```

`ExcepcionDeNegocio` es la base de las de negocio y `DatoInvalidoException` hace lo mismo con las
validaciones del modelo, sin dejar de ser una `IllegalArgumentException`.

Las vistas muestran el resultado con el fragmento `avisos` (`exito` en verde, `error` en rosa).

### Controllers

`@Controller` de Thymeleaf, nunca `@RestController`. Todos heredan de `BaseController`, que junta lo
que comparten: el nombre con el que viaja el formulario (`FORM_ATTRIBUTE`), los de los avisos
(`EXITO`, `ERROR`, `ERRORES`), `redirect(ruta)` y los dos métodos de validación. También lo hereda
`ManejoDeErrores`. Después de un POST siempre se redirige, salvo cuando hay que volver a mostrar un
formulario con sus errores.

Cada dirección es una constante del controller, y se usa en la anotación y en los redirect:

```java
public static final String MIS_COMPRAS_URL = "/mis-compras";
public static final String PAGAR_URL = MIS_COMPRAS_URL + "/{compraId}/pagar";

@PostMapping(value = PAGAR_URL)
public String pagar(@PathVariable("compraId") int compraId, ...) {
    ...
    return redirect(MIS_COMPRAS_URL);
}
```

Las usan también `Rol` (la página de inicio de cada rol), `ControlDeAcceso` y `ConfiguracionWeb`.
En las plantillas y en los tests las direcciones van escritas: si el test usara la constante, un
error en la constante lo haría pasar igual.

Cada `@RequestParam`, `@PathVariable` y `@ModelAttribute` lleva su nombre escrito: sin eso el nombre
depende de que se compile con `-parameters`, y hay un test que lo verifica
(`NombresDeParametrosTest`).

### Formularios

Un formulario de varios campos no se recibe con varios `@RequestParam` sino con su clase de
`web/formularios`, que son objetos con campos, getters y setters y nada más:

```java
@PostMapping(value = PLANTAR_URL)
public String plantar(@Validated @ModelAttribute(FORM_ATTRIBUTE) FormularioDePlanta form,
        BindingResult errores, ...) {
```

Las acciones de un solo dato (aprobar la compra 14) siguen con `@RequestParam`.

Las páginas de un solo formulario (ingresar, crear cuenta) usan `th:object` y `th:field`, y el GET
deja el formulario vacío en el modelo. Los paneles, que tienen muchos formularios juntos, usan los
`name` de siempre: el binding funciona igual y el HTML no se llena de atributos.

### Validaciones

Lo que se puede mirar del formulario (campos vacíos, números que no dan, dos contraseñas que no
coinciden) va en un validador de `web/validadores`, que extiende `ValidadorDeFormulario<T>`. Pueden
inyectar repositorios: el de registro avisa que el email ya está tomado antes de intentar guardar.

Cada controller engancha los suyos con `@InitBinder`, y los rechazos se escriben con la clave del
mensaje, nunca con el texto:

```java
@InitBinder(value = FORM_ATTRIBUTE)
public void engancharValidadores(WebDataBinder binder) {
    enganchar(binder, validadorDeIngreso, validadorDeRegistro);
}
```

El motivo se muestra abajo del campo con `th:errors` en las páginas de un solo formulario, y arriba,
todos juntos, en los paneles (`avisar(...)` de `BaseController`).

**Las reglas del negocio no se repiten acá**: que el indoor no esté lleno o que alcance el stock lo
sigue haciendo cumplir el modelo, que es el único que puede hacerlo siempre.

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
base real. Se crea una sola vez con `crear_base_test.sql` (como root) y después con los
`create_db_1.0.X.sql` en orden, igual que la base real.

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
- Los validadores se prueban solos, sin levantar Spring (`ValidadoresTest`), salvo el de registro,
  que consulta la base y por eso corre con `@DataJpaTest` (`ValidadorDeRegistroTest`).
- La API se prueba con token de verdad: `BaseContextTest.entrarComo(email, clave)` hace el login,
  se queda con el token y lo manda en los pedidos que siguen. `SeguridadDeLaApiTest` prueba lo
  contrario: sin token, con un token falso, con la cuenta pendiente y con el cliente entrando donde
  no le toca.

Todos los tests piden la misma configuración para que Spring reutilice el contexto entre clases.

Dos cosas que hacen perder tiempo si no se saben: cuando Maven compila sobre una corrida anterior
pueden quedar clases viejas y los tests fallan con un `NoClassDefFoundError` de algo que sí existe
(se arregla con `mvn clean test`), y conviene cerrar la aplicación antes de correr la suite.

## API REST

Las APIs viven en `web/rest`, una por recurso, con el mismo formato que los controllers: la
dirección es una constante y el método la usa en la anotación.

```java
@RestController
public class ProductoAPI {

    public static final String LISTAR_PRODUCTOS_URL = "/api/productos";

    @GetMapping(value = LISTAR_PRODUCTOS_URL,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductoDTO>> listar() { ... }
}
```

**Por la API no viajan entidades, viajan DTOs.** Un DTO (`dtos/`) hereda de `BaseDTO` (que trae el
`id`) y tiene campos, getters y setters y nada más. Los DTO se heredan entre sí cuando una vista
necesita más datos: `UsuarioAdminDTO extends UsuarioDTO` agrega lo que solo ve la administración.

Los arma **ModelMapper**, copiando los campos que se llaman igual en la entidad y en el DTO. Por eso
las propiedades se nombran igual de los dos lados (`getNombreCompleto()` en `Persona`).

La entidad no construye el DTO: implementa `Exponible` y solo dice **cuál** le corresponde.

```java
@Override
public Class<? extends DTO> dtoParaApi() {
    return UsuarioDTO.class;
}

@Override
public Class<? extends DTO> dtoParaAdmin() {
    return UsuarioAdminDTO.class;
}
```

Lo que **no es una copia** (la deuda, que se recalcula; las plantas en cultivo, que son una cuenta)
se completa con un `setPostConverter` en `ConfiguracionDeMapeo`, que es el único lugar donde está
escrito. El mapeador usa la estrategia estricta: solo copia lo que coincide exacto, nada de adivinar
por parecido.

La conversión la hace el service, no la API: `ExponibleServiceImpl` la escribe una sola vez y cada
service solo dice qué entidades muestra.

```java
public class ProductoServiceImpl extends ExponibleServiceImpl<Producto> implements ProductoService {

    /** Por la API se muestran las geneticas con stock, que son las que se pueden comprar. */
    @Override
    protected List<Producto> aExponer() {
        return listarProductos();
    }
```

Está en el service porque la conversión necesita la transacción abierta: armar el DTO de un cliente
le pregunta la deuda, que se recalcula leyendo sus compras. La API queda en una línea:
`servicio.exponer()`, y devuelve `List<DTO>`.

Un detalle de los tests: los que levantan solo un pedazo de la aplicación (`@DataJpaTest`) no cargan
las configuraciones, así que los que usan un service que expone necesitan
`@Import(ConfiguracionDeMapeo.class)`.

Así la API no arrastra las relaciones de JPA ni se rompe cuando el modelo cambia, y de paso no hay
que ir tapando campos con `@JsonIgnore`. Los de `Persona.getContrasenia()` y `Usuario.getCompras()`
quedaron igual, por si alguna vez se serializa una entidad por error.

Todas las direcciones de la API piden token menos el login y el registro: ver "Acceso".

Dos cosas de los media types, probadas: `produces` hace que a quien pida otro formato se le conteste
406, y `consumes` deja afuera a quien no mande `Content-Type`. En un GET eso significa que el
navegador recibe 415 y hay que pedirlo desde Postman o con el header puesto.

## Vistas

El esqueleto de todas las páginas está en `layout.html`, con tres huecos: `menu`, `contenido` y
`scripts`. Cada página se declara con `layout:decorate="~{layout}"` y escribe solo lo suyo, incluido
su `<title>`, que se junta con el del layout ("Catálogo" queda como "Catálogo · OneDesK").

`fragmentos.html` tiene las piezas que se repiten entre páginas: los dos menús, salir, el selector
de idioma, la portada con foto, las tarjetas de números, el estado de una compra, un evento y los
avisos. Cada página las incluye con `th:replace`. **Layout para la estructura, fragmentos para las
piezas.**

## Textos e idiomas

Ningún texto se escribe en el HTML ni en el código: todos salen de `messages.properties` (español,
el de por defecto) y `messages_en.properties`, con las mismas claves. En las plantillas se usan con
`#{clave}`, y los que llevan datos con `#{clave(${dato})}`.

El idioma se cambia con `?idioma=en` desde cualquier página y queda guardado en la sesión
(`LocaleChangeInterceptor` y `SessionLocaleResolver` en `ConfiguracionWeb`). El selector está en los
menús y en las dos páginas que no tienen menú. Como Thymeleaf 3.1 ya no da acceso al pedido, la
dirección actual llega al modelo por `RutaActual`, un `@ControllerAdvice`.

En el código los textos se piden con `Mensajes`: `mensajes.de("exito.indoor.creado", idioma, nombre)`.
Ojo con `MessageFormat`: a los números les pone separador de miles, así que los ids van como
`{0,number,#}`.

El CSS (`static/css/onedesk.css`) **pisa las variables de Bootstrap**, no sus reglas, así el HTML
sigue usando las clases del framework. La identidad está declarada como variables en `:root`.

`spring.jpa.open-in-view=true` está puesto a propósito y declarado explícitamente: deja abierta la
sesión de Hibernate mientras se arma la vista, para que Thymeleaf pueda recorrer las relaciones.
