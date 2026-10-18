package com.OneDesK.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.DatosDePrueba;
import com.OneDesK.services.AdministradorService;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.UsuarioService;

// La API se prueba como la usaria otro programa: pedidos HTTP de verdad contra la aplicacion
// levantada, mirando el JSON que devuelve. La generacion de eventos se apaga: no tiene nada que ver.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "onedesk.eventos.activo=false" })
public class ApiRestTest {

	private static final String GENETICA = "API Kush";
	private static final String EMAIL = "api@test.com";
	private static final String CLAVE = "12345";

	@Autowired
	private TestRestTemplate navegador;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private AdministradorService administradorService;
	@Autowired
	private JdbcTemplate jdbc;

	private int producto;

	@BeforeEach
	public void cargarDatos() {
		producto = productoService.crearProducto(GENETICA, 1200).getId();
		productoService.fijarStock(producto, 50);
		int admin = administradorService.registrar("Ana", "Admin", DatosDePrueba.email(), CLAVE).getId();
		int usuario = usuarioService.registrar("Juan", "Perez", EMAIL, CLAVE).getId();
		administradorService.aprobarUsuario(admin, usuario, 5000);
	}

	@AfterEach
	public void borrarDatos() {
		jdbc.update("DELETE FROM Producto WHERE genetica = ?", GENETICA);
		jdbc.update("DELETE u, p, d FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " LEFT JOIN Deuda d ON d.ID = u.ID_DEUDA WHERE p.email = ?", EMAIL);
		jdbc.update("DELETE a, p FROM Administrador a JOIN Persona p ON p.ID = a.ID WHERE p.email LIKE 'prueba-%@test.com'");
	}

	// GET /api/productos devuelve el catalogo en JSON, con la genetica, el precio y el stock
	@Test
	public void elCatalogoSeLeePorLaApi() {
		ResponseEntity<String> respuesta = pedir(ProductoAPI.LISTAR_PRODUCTOS_URL);

		assertEquals(HttpStatus.OK, respuesta.getStatusCode());
		assertEquals(MediaType.APPLICATION_JSON, respuesta.getHeaders().getContentType());
		assertTrue(respuesta.getBody().contains("\"genetica\":\"" + GENETICA + "\""));
		assertTrue(respuesta.getBody().contains("\"precio\":1200"));
		assertTrue(respuesta.getBody().contains("\"stock\":50"));
	}

	// GET /api/usuarios devuelve los clientes aprobados, con su deuda y su tope
	@Test
	public void losClientesAprobadosSeLeenPorLaApi() {
		ResponseEntity<String> respuesta = pedir(UsuarioAPI.LISTAR_USUARIOS_URL);

		assertEquals(HttpStatus.OK, respuesta.getStatusCode());
		assertTrue(respuesta.getBody().contains("\"email\":\"" + EMAIL + "\""));
		assertTrue(respuesta.getBody().contains("\"topeCredito\":5000"));
		assertTrue(respuesta.getBody().contains("\"aprobado\":true"));
	}

	// La API dice que devuelve JSON (produces): a quien pida otro formato se le avisa con un 406,
	// en vez de mandarle algo que no va a poder leer
	@Test
	public void laApiSoloDevuelveJson() {
		HttpHeaders cabeceras = cabecerasJson();
		cabeceras.setAccept(List.of(MediaType.APPLICATION_XML));

		ResponseEntity<String> respuesta = navegador.exchange(ProductoAPI.LISTAR_PRODUCTOS_URL, HttpMethod.GET,
				new HttpEntity<>(cabeceras), String.class);

		assertEquals(HttpStatus.NOT_ACCEPTABLE, respuesta.getStatusCode());
	}

	// La otra cara del consumes: quien no dice en que formato habla recibe un 415. Le pasa al
	// navegador, que en un GET no manda Content-Type; Postman y los clientes de API si lo mandan.
	@Test
	public void sinDecirElFormatoLaApiNoContesta() {
		ResponseEntity<String> respuesta = navegador.getForEntity(ProductoAPI.LISTAR_PRODUCTOS_URL, String.class);

		assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, respuesta.getStatusCode());
	}

	// Lo que no puede salir nunca: la contrasenia de nadie, ni cifrada ni en claro
	@Test
	public void laContraseniaNoViajaEnLaApi() {
		String usuarios = pedir(UsuarioAPI.LISTAR_USUARIOS_URL).getBody();

		assertFalse(usuarios.contains("contrasenia"), "El JSON trae el campo de la contrasenia");
		assertFalse(usuarios.contains(CLAVE), "El JSON trae la contrasenia");
	}

	/** Un pedido como el de Postman: diciendo que habla JSON, que es lo que la API pide (consumes). */
	private ResponseEntity<String> pedir(String url) {
		return navegador.exchange(url, HttpMethod.GET, new HttpEntity<>(cabecerasJson()), String.class);
	}

	private HttpHeaders cabecerasJson() {
		HttpHeaders cabeceras = new HttpHeaders();
		cabeceras.setContentType(MediaType.APPLICATION_JSON);
		return cabeceras;
	}
}
