package com.OneDesK.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.DatosDePrueba;
import com.OneDesK.services.AdministradorService;
import com.OneDesK.services.GeneradorDeEventosService;

/**
 * Los circuitos completos de la aplicacion, de punta a punta: vista, controller, services y MySQL.
 * La aplicacion se levanta de verdad en un puerto libre y cada test la usa por HTTP como lo haria un
 * navegador (con su cookie de sesion). Despues de cada accion se revisa la pagina que se ve y lo que
 * quedo guardado en la base.
 *
 * Los datos de prueba se reconocen por el email @web.test y las geneticas que empiezan con "WEB":
 * aca no hay transaccion de test que los deshaga, asi que se borran al terminar cada test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "onedesk.eventos.activo=false")
public class CircuitosWebTest {

	private static final String CLAVE = "12345";

	@LocalServerPort
	private int puerto;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private AdministradorService administradorService;
	@Autowired
	private GeneradorDeEventosService generadorDeEventos;

	private Navegador admin;
	private final List<Integer> indoorsCreados = new ArrayList<>();
	private List<Map<String, Object>> limitesOriginales;

	@BeforeEach
	public void setUp() {
		administradorService.registrar("Ana", "Admin", "admin@web.test", CLAVE);
		admin = ingresar("admin@web.test");
		// los limites de gramos son de la aplicacion real: se guardan para dejarlos como estaban, y se abren
		// para que los demas circuitos no dependan de ellos (los limites tienen sus propios tests)
		limitesOriginales = jdbc.queryForList("SELECT ID, minimoGramos, maximoGramos FROM LimitesDeCompra");
		admin.enviar("/admin/limites", "minimo", "1", "maximo", "1000");
	}

	@AfterEach
	public void borrarDatosDePrueba() {
		jdbc.update("DELETE e FROM Evento e JOIN Planta p ON p.ID = e.ID_PLANTA WHERE p.genetica LIKE 'WEB%'");
		jdbc.update("DELETE r FROM RegistroProduccion r JOIN Producto p ON p.ID = r.ID_PRODUCTO WHERE p.genetica LIKE 'WEB%'");
		jdbc.update("DELETE i FROM ItemCompra i JOIN Compra c ON c.ID = i.ID_COMPRA"
				+ " JOIN Persona p ON p.ID = c.ID_USUARIO WHERE p.email LIKE '%@web.test'");
		jdbc.update("DELETE c FROM Compra c JOIN Persona p ON p.ID = c.ID_USUARIO WHERE p.email LIKE '%@web.test'");
		jdbc.update("DELETE t FROM Trabaja t JOIN Persona p ON p.ID = t.ID_EMPLEADO_INDOOR WHERE p.email LIKE '%@web.test'");
		jdbc.update("DELETE FROM Planta WHERE genetica LIKE 'WEB%'");
		for (Integer indoor : indoorsCreados) {
			jdbc.update("DELETE FROM Indoor WHERE ID = ?", indoor);
		}
		List<Integer> deudas = jdbc.queryForList("SELECT u.ID_DEUDA FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " WHERE p.email LIKE '%@web.test'", Integer.class);
		for (String tabla : List.of("Usuario", "EmpleadoIndoor", "Empleado", "Administrador")) {
			jdbc.update("DELETE t FROM " + tabla + " t JOIN Persona p ON p.ID = t.ID WHERE p.email LIKE '%@web.test'");
		}
		jdbc.update("DELETE FROM Persona WHERE email LIKE '%@web.test'");
		for (Integer deuda : deudas) {
			jdbc.update("DELETE FROM Deuda WHERE ID = ?", deuda);
		}
		jdbc.update("DELETE FROM Producto WHERE genetica LIKE 'WEB%'");
		if (limitesOriginales.isEmpty()) {
			jdbc.update("DELETE FROM LimitesDeCompra");
		}
		for (Map<String, Object> fila : limitesOriginales) {
			jdbc.update("UPDATE LimitesDeCompra SET minimoGramos = ?, maximoGramos = ? WHERE ID = ?",
					fila.get("minimoGramos"), fila.get("maximoGramos"), fila.get("ID"));
		}
	}

	// --- acceso ---

	// Sin sesion, cualquier pagina privada manda al login
	@Test
	public void sinSesionLasPaginasPrivadasMandanAlLogin() {
		Navegador anonimo = new Navegador(puerto);

		for (String ruta : List.of("/catalogo", "/carrito", "/mis-compras", "/empleado", "/admin")) {
			anonimo.abrir(ruta);
			assertEquals("/", anonimo.ruta(), "La ruta " + ruta + " no mando al login");
			assertTrue(anonimo.html().contains("Ingresar"));
		}
	}

	// Registrarse deja una solicitud pendiente: no entra hasta que el admin la aprueba, y despues si
	@Test
	public void registrarseDejaUnaSolicitudQueElAdminAprueba() {
		Navegador nuevo = new Navegador(puerto);
		nuevo.abrir("/registro");

		nuevo.enviar("/registro", "nombre", "Juan", "apellido", "Perez", "email", "Juan@Web.Test",
				"contrasenia", CLAVE, "repetir", CLAVE);

		assertEquals("/", nuevo.ruta());
		assertTrue(nuevo.html().contains("Recibimos tu solicitud"));
		assertTrue(nuevo.html().contains("Te vamos a avisar por mail"));
		int id = idDePersona("juan@web.test");
		assertEquals(1, contar("SELECT COUNT(*) FROM Usuario WHERE ID = " + id + " AND aprobado = 0"));

		// pendiente: el login lo frena con el motivo
		Navegador pendiente = ingresar("juan@web.test");
		assertEquals("/", pendiente.ruta());
		assertTrue(pendiente.html().contains("esperando que la administración la apruebe"));

		// el admin ve la solicitud y la aprueba con un tope
		assertTrue(admin.abrir("/admin").contains("juan@web.test"));
		admin.enviar("/admin/solicitudes/aprobar", "usuarioId", "" + id, "tope", "4000");
		assertTrue(admin.html().contains("Aprobaste a Juan Perez"));
		assertEquals(4000, tope(id));

		assertEquals("/catalogo", ingresar("juan@web.test").ruta());
	}

	// El admin rechaza una solicitud: se borra y esa persona no puede ingresar
	@Test
	public void elAdminRechazaUnaSolicitud() {
		Navegador nuevo = new Navegador(puerto);
		nuevo.abrir("/registro");
		nuevo.enviar("/registro", "nombre", "Juan", "apellido", "Perez", "email", "juan@web.test",
				"contrasenia", CLAVE, "repetir", CLAVE);
		int id = idDePersona("juan@web.test");
		admin.abrir("/admin");

		admin.enviar("/admin/solicitudes/rechazar", "usuarioId", "" + id);

		assertTrue(admin.html().contains("Rechazaste la solicitud"));
		assertEquals(0, contar("SELECT COUNT(*) FROM Persona WHERE ID = " + id));
		assertTrue(ingresar("juan@web.test").html().contains("Email o contraseña incorrectos"));
	}

	// Si las contrasenias no coinciden, vuelve al registro con el error y no guarda nada
	@Test
	public void registroConContraseniasDistintasMuestraElError() {
		Navegador nuevo = new Navegador(puerto);
		nuevo.abrir("/registro");

		nuevo.enviar("/registro", "nombre", "Juan", "apellido", "Perez", "email", "juan@web.test",
				"contrasenia", CLAVE, "repetir", "54321");

		assertEquals("/registro", nuevo.ruta());
		assertTrue(nuevo.html().contains("Las contraseñas no coinciden"));
		assertEquals(0, contar("SELECT COUNT(*) FROM Persona WHERE email = 'juan@web.test'"));
	}

	// Un email ya usado vuelve al registro con el error
	@Test
	public void registroConEmailRepetidoMuestraElError() {
		Navegador nuevo = new Navegador(puerto);
		nuevo.abrir("/registro");

		nuevo.enviar("/registro", "nombre", "Otra", "apellido", "Admin", "email", "admin@web.test",
				"contrasenia", CLAVE, "repetir", CLAVE);

		assertEquals("/registro", nuevo.ruta());
		assertTrue(nuevo.html().contains("Ya existe una persona registrada con el email admin@web.test"));
	}

	// Con la contrasenia equivocada vuelve al login con el error y sin sesion
	@Test
	public void ingresarConContraseniaIncorrectaMuestraElError() {
		Navegador anonimo = new Navegador(puerto);
		anonimo.abrir("/");

		anonimo.enviar("/ingresar", "email", "admin@web.test", "contrasenia", "otra-clave");

		assertEquals("/", anonimo.ruta());
		assertTrue(anonimo.html().contains("Email o contraseña incorrectos"));
		anonimo.abrir("/admin");
		assertEquals("/", anonimo.ruta());
	}

	// Cada rol entra a su propia pagina
	@Test
	public void cadaRolEntraASuPagina() {
		registrarUsuario("cliente@web.test", 0);
		registrarEmpleado("empleado@web.test");

		assertEquals("/admin", admin.ruta());
		assertEquals("/catalogo", ingresar("cliente@web.test").ruta());
		assertEquals("/empleado", ingresar("empleado@web.test").ruta());
	}

	// Nadie entra a la pagina de otro rol escribiendo la direccion: vuelve a la suya
	@Test
	public void nadieEntraALaPaginaDeOtroRol() {
		registrarUsuario("cliente@web.test", 0);
		registrarEmpleado("empleado@web.test");
		Navegador cliente = ingresar("cliente@web.test");
		Navegador empleado = ingresar("empleado@web.test");

		cliente.abrir("/admin");
		assertEquals("/catalogo", cliente.ruta());
		cliente.abrir("/empleado");
		assertEquals("/catalogo", cliente.ruta());
		empleado.abrir("/catalogo");
		assertEquals("/empleado", empleado.ruta());
		admin.abrir("/mis-compras");
		assertEquals("/admin", admin.ruta());
	}

	// Salir cierra la sesion: las paginas privadas vuelven a mandar al login
	@Test
	public void salirCierraLaSesion() {
		admin.enviar("/salir");

		assertEquals("/", admin.ruta());
		admin.abrir("/admin");
		assertEquals("/", admin.ruta());
	}

	// --- circuito de compra ---

	// Circuito completo: el admin carga un producto, el cliente lo ve, lo agrega al carrito y lo compra pagando
	@Test
	public void compraPagadaDePuntaAPunta() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");

		assertTrue(cliente.abrir("/catalogo").contains("WEB Kush"));
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "3");
		assertTrue(cliente.html().contains("Agregaste 3 g de WEB Kush al carrito"));

		String carrito = cliente.abrir("/carrito");
		assertTrue(carrito.contains("WEB Kush"));
		assertTrue(carrito.contains("$3.000"));

		cliente.enviar("/carrito/confirmar", "pago", "ahora");
		assertEquals("/mis-compras", cliente.ruta());
		assertTrue(cliente.html().contains("pendiente hasta que la administración lo apruebe"));
		assertTrue(cliente.html().contains("Esperando aprobación"));
		// el stock ya esta reservado, pero todavia no se cobro
		assertEquals(7, stock(producto));
		int pedido = compraDe("cliente@web.test");
		assertEquals(1, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + pedido + " AND estado = 'PENDIENTE' AND pagado = 0"));
		// el carrito quedo vacio
		assertTrue(cliente.abrir("/carrito").contains("Tu carrito está vacío"));

		// el admin lo ve entre las compras por aprobar y lo aprueba
		assertTrue(admin.abrir("/admin").contains("Compras por aprobar"));
		admin.enviar("/admin/compras/aprobar", "compraId", "" + pedido);
		assertTrue(admin.html().contains("Aprobaste la compra #" + pedido));

		assertEquals(1, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + pedido
				+ " AND estado = 'APROBADA' AND pagado = 1 AND fechaPago = CURDATE() AND precio = 3000"));
		assertTrue(cliente.abrir("/mis-compras").contains("Pagada el"));
	}

	// Comprar en cuenta corriente genera deuda, y pagarla desde Mis compras la salda
	@Test
	public void compraEnCuentaCorrienteYDespuesPagarla() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 5000);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "2");
		cliente.abrir("/carrito");

		cliente.enviar("/carrito/confirmar", "pago", "cuenta");
		int compra = compraDe("cliente@web.test");
		// pendiente todavia no es deuda
		assertEquals(0, deudaDe("cliente@web.test"));
		admin.enviar("/admin/compras/aprobar", "compraId", "" + compra);
		assertEquals(2000, deudaDe("cliente@web.test"));
		assertTrue(cliente.abrir("/mis-compras").contains("Aprobada · impaga"));

		cliente.enviar("/mis-compras/" + compra + "/pagar");

		assertTrue(cliente.html().contains("Pagaste la compra #" + compra));
		assertEquals(0, deudaDe("cliente@web.test"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + compra + " AND pagado = 1"));
	}

	// Anular una compra impaga desde Mis compras la borra y devuelve el stock
	@Test
	public void anularUnaCompraDesdeMisCompras() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 5000);
		Navegador cliente = ingresar("cliente@web.test");
		comprar(cliente, producto, 4, "cuenta");
		int compra = compraDe("cliente@web.test");
		assertEquals(6, stock(producto));

		cliente.enviar("/mis-compras/" + compra + "/anular");

		assertTrue(cliente.html().contains("Anulaste la compra #" + compra));
		assertEquals(10, stock(producto));
		assertEquals(0, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + compra));
	}

	// Un usuario no puede pagar ni anular la compra de otro cambiando el numero en la direccion
	@Test
	public void unUsuarioNoPuedeTocarLaCompraDeOtro() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 5000);
		registrarUsuario("otro@web.test", 0);
		comprar(ingresar("cliente@web.test"), producto, 2, "cuenta");
		int compraAjena = compraDe("cliente@web.test");
		Navegador otro = ingresar("otro@web.test");
		otro.abrir("/mis-compras");

		otro.enviar("/mis-compras/" + compraAjena + "/pagar");
		assertTrue(otro.html().contains("no es tuya"));
		otro.enviar("/mis-compras/" + compraAjena + "/anular");
		assertTrue(otro.html().contains("no es tuya"));

		assertEquals(1, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + compraAjena + " AND pagado = 0"));
		assertEquals(8, stock(producto));
	}

	// Una compra en cuenta corriente que supera el tope muestra el error y el carrito sigue armado
	@Test
	public void compraQueSuperaElTopeMuestraElErrorYConservaElCarrito() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 1000);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "3");
		cliente.abrir("/carrito");

		cliente.enviar("/carrito/confirmar", "pago", "cuenta");

		assertEquals("/carrito", cliente.ruta());
		assertTrue(cliente.html().contains("tope"));
		assertTrue(cliente.html().contains("WEB Kush"));
		assertEquals(10, stock(producto));
	}

	// Pedir mas de lo que hay muestra el error de stock y no descuenta nada
	@Test
	public void comprarMasDelStockMuestraElError() {
		int producto = productoConStock("WEB Kush", 1000, 2);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "5");
		cliente.abrir("/carrito");

		cliente.enviar("/carrito/confirmar", "pago", "ahora");

		assertTrue(cliente.html().contains("Stock insuficiente"));
		assertEquals(2, stock(producto));
	}

	// La busqueda del catalogo filtra por genetica, y un producto sin stock no aparece
	@Test
	public void laBusquedaDelCatalogoFiltraYEsconde() {
		productoConStock("WEB Kush", 1000, 5);
		productoConStock("WEB Haze", 1000, 5);
		productoConStock("WEB Vacia", 1000, 0);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");

		String busqueda = cliente.abrir("/catalogo?busqueda=web+kush");
		assertTrue(busqueda.contains("WEB Kush"));
		assertFalse(busqueda.contains("WEB Haze"));
		assertFalse(cliente.abrir("/catalogo").contains("WEB Vacia"));
	}

	// Quitar un producto del carrito lo saca de la lista
	@Test
	public void quitarDelCarrito() {
		int producto = productoConStock("WEB Kush", 1000, 5);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "1");
		cliente.abrir("/carrito");

		cliente.enviar("/carrito/quitar", "productoId", "" + producto);

		assertTrue(cliente.html().contains("Tu carrito está vacío"));
	}

	// El admin rechaza un pedido: el cliente lo ve rechazado y el stock vuelve al catalogo
	@Test
	public void elAdminRechazaUnPedido() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "4");
		cliente.abrir("/carrito");
		cliente.enviar("/carrito/confirmar", "pago", "ahora");
		int pedido = compraDe("cliente@web.test");
		assertEquals(6, stock(producto));

		admin.abrir("/admin");
		admin.enviar("/admin/compras/rechazar", "compraId", "" + pedido);

		assertTrue(admin.html().contains("Rechazaste la compra #" + pedido));
		assertEquals(10, stock(producto));
		assertEquals(1, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + pedido + " AND estado = 'RECHAZADA'"));
		assertTrue(cliente.abrir("/mis-compras").contains("Rechazada"));
	}

	// El cliente anula su pedido mientras espera aprobacion y el stock vuelve
	@Test
	public void elClienteAnulaUnPedidoPendiente() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "4");
		cliente.abrir("/carrito");
		cliente.enviar("/carrito/confirmar", "pago", "ahora");
		int pedido = compraDe("cliente@web.test");

		cliente.enviar("/mis-compras/" + pedido + "/anular");

		assertTrue(cliente.html().contains("Anulaste la compra #" + pedido));
		assertEquals(10, stock(producto));
		assertEquals(0, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + pedido));
	}

	// Con los limites de 5 g a 40 g, un pedido de 3 g muestra el error y el carrito sigue armado
	@Test
	public void unPedidoFueraDeLosLimitesMuestraElError() {
		admin.enviar("/admin/limites", "minimo", "5", "maximo", "40");
		int producto = productoConStock("WEB Kush", 1000, 50);
		registrarUsuario("cliente@web.test", 0);
		Navegador cliente = ingresar("cliente@web.test");
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "3");
		assertTrue(cliente.abrir("/carrito").contains("3 g · cada compra va de 5 g a 40 g"));

		cliente.enviar("/carrito/confirmar", "pago", "ahora");

		assertEquals("/carrito", cliente.ruta());
		assertTrue(cliente.html().contains("entre 5 g y 40 g"));
		assertTrue(cliente.html().contains("WEB Kush"));
		assertEquals(50, stock(producto));
	}

	// El admin cambia los limites desde su panel y quedan guardados
	@Test
	public void elAdminCambiaLosLimites() {
		admin.abrir("/admin");

		admin.enviar("/admin/limites", "minimo", "10", "maximo", "25");

		assertTrue(admin.html().contains("Ahora cada compra va de 10 g a 25 g"));
		assertEquals(1, contar("SELECT COUNT(*) FROM LimitesDeCompra WHERE minimoGramos = 10 AND maximoGramos = 25"));
	}

	// Un maximo menor al minimo muestra el error y no cambia los limites
	@Test
	public void limitesInvalidosMuestranElError() {
		admin.enviar("/admin/limites", "minimo", "5", "maximo", "40");

		admin.enviar("/admin/limites", "minimo", "30", "maximo", "10");

		assertTrue(admin.html().contains("El máximo no puede ser menor que el mínimo"));
		assertEquals(1, contar("SELECT COUNT(*) FROM LimitesDeCompra WHERE minimoGramos = 5 AND maximoGramos = 40"));
	}

	// --- circuito del empleado ---

	// El empleado planta en su indoor, atiende el evento que genera la planta y cosecha: todo queda en la base
	@Test
	public void plantarAtenderYCosecharDePuntaAPunta() {
		int indoor = crearIndoor();
		int empleadoId = registrarEmpleado("empleado@web.test");
		admin.enviar("/admin/empleados/asignar", "empleadoId", "" + empleadoId, "indoorId", "" + indoor);
		int producto = productoConStock("WEB Kush", 1000, 0);
		Navegador empleado = ingresar("empleado@web.test");

		empleado.enviar("/empleado/plantar", "indoorId", "" + indoor, "genetica", "WEB Kush",
				"fechaGerminado", hoyMenos(20), "fechaPlantado", hoyMenos(10),
				"tiempoRegado", "600", "tiempoLuz", "600", "tiempoVentilacion", "30");
		assertTrue(empleado.html().contains("Plantaste WEB Kush"));
		int planta = jdbc.queryForObject("SELECT ID FROM Planta WHERE ID_INDOOR = ? AND genetica = 'WEB Kush'",
				Integer.class, indoor);

		// pasaron 31 minutos desde que se planto: la revision crea la ventilacion
		jdbc.update("UPDATE Planta SET ultimoVentilacion = NOW() - INTERVAL 31 MINUTE WHERE ID = ?", planta);
		generadorDeEventos.generarEventos();
		String panel = empleado.abrir("/empleado");
		assertTrue(panel.contains("Ventilación"));
		int evento = jdbc.queryForObject("SELECT ID FROM Evento WHERE ID_PLANTA = ?", Integer.class, planta);

		empleado.enviar("/empleado/atender", "indoorId", "" + indoor, "eventoId", "" + evento);
		assertTrue(empleado.html().contains("Evento atendido"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Evento WHERE ID = " + evento + " AND realizado = 1"));

		empleado.enviar("/empleado/cosechar", "plantaId", "" + planta, "cantidad", "40");
		assertTrue(empleado.html().contains("Cosecha registrada"));
		assertEquals(40, stock(producto));
		assertEquals(1, contar("SELECT COUNT(*) FROM RegistroProduccion WHERE ID_PLANTA = " + planta
				+ " AND ID_EMPLEADO_INDOOR = " + empleadoId + " AND cantidad = 40"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Planta WHERE ID = " + planta + " AND fechaCosecha IS NOT NULL"));
	}

	// Un empleado no puede plantar en un indoor que no tiene a cargo
	@Test
	public void unEmpleadoNoPlantaEnUnIndoorAjeno() {
		int indoor = crearIndoor();
		registrarEmpleado("empleado@web.test");
		Navegador empleado = ingresar("empleado@web.test");

		empleado.enviar("/empleado/plantar", "indoorId", "" + indoor, "genetica", "WEB Kush",
				"fechaGerminado", hoyMenos(20), "fechaPlantado", hoyMenos(10),
				"tiempoRegado", "600", "tiempoLuz", "600", "tiempoVentilacion", "30");

		assertTrue(empleado.html().contains("No estas asignado al indoor " + indoor));
		assertEquals(0, contar("SELECT COUNT(*) FROM Planta WHERE ID_INDOOR = " + indoor));
	}

	// Cosechar una genetica que no esta en el catalogo muestra el error y la planta sigue en cultivo
	@Test
	public void cosecharSinProductoMuestraElError() {
		int indoor = crearIndoor();
		int empleadoId = registrarEmpleado("empleado@web.test");
		admin.enviar("/admin/empleados/asignar", "empleadoId", "" + empleadoId, "indoorId", "" + indoor);
		Navegador empleado = ingresar("empleado@web.test");
		empleado.enviar("/empleado/plantar", "indoorId", "" + indoor, "genetica", "WEB Sin Producto",
				"fechaGerminado", hoyMenos(20), "fechaPlantado", hoyMenos(10),
				"tiempoRegado", "600", "tiempoLuz", "600", "tiempoVentilacion", "300");
		int planta = jdbc.queryForObject("SELECT ID FROM Planta WHERE ID_INDOOR = ?", Integer.class, indoor);

		empleado.enviar("/empleado/cosechar", "plantaId", "" + planta, "cantidad", "40");

		assertEquals("/empleado", empleado.ruta());
		assertTrue(empleado.html().contains("aviso-accion--error"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Planta WHERE ID = " + planta + " AND fechaCosecha IS NULL"));
	}

	// Un campo numerico vacio vuelve a la pagina con un aviso, sin pagina de error
	@Test
	public void unCampoVacioMuestraUnAviso() {
		registrarEmpleado("empleado@web.test");
		Navegador empleado = ingresar("empleado@web.test");

		empleado.enviar("/empleado/productos", "genetica", "WEB Nueva", "precio", "");

		assertEquals("/empleado", empleado.ruta());
		assertTrue(empleado.html().contains("Completá todos los campos"));
	}

	// --- circuito del administrador ---

	// El admin aprueba un usuario con tope, despues se lo cambia, y el usuario lo ve en Mis compras
	@Test
	public void elAdminApruebaUnUsuarioYDespuesLeCambiaElTope() {
		int usuario = registrarUsuario("cliente@web.test", 3000);
		assertEquals(3000, tope(usuario));

		admin.enviar("/admin/usuarios/tope", "usuarioId", "" + usuario, "tope", "8000");

		assertTrue(admin.html().contains("Tope actualizado"));
		assertEquals(8000, tope(usuario));
		assertTrue(ingresar("cliente@web.test").abrir("/mis-compras").contains("$8.000"));
	}

	// El admin cambia el precio, fija y ajusta el stock, y el catalogo del cliente lo refleja
	@Test
	public void elAdminManejaPrecioYStock() {
		int producto = productoConStock("WEB Kush", 1000, 10);

		admin.enviar("/admin/productos/precio", "productoId", "" + producto, "precio", "1500");
		admin.enviar("/admin/productos/ajustar", "productoId", "" + producto, "cantidad", "-4");

		assertEquals(6, stock(producto));
		assertEquals(1500, (int) jdbc.queryForObject("SELECT precio FROM Producto WHERE ID = ?", Integer.class, producto));
		registrarUsuario("cliente@web.test", 0);
		String catalogo = ingresar("cliente@web.test").abrir("/catalogo");
		assertTrue(catalogo.contains("$1.500"));
		assertTrue(catalogo.contains("6 g en stock"));
	}

	// Un ajuste que dejaria el stock negativo muestra el error y no cambia nada
	@Test
	public void unAjusteInvalidoMuestraElError() {
		int producto = productoConStock("WEB Kush", 1000, 3);

		admin.enviar("/admin/productos/ajustar", "productoId", "" + producto, "cantidad", "-5");

		assertTrue(admin.html().contains("Stock insuficiente"));
		assertEquals(3, stock(producto));
	}

	// El admin crea un indoor, da de alta un empleado, se lo asigna, le cambia el salario y lo desasigna
	@Test
	public void elAdminManejaIndoorsYEmpleados() {
		int indoor = crearIndoor();
		int empleado = registrarEmpleado("empleado@web.test");

		admin.enviar("/admin/empleados/asignar", "empleadoId", "" + empleado, "indoorId", "" + indoor);
		assertEquals(1, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado
				+ " AND ID_INDOOR = " + indoor));
		admin.enviar("/admin/empleados/salario", "empleadoId", "" + empleado, "salario", "650000");
		assertEquals(650000, (int) jdbc.queryForObject("SELECT salarioMensual FROM EmpleadoIndoor WHERE ID = ?",
				Integer.class, empleado));

		admin.enviar("/admin/empleados/desasignar", "empleadoId", "" + empleado, "indoorId", "" + indoor);
		assertEquals(0, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado));
	}

	// El admin planta en cualquier indoor, aunque no tenga empleados
	@Test
	public void elAdminPlantaEnCualquierIndoor() {
		int indoor = crearIndoor();

		admin.enviar("/admin/plantar", "indoorId", "" + indoor, "genetica", "WEB Kush",
				"fechaGerminado", hoyMenos(20), "fechaPlantado", hoyMenos(10),
				"tiempoRegado", "600", "tiempoLuz", "600", "tiempoVentilacion", "300");

		assertTrue(admin.html().contains("Plantaste WEB Kush"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Planta WHERE ID_INDOOR = " + indoor));
	}

	// El admin anula la compra impaga de un cliente y los reportes muestran pagos y deudores
	@Test
	public void elAdminAnulaUnaCompraYVeLosReportes() {
		int producto = productoConStock("WEB Kush", 1000, 10);
		registrarUsuario("cliente@web.test", 5000);
		Navegador cliente = ingresar("cliente@web.test");
		comprar(cliente, producto, 2, "ahora");
		comprar(cliente, producto, 3, "cuenta");

		String panel = admin.abrir("/admin");
		assertTrue(panel.contains("Registros de pago"));
		assertTrue(panel.contains("$3.000"));
		int impaga = jdbc.queryForObject("SELECT c.ID FROM Compra c JOIN Persona p ON p.ID = c.ID_USUARIO"
				+ " WHERE p.email = 'cliente@web.test' AND c.pagado = 0", Integer.class);

		admin.enviar("/admin/compras/anular", "compraId", "" + impaga);

		assertTrue(admin.html().contains("Anulaste la compra #" + impaga));
		assertEquals(0, deudaDe("cliente@web.test"));
		assertEquals(8, stock(producto));
	}

	// Un email repetido al dar de alta un empleado muestra el error en el panel
	@Test
	public void elAdminVeElErrorDeUnEmailRepetido() {
		registrarUsuario("cliente@web.test", 0);

		admin.enviar("/admin/empleados", "nombre", "Pedro", "apellido", "Gomez", "email", "cliente@web.test",
				"contrasenia", CLAVE, "salario", "500000");

		assertEquals("/admin", admin.ruta());
		assertTrue(admin.html().contains("Ya existe una persona registrada con el email cliente@web.test"));
	}

	// --- indoors con nombre y capacidad ---

	// El admin crea un indoor desde la ventana: queda con su nombre y su capacidad, y se ve en el panel
	@Test
	public void elAdminCreaUnIndoorConNombreYCapacidad() {
		int indoor = crearIndoor("WEB Carpa grande", 12);

		assertEquals(1, contar("SELECT COUNT(*) FROM Indoor WHERE ID = " + indoor
				+ " AND nombre = 'WEB Carpa grande' AND capacidad = 12"));
		assertTrue(admin.abrir("/admin").contains("WEB Carpa grande"));
		assertTrue(admin.html().contains("0 de 12"));
	}

	// Un nombre repetido, aunque cambien las mayusculas, muestra el error y no crea otro indoor
	@Test
	public void unNombreDeIndoorRepetidoMuestraElError() {
		crearIndoor("WEB Carpa grande", 12);

		admin.enviar("/admin/indoors", "nombre", "web carpa GRANDE", "capacidad", "5");

		assertTrue(admin.html().contains("Ya existe un indoor llamado web carpa GRANDE"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Indoor WHERE nombre LIKE 'WEB Carpa%'"));
	}

	// Plantar en un indoor lleno muestra el error y no agrega la planta
	@Test
	public void plantarEnUnIndoorLlenoMuestraElError() {
		int indoor = crearIndoor("WEB Chiquito", 1);
		admin.enviar("/admin/plantar", "indoorId", "" + indoor, "genetica", "WEB Kush",
				"fechaGerminado", hoyMenos(20), "fechaPlantado", hoyMenos(10),
				"tiempoRegado", "600", "tiempoLuz", "600", "tiempoVentilacion", "300");

		admin.enviar("/admin/plantar", "indoorId", "" + indoor, "genetica", "WEB Haze",
				"fechaGerminado", hoyMenos(20), "fechaPlantado", hoyMenos(10),
				"tiempoRegado", "600", "tiempoLuz", "600", "tiempoVentilacion", "300");

		assertTrue(admin.html().contains("El indoor WEB Chiquito está lleno"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Planta WHERE ID_INDOOR = " + indoor));
	}

	// --- helpers ---

	private Navegador ingresar(String email) {
		Navegador navegador = new Navegador(puerto);
		navegador.abrir("/");
		navegador.enviar("/ingresar", "email", email, "contrasenia", CLAVE);
		return navegador;
	}

	// como en la aplicacion: la persona pide la cuenta y el admin la aprueba con su tope
	private int registrarUsuario(String email, int tope) {
		Navegador nuevo = new Navegador(puerto);
		nuevo.abrir("/registro");
		nuevo.enviar("/registro", "nombre", "Cliente", "apellido", "Web", "email", email,
				"contrasenia", CLAVE, "repetir", CLAVE);
		int id = idDePersona(email);
		admin.abrir("/admin");
		admin.enviar("/admin/solicitudes/aprobar", "usuarioId", "" + id, "tope", "" + tope);
		return id;
	}

	private int registrarEmpleado(String email) {
		admin.abrir("/admin");
		admin.enviar("/admin/empleados", "nombre", "Empleado", "apellido", "Web", "email", email,
				"contrasenia", CLAVE, "salario", "500000");
		return idDePersona(email);
	}

	// como en el panel: el admin completa la ventana de crear indoor con un nombre y la capacidad
	private int crearIndoor() {
		return crearIndoor(DatosDePrueba.nombreDeIndoor(), 20);
	}

	private int crearIndoor(String nombre, int capacidad) {
		admin.abrir("/admin");
		admin.enviar("/admin/indoors", "nombre", nombre, "capacidad", "" + capacidad);
		assertTrue(admin.html().contains("Creaste el indoor " + nombre), "No aparecio el aviso del indoor creado");
		int id = jdbc.queryForObject("SELECT ID FROM Indoor WHERE nombre = ?", Integer.class, nombre);
		indoorsCreados.add(id);
		return id;
	}

	private int productoConStock(String genetica, int precio, int stock) {
		admin.abrir("/admin");
		admin.enviar("/admin/productos", "genetica", genetica, "precio", "" + precio);
		int id = jdbc.queryForObject("SELECT ID FROM Producto WHERE genetica = ?", Integer.class, genetica);
		if (stock > 0) {
			admin.enviar("/admin/productos/fijar", "productoId", "" + id, "stock", "" + stock);
		}
		return id;
	}

	// el cliente pide y el admin aprueba el pedido
	private void comprar(Navegador cliente, int producto, int cantidad, String pago) {
		cliente.abrir("/catalogo");
		cliente.enviar("/carrito/agregar", "productoId", "" + producto, "cantidad", "" + cantidad);
		cliente.abrir("/carrito");
		cliente.enviar("/carrito/confirmar", "pago", pago);
		int pedido = jdbc.queryForObject("SELECT MAX(ID) FROM Compra", Integer.class);
		admin.abrir("/admin");
		admin.enviar("/admin/compras/aprobar", "compraId", "" + pedido);
	}

	private int idDePersona(String email) {
		Integer id = jdbc.queryForObject("SELECT ID FROM Persona WHERE email = ?", Integer.class, email);
		assertNotNull(id);
		return id;
	}

	private int compraDe(String email) {
		return jdbc.queryForObject("SELECT MAX(c.ID) FROM Compra c JOIN Persona p ON p.ID = c.ID_USUARIO"
				+ " WHERE p.email = ?", Integer.class, email);
	}

	private int stock(int producto) {
		return jdbc.queryForObject("SELECT stock FROM Producto WHERE ID = ?", Integer.class, producto);
	}

	private int tope(int usuario) {
		return jdbc.queryForObject("SELECT topeCredito FROM Usuario WHERE ID = ?", Integer.class, usuario);
	}

	private int deudaDe(String email) {
		return jdbc.queryForObject("SELECT d.monto FROM Deuda d JOIN Usuario u ON u.ID_DEUDA = d.ID"
				+ " JOIN Persona p ON p.ID = u.ID WHERE p.email = ?", Integer.class, email);
	}

	private int contar(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	private String hoyMenos(int dias) {
		return java.time.LocalDate.now().minusDays(dias).toString();
	}
}
