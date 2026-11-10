package com.OneDesK.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.BaseContextTest;
import com.OneDesK.DatosDePrueba;
import com.OneDesK.services.AdministradorService;
import com.OneDesK.services.UsuarioService;
import com.OneDesK.web.formularios.FormularioDeIngreso;

// La API se usa con un token: se pide una vez con email y contrasenia y despues viaja en cada
// pedido. Aca se prueba quien lo consigue, que abre y que no.
public class SeguridadDeLaApiTest extends BaseContextTest {

	private static final String CLAVE = "12345";

	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private AdministradorService administradorService;
	@Autowired
	private JdbcTemplate jdbc;

	private String cliente;
	private String administrador;
	private String pendiente;

	@BeforeEach
	public void cargarPersonas() {
		cliente = DatosDePrueba.email();
		administrador = DatosDePrueba.email();
		pendiente = DatosDePrueba.email();
		int admin = administradorService.registrar("Ana", "Admin", administrador, CLAVE).getId();
		int usuario = usuarioService.registrar("Juan", "Perez", cliente, CLAVE).getId();
		administradorService.aprobarUsuario(admin, usuario, 5000);
		usuarioService.registrar("Sin", "Aprobar", pendiente, CLAVE);
	}

	@AfterEach
	public void borrarPersonas() {
		jdbc.update("DELETE u, p, d FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " LEFT JOIN Deuda d ON d.ID = u.ID_DEUDA WHERE p.email LIKE 'prueba-%@test.com'");
		jdbc.update("DELETE a, p FROM Administrador a JOIN Persona p ON p.ID = a.ID WHERE p.email LIKE 'prueba-%@test.com'");
	}

	// Sin token la API no contesta nada: 401 y ninguna redireccion, que del otro lado no hay navegador
	@Test
	public void sinTokenLaApiNoContesta() {
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(ProductoAPI.LISTAR_PRODUCTOS_URL).statusCode());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(UsuarioAPI.LISTAR_USUARIOS_URL).statusCode());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(IndoorAPI.LISTAR_INDOORS_URL).statusCode());
	}

	// El login devuelve el token, y con el token la misma direccion ya contesta
	@Test
	public void conElTokenLaApiContesta() {
		entrarComo(cliente, CLAVE);

		assertNotNull(getToken());
		assertEquals(HttpStatus.OK.value(), get(ProductoAPI.LISTAR_PRODUCTOS_URL).statusCode());
	}

	// El token lleva firma: uno inventado, o uno al que le cambiaron una letra, no sirve
	@Test
	public void unTokenFalsoNoSirve() {
		entrarComo(cliente, CLAVE);
		String verdadero = getToken();

		conToken("abc.def.ghi");
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(ProductoAPI.LISTAR_PRODUCTOS_URL).statusCode());

		conToken(cambiarleUnaLetra(verdadero));
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(ProductoAPI.LISTAR_PRODUCTOS_URL).statusCode());
	}

	// Con la contrasenia equivocada no se consigue ningun token
	@Test
	public void conLaContraseniaEquivocadaNoHayToken() {
		HttpResponse<String> respuesta = login(cliente, "otra-clave");

		assertEquals(HttpStatus.UNAUTHORIZED.value(), respuesta.statusCode());
		assertTrue(respuesta.body().contains("Email o contraseña incorrectos"));
	}

	// Un email que no existe da el mismo error, sin decir que la cuenta no esta
	@Test
	public void unEmailInexistenteDaElMismoError() {
		HttpResponse<String> respuesta = login("nadie@test.com", CLAVE);

		assertEquals(HttpStatus.UNAUTHORIZED.value(), respuesta.statusCode());
		assertTrue(respuesta.body().contains("Email o contraseña incorrectos"));
	}

	// La cuenta que todavia no fue aprobada no entra, y se le dice por que
	@Test
	public void unaCuentaPendienteNoRecibeToken() {
		HttpResponse<String> respuesta = login(pendiente, CLAVE);

		assertEquals(HttpStatus.UNAUTHORIZED.value(), respuesta.statusCode());
		assertTrue(respuesta.body().contains("esperando"));
	}

	// Entrar no alcanza para todo: la administracion pide el permiso de administrar
	@Test
	public void unClienteNoEntraALaApiDeAdministracion() {
		entrarComo(cliente, CLAVE);

		assertEquals(HttpStatus.FORBIDDEN.value(), get(UsuarioAdminAPI.LISTAR_USUARIOS_ADMIN_URL).statusCode());
	}

	@Test
	public void elAdministradorSiEntraALaApiDeAdministracion() {
		entrarComo(administrador, CLAVE);

		assertEquals(HttpStatus.OK.value(), get(UsuarioAdminAPI.LISTAR_USUARIOS_ADMIN_URL).statusCode());
	}

	// Pedir una cuenta es lo unico que se puede hacer sin token: si no, nadie podria registrarse
	@Test
	public void pedirUnaCuentaNoPideToken() {
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(UsuarioAPI.LISTAR_USUARIOS_URL).statusCode());
		// el registro contesta 400 porque el cuerpo va vacio, no 401: la direccion esta abierta
		assertNotEquals(HttpStatus.UNAUTHORIZED.value(),
				post(UsuarioAPI.GUARDAR_USUARIO_URL, "{}").statusCode());
	}

	// Dos tokens de personas distintas no son el mismo: cada uno dice de quien es
	@Test
	public void cadaTokenEsDeSuPersona() {
		entrarComo(cliente, CLAVE);
		String delCliente = getToken();
		entrarComo(administrador, CLAVE);

		assertNotEquals(delCliente, getToken());
		salir();
		assertEquals(HttpStatus.UNAUTHORIZED.value(), get(ProductoAPI.LISTAR_PRODUCTOS_URL).statusCode());
	}

	// La contrasenia no viaja ni se guarda en claro: en la base esta el hash de BCrypt
	@Test
	public void laContraseniaSeGuardaEncriptada() {
		String guardada = jdbc.queryForObject("SELECT contraseña FROM Persona WHERE email = ?",
				String.class, cliente);

		assertNotEquals(CLAVE, guardada);
		assertTrue(guardada.startsWith("$2"), "La contrasenia no quedo encriptada con BCrypt");
	}

	private HttpResponse<String> login(String email, String contrasenia) {
		FormularioDeIngreso credenciales = new FormularioDeIngreso();
		credenciales.setEmail(email);
		credenciales.setContrasenia(contrasenia);

		return post(AutenticacionAPI.LOGIN_URL, comoJson(credenciales));
	}

	private String cambiarleUnaLetra(String token) {
		char primera = token.charAt(0);
		return (primera == 'a' ? 'b' : 'a') + token.substring(1);
	}
}
