package com.OneDesK.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.BaseContextTest;
import com.OneDesK.DatosDePrueba;
import com.OneDesK.dtos.IndoorDTO;
import com.OneDesK.dtos.ProductoDTO;
import com.OneDesK.dtos.UsuarioAdminDTO;
import com.OneDesK.dtos.UsuarioDTO;
import com.OneDesK.web.formularios.FormularioDeRegistro;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.services.AdministradorService;
import com.OneDesK.services.IndoorService;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.UsuarioService;

// La API se prueba como la usaria otro programa: pedidos HTTP de verdad contra la aplicacion
// levantada, mirando el JSON que devuelve. El puerto y el cliente los pone BaseContextTest.
public class ApiRestTest extends BaseContextTest {

	private static final String GENETICA = "API Kush";
	private static final String EMAIL = "api@test.com";
	private static final String CLAVE = "12345";

	@Autowired
	private ProductoService productoService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private AdministradorService administradorService;
	@Autowired
	private IndoorService indoorService;
	@Autowired
	private JdbcTemplate jdbc;

	private int producto;
	private Indoor indoor;

	@BeforeEach
	public void cargarDatos() {
		producto = productoService.crearProducto(GENETICA, 1200).getId();
		productoService.fijarStock(producto, 50);
		int admin = administradorService.registrar("Ana", "Admin", DatosDePrueba.email(), CLAVE).getId();
		int usuario = usuarioService.registrar("Juan", "Perez", EMAIL, CLAVE).getId();
		administradorService.aprobarUsuario(admin, usuario, 5000);
		indoor = indoorService.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
	}

	@AfterEach
	public void borrarDatos() {
		jdbc.update("DELETE FROM Producto WHERE genetica = ?", GENETICA);
		jdbc.update("DELETE FROM Indoor WHERE ID = ?", indoor.getId());
		jdbc.update("DELETE u, p, d FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " LEFT JOIN Deuda d ON d.ID = u.ID_DEUDA WHERE p.email = ?", EMAIL);
		jdbc.update("DELETE u, p, d FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " LEFT JOIN Deuda d ON d.ID = u.ID_DEUDA WHERE p.email LIKE 'prueba-%@test.com'");
		jdbc.update("DELETE a, p FROM Administrador a JOIN Persona p ON p.ID = a.ID WHERE p.email LIKE 'prueba-%@test.com'");
	}

	// GET /api/productos devuelve el catalogo en JSON, con la genetica, el precio y el stock
	@Test
	public void elCatalogoSeLeePorLaApi() {
		HttpResponse<String> respuesta = get(ProductoAPI.LISTAR_PRODUCTOS_URL);

		assertEquals(HttpStatus.OK.value(), respuesta.statusCode());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				respuesta.headers().firstValue("Content-Type").orElse(""));

		List<ProductoDTO> productos = leerLista(respuesta, ProductoDTO.class);
		ProductoDTO elNuestro = buscar(productos, dto -> GENETICA.equals(dto.getGenetica()));

		assertEquals(1200, elNuestro.getPrecio());
		assertEquals(50, elNuestro.getStock());
		assertEquals(producto, elNuestro.getId());
	}

	// GET /api/usuarios devuelve los clientes aprobados, con su deuda y su tope
	@Test
	public void losClientesAprobadosSeLeenPorLaApi() {
		HttpResponse<String> respuesta = get(UsuarioAPI.LISTAR_USUARIOS_URL);

		assertEquals(HttpStatus.OK.value(), respuesta.statusCode());

		List<UsuarioDTO> usuarios = leerLista(respuesta, UsuarioDTO.class);
		UsuarioDTO elNuestro = buscar(usuarios, dto -> EMAIL.equals(dto.getEmail()));

		assertEquals("Juan Perez", elNuestro.getNombreCompleto());
		assertEquals(5000, elNuestro.getTopeCredito());
		assertEquals(0, elNuestro.getDeuda());
	}

	// La API dice que devuelve JSON (produces): a quien pida otro formato se le avisa con un 406,
	// en vez de mandarle algo que no va a poder leer
	@Test
	public void laApiSoloDevuelveJson() {
		HttpResponse<String> respuesta = getPidiendo(ProductoAPI.LISTAR_PRODUCTOS_URL, MediaType.APPLICATION_XML);

		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), respuesta.statusCode());
	}

	// La otra cara del consumes: quien no dice en que formato habla recibe un 415. Le pasa al
	// navegador, que en un GET no manda Content-Type; Postman y los clientes de API si lo mandan.
	@Test
	public void sinDecirElFormatoLaApiNoContesta() {
		HttpResponse<String> respuesta = getSinFormato(ProductoAPI.LISTAR_PRODUCTOS_URL);

		assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), respuesta.statusCode());
	}

	// La API manda DTOs, no entidades: sale lo que el DTO tiene y nada mas. Ni la contrasenia,
	// ni las compras, ni el estado de aprobado, que son cosas de adentro de la aplicacion.
	@Test
	public void laApiMandaSoloLoQueElDtoTiene() {
		String usuarios = get(UsuarioAPI.LISTAR_USUARIOS_URL).body();

		assertFalse(usuarios.contains("contrasenia"), "El JSON trae el campo de la contrasenia");
		assertFalse(usuarios.contains(CLAVE), "El JSON trae la contrasenia");
		assertFalse(usuarios.contains("compras"), "El JSON trae las compras");
		assertFalse(usuarios.contains("aprobado"), "El JSON trae el estado de aprobado");
	}

	// El DTO del administrador hereda del publico: trae lo mismo y ademas lo suyo, y tambien
	// muestra las solicitudes que todavia no se aprobaron
	@Test
	public void elAdministradorVeMasDatosDeCadaCliente() {
		String pendiente = DatosDePrueba.email();
		usuarioService.registrar("Sin", "Aprobar", pendiente, CLAVE);

		List<UsuarioAdminDTO> usuarios = leerLista(get(UsuarioAdminAPI.LISTAR_USUARIOS_ADMIN_URL),
				UsuarioAdminDTO.class);
		UsuarioAdminDTO cliente = buscar(usuarios, dto -> EMAIL.equals(dto.getEmail()));
		UsuarioAdminDTO solicitud = buscar(usuarios, dto -> pendiente.equals(dto.getEmail()));

		// lo que ya traia el DTO publico, que hereda
		assertEquals("Juan Perez", cliente.getNombreCompleto());
		assertEquals(5000, cliente.getTopeCredito());
		// y lo que agrega el del administrador
		assertTrue(cliente.isAprobado());
		assertEquals(0, cliente.getDeudaComprometida());
		assertEquals(0, cliente.getCompras());
		// la solicitud pendiente, que en la API publica no aparece
		assertFalse(solicitud.isAprobado());
		assertFalse(get(UsuarioAPI.LISTAR_USUARIOS_URL).body().contains(pendiente));
	}

	// GET /api/indoors muestra como viene cada indoor
	@Test
	public void losIndoorsSeLeenPorLaApi() {
		HttpResponse<String> respuesta = get(IndoorAPI.LISTAR_INDOORS_URL);

		assertEquals(HttpStatus.OK.value(), respuesta.statusCode());

		List<IndoorDTO> indoors = leerLista(respuesta, IndoorDTO.class);
		IndoorDTO elNuestro = buscar(indoors, dto -> indoor.getNombre().equals(dto.getNombre()));

		assertEquals(DatosDePrueba.CAPACIDAD, elNuestro.getCapacidad());
		assertEquals(0, elNuestro.getPlantasEnCultivo());
		assertEquals(0, elNuestro.getEventosPendientes());
	}

	// POST /api/usuarios crea la solicitud de cuenta y contesta 201, sin cuerpo
	@Test
	public void sePuedePedirUnaCuentaPorLaApi() {
		String nuevo = DatosDePrueba.email();

		HttpResponse<String> respuesta = registrar(solicitud(nuevo));

		assertEquals(HttpStatus.CREATED.value(), respuesta.statusCode());
		assertEquals(1, contar(nuevo));
		// queda pendiente, igual que cuando la piden desde la pagina
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " WHERE p.email = ? AND u.aprobado = 1", Integer.class, nuevo));
	}

	// Una solicitud que no pasa las validaciones vuelve con 400 y el motivo, y no crea nada
	@Test
	public void unaSolicitudInvalidaVuelveCon400() {
		String nuevo = DatosDePrueba.email();

		FormularioDeRegistro form = solicitud(nuevo);
		form.setRepetir("otra-cosa");

		HttpResponse<String> respuesta = registrar(form);

		assertEquals(HttpStatus.BAD_REQUEST.value(), respuesta.statusCode());
		assertTrue(respuesta.body().contains("Las contraseñas no coinciden"));
		assertEquals(0, contar(nuevo));
	}

	// El email ya tomado tambien lo frena el validador, con el mismo motivo que en la pagina
	@Test
	public void laApiNoDejaRepetirUnEmail() {
		HttpResponse<String> respuesta = registrar(solicitud(EMAIL));

		assertEquals(HttpStatus.BAD_REQUEST.value(), respuesta.statusCode());
		assertTrue(respuesta.body().contains("Ya hay una cuenta con ese email"));
	}

	/** El dato que creo este test, entre todos los que ya tiene la base. */
	private <T> T buscar(List<T> dtos, Predicate<T> comoEs) {
		return dtos.stream().filter(comoEs).findFirst()
				.orElseThrow(() -> new AssertionError("La API no devolvio el dato que creo el test"));
	}

	/** La solicitud de cuenta como la manda otro programa: el mismo formulario que usa la pagina. */
	private FormularioDeRegistro solicitud(String email) {
		FormularioDeRegistro form = new FormularioDeRegistro();
		form.setNombre("Ana");
		form.setApellido("Lopez");
		form.setEmail(email);
		form.setContrasenia(CLAVE);
		form.setRepetir(CLAVE);

		return form;
	}

	// el cuerpo sale del formulario y no de un texto escrito a mano: si un campo cambia de nombre,
	// el JSON del test cambia solo
	private HttpResponse<String> registrar(FormularioDeRegistro form) {
		return post(UsuarioAPI.GUARDAR_USUARIO_URL, comoJson(form));
	}

	private int contar(String email) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM Persona WHERE email = ?", Integer.class, email);
	}

}
