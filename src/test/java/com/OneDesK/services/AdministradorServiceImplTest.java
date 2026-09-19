package com.OneDesK.services;

import com.OneDesK.DatosDePrueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.evento.Evento;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.excepciones.StockInsuficienteException;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.RegistroProduccion;
import com.OneDesK.modelo.Usuario;

// Acciones del administrador contra MySQL con los services reales.
// Cada resultado se verifica recargando desde la base (flush + clear). Los reportes se filtran
// por los datos que crea cada test, porque la base puede tener otros cargados.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ AdministradorServiceImpl.class, IndoorServiceImpl.class, EmpleadoIndoorServiceImpl.class,
		ProductoServiceImpl.class, UsuarioServiceImpl.class, CompraServiceImpl.class,
		RegistroProduccionServiceImpl.class, LimitesDeCompraServiceImpl.class })
public class AdministradorServiceImplTest {

	private static final int INEXISTENTE = 999999;

	@Autowired
	private AdministradorService service;
	@Autowired
	private CompraService compraService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private RegistroProduccionService registroProduccionService;
	@Autowired
	private TestEntityManager em;

	private int admin;

	@BeforeEach
	public void setUp() {
		admin = service.registrar("Ana", "Admin", "admin@test.com", "12345").getId();
		service.cambiarLimites(admin, 1, 1000);
		recargar();
	}

	// --- el administrador ---

	// Registrar un administrador lo guarda en Persona y en Administrador
	@Test
	public void registrarGuardaAlAdministradorEnSusDosTablas() {
		assertEquals(1, contar("SELECT COUNT(*) FROM Persona WHERE ID = " + admin + " AND email = 'admin@test.com'"));
		assertEquals(1, contar("SELECT COUNT(*) FROM Administrador WHERE ID = " + admin));
	}

	// No se puede registrar un administrador con el email de otra persona, aunque cambien las mayusculas
	@Test
	public void noSePuedeRegistrarUnAdministradorConUnEmailUsado() {
		assertThrows(EmailDuplicadoException.class, () -> service.registrar("Otro", "Admin", "ADMIN@test.com", "12345"));
	}

	// --- permisos ---

	// Un usuario no puede usar las acciones del administrador y no se crea nada
	@Test
	public void unUsuarioNoPuedeActuarComoAdministrador() {
		int usuario = nuevoUsuario(0);
		recargar();
		long indoorsAntes = contar("SELECT COUNT(*) FROM Indoor");

		assertThrows(RecursoNoEncontradoException.class, () -> service.crearIndoor(usuario, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD));
		recargar();

		assertEquals(indoorsAntes, contar("SELECT COUNT(*) FROM Indoor"));
	}

	// Un empleado tampoco puede usar las acciones del administrador
	@Test
	public void unEmpleadoNoPuedeActuarComoAdministrador() {
		int empleado = service.registrarEmpleado(admin, "Pedro", "Gomez", "pedro@test.com", "12345", 500000).getId();
		recargar();

		assertThrows(RecursoNoEncontradoException.class, () -> service.crearProducto(empleado, "Nueva", 1000));
		recargar();

		assertEquals(0, contar("SELECT COUNT(*) FROM Producto WHERE genetica = 'Nueva'"));
	}

	// Un id que no existe tampoco es administrador
	@Test
	public void unIdInexistenteNoEsAdministrador() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.deudores(INEXISTENTE));
	}

	// --- indoors y empleados ---

	// Crear un indoor lo deja guardado en la base
	@Test
	public void crearIndoorQuedaGuardado() {
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		recargar();

		assertNotNull(em.find(Indoor.class, indoor));
	}

	// Registrar un empleado lo guarda con su salario
	@Test
	public void registrarEmpleadoQuedaGuardadoConSuSalario() {
		int empleado = service.registrarEmpleado(admin, "Pedro", "Gomez", "pedro@test.com", "12345", 500000).getId();
		recargar();

		EmpleadoIndoor recargado = em.find(EmpleadoIndoor.class, empleado);
		assertEquals("pedro@test.com", recargado.getEmail());
		assertEquals(500000, recargado.getSalarioMensual());
	}

	// Asignar un empleado a un indoor queda en la tabla Trabaja, y desasignarlo lo saca
	@Test
	public void asignarYDesasignarUnEmpleadoQuedaGuardado() {
		int empleado = nuevoEmpleado();
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		recargar();

		service.asignarEmpleado(admin, empleado, indoor);
		recargar();
		assertEquals(1, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado
				+ " AND ID_INDOOR = " + indoor));

		service.desasignarEmpleado(admin, empleado, indoor);
		recargar();
		assertEquals(0, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado));
	}

	// Cambiar el salario queda guardado; un salario de 0 se rechaza y queda el anterior
	@Test
	public void cambiarSalarioQuedaGuardadoYSeValida() {
		int empleado = nuevoEmpleado();

		service.cambiarSalario(admin, empleado, 650000);
		recargar();
		assertEquals(650000, em.find(EmpleadoIndoor.class, empleado).getSalarioMensual());

		assertThrows(IllegalArgumentException.class, () -> service.cambiarSalario(admin, empleado, 0));
		recargar();
		assertEquals(650000, em.find(EmpleadoIndoor.class, empleado).getSalarioMensual());
	}

	// --- productos ---

	// Un producto nuevo queda guardado con su precio y sin stock
	@Test
	public void crearProductoQuedaGuardadoSinStock() {
		int producto = service.crearProducto(admin, "Northern Lights", 1200).getId();
		recargar();

		Producto recargado = em.find(Producto.class, producto);
		assertEquals(1200, recargado.getPrecio());
		assertEquals(0, recargado.getStock());
	}

	// No se puede crear un producto con una genetica que ya existe
	@Test
	public void noSePuedeCrearUnProductoRepetido() {
		service.crearProducto(admin, "Northern Lights", 1200);
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.crearProducto(admin, "northern lights", 900));
	}

	// Cambiar el precio queda guardado
	@Test
	public void cambiarPrecioQuedaGuardado() {
		int producto = service.crearProducto(admin, "Northern Lights", 1200).getId();

		service.cambiarPrecio(admin, producto, 1500);
		recargar();

		assertEquals(1500, em.find(Producto.class, producto).getPrecio());
	}

	// Fijar el stock lo deja en el valor contado; un valor negativo se rechaza y queda el anterior
	@Test
	public void fijarStockQuedaGuardadoYSeValida() {
		int producto = service.crearProducto(admin, "Northern Lights", 1200).getId();

		service.fijarStock(admin, producto, 30);
		recargar();
		assertEquals(30, em.find(Producto.class, producto).getStock());

		assertThrows(IllegalArgumentException.class, () -> service.fijarStock(admin, producto, -1));
		recargar();
		assertEquals(30, em.find(Producto.class, producto).getStock());
	}

	// Ajustar el stock suma o resta; un ajuste que lo dejaria negativo se rechaza y queda el anterior
	@Test
	public void ajustarStockQuedaGuardadoYSeValida() {
		int producto = service.crearProducto(admin, "Northern Lights", 1200).getId();
		service.fijarStock(admin, producto, 10);

		service.ajustarStock(admin, producto, 5);
		service.ajustarStock(admin, producto, -3);
		recargar();
		assertEquals(12, em.find(Producto.class, producto).getStock());

		assertThrows(StockInsuficienteException.class, () -> service.ajustarStock(admin, producto, -13));
		recargar();
		assertEquals(12, em.find(Producto.class, producto).getStock());
	}

	// Modificar un producto que no existe falla con RecursoNoEncontradoException
	@Test
	public void modificarUnProductoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.fijarStock(admin, INEXISTENTE, 5));
	}

	// --- usuarios ---

	// --- solicitudes de registro ---

	// Quien se registra queda pendiente: aparece en las solicitudes y no entre los usuarios aprobados
	@Test
	public void unRegistroQuedaComoSolicitudPendiente() {
		int pendiente = solicitud("juan@test.com");

		assertTrue(idsDe(service.solicitudesPendientes(admin)).contains(pendiente));
		assertFalse(idsDe(service.usuarios(admin)).contains(pendiente));
		assertEquals(1, contar("SELECT COUNT(*) FROM Usuario WHERE ID = " + pendiente + " AND aprobado = 0"));
	}

	// Aprobar la solicitud la habilita con el tope que pone el administrador, y pasa a los usuarios
	@Test
	public void aprobarLaSolicitudLaHabilitaConSuTope() {
		int usuario = solicitud("juan@test.com");

		service.aprobarUsuario(admin, usuario, 8000);
		recargar();

		Usuario recargado = em.find(Usuario.class, usuario);
		assertTrue(recargado.isAprobado());
		assertEquals(8000, recargado.getTopeCredito());
		assertFalse(idsDe(service.solicitudesPendientes(admin)).contains(usuario));
		assertTrue(idsDe(service.usuarios(admin)).contains(usuario));
	}

	// Aprobar con un tope negativo se rechaza y la solicitud sigue pendiente
	@Test
	public void aprobarConTopeNegativoLaDejaPendiente() {
		int usuario = solicitud("juan@test.com");

		assertThrows(IllegalArgumentException.class, () -> service.aprobarUsuario(admin, usuario, -1));
		recargar();

		assertFalse(em.find(Usuario.class, usuario).isAprobado());
	}

	// Un usuario ya aprobado no se puede aprobar de nuevo ni rechazar
	@Test
	public void unUsuarioAprobadoNoSeApruebaNiRechazaDeNuevo() {
		int usuario = nuevoUsuario(5000);

		assertThrows(OperacionInvalidaException.class, () -> service.aprobarUsuario(admin, usuario, 100));
		assertThrows(OperacionInvalidaException.class, () -> service.rechazarUsuario(admin, usuario));
		recargar();

		assertEquals(5000, em.find(Usuario.class, usuario).getTopeCredito());
	}

	// Rechazar borra la solicitud con su deuda, y ese email puede volver a pedir cuenta
	@Test
	public void rechazarBorraLaSolicitudYLiberaElEmail() {
		int usuario = solicitud("juan@test.com");
		int deuda = em.find(Usuario.class, usuario).getDeuda().getId();

		service.rechazarUsuario(admin, usuario);
		recargar();

		assertEquals(0, contar("SELECT COUNT(*) FROM Persona WHERE ID = " + usuario));
		assertEquals(0, contar("SELECT COUNT(*) FROM Deuda WHERE ID = " + deuda));
		solicitud("juan@test.com");
	}

	// Aprobar o rechazar una solicitud que no existe falla con RecursoNoEncontradoException
	@Test
	public void aprobarORechazarUnaSolicitudInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.aprobarUsuario(admin, INEXISTENTE, 0));
		assertThrows(RecursoNoEncontradoException.class, () -> service.rechazarUsuario(admin, INEXISTENTE));
	}

	// Asignar el tope a un usuario existente queda guardado
	@Test
	public void asignarTopeQuedaGuardado() {
		int usuario = nuevoUsuario(0);

		service.asignarTope(admin, usuario, 5000);
		recargar();

		assertEquals(5000, em.find(Usuario.class, usuario).getTopeCredito());
	}

	// --- compras ---

	// El administrador anula la compra impaga de un usuario: se borra, vuelve el stock y la deuda queda en 0
	@Test
	public void anularLaCompraImpagaDeUnUsuario() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int compra = comprar(usuario, producto, 3, false);

		service.anularCompra(admin, compra);
		recargar();

		assertNull(em.find(Compra.class, compra));
		assertEquals(10, em.find(Producto.class, producto).getStock());
		assertEquals(0, em.find(Usuario.class, usuario).getDeuda().getMonto());
	}

	// Una compra pagada no se puede anular: sigue guardada y el stock no vuelve
	@Test
	public void noSePuedeAnularUnaCompraPagada() {
		int usuario = nuevoUsuario(0);
		int producto = productoConStock("OG Kush", 10);
		int compra = comprar(usuario, producto, 3, true);

		assertThrows(OperacionInvalidaException.class, () -> service.anularCompra(admin, compra));
		recargar();

		assertNotNull(em.find(Compra.class, compra));
		assertEquals(7, em.find(Producto.class, producto).getStock());
	}

	// --- reportes ---

	// Los registros de produccion incluyen las cosechas, de la mas reciente a la mas vieja
	@Test
	public void registrosDeProduccionDeLaMasRecienteALaMasVieja() {
		int empleado = nuevoEmpleado();
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		service.asignarEmpleado(admin, empleado, indoor);
		service.crearProducto(admin, "OG Kush", 1000);
		int primera = plantar(indoor);
		int segunda = plantar(indoor);
		int registroPrimera = registroProduccionService.registrarCosecha(empleado, indoor, primera, 40).getId();
		int registroSegunda = registroProduccionService.registrarCosecha(empleado, indoor, segunda, 25).getId();
		recargar();

		List<Integer> ids = new ArrayList<>();
		for (RegistroProduccion registro : service.registrosDeProduccion(admin)) {
			ids.add(registro.getId());
		}

		assertTrue(ids.indexOf(registroSegunda) >= 0);
		assertTrue(ids.indexOf(registroSegunda) < ids.indexOf(registroPrimera));
	}

	// Los registros de pago tienen las compras pagadas con su fecha de pago, y no las impagas
	@Test
	public void registrosDePagoSoloIncluyenLasComprasPagadas() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int pagada = comprar(usuario, producto, 1, true);
		int impaga = comprar(usuario, producto, 1, false);

		List<Integer> pagos = idsDePagos();
		assertTrue(pagos.contains(pagada));
		assertFalse(pagos.contains(impaga));
		assertEquals(LocalDate.now(), em.find(Compra.class, pagada).getFechaPago());

		// al pagar la impaga pasa a estar en los registros, con la fecha de hoy
		compraService.registrarPago(impaga);
		recargar();
		assertTrue(idsDePagos().contains(impaga));
		assertEquals(LocalDate.now(), em.find(Compra.class, impaga).getFechaPago());
	}

	// Los deudores son los usuarios con deuda, del que mas debe al que menos; quien no debe no aparece
	@Test
	public void deudoresDelQueMasDebeAlQueMenos() {
		int producto = productoConStock("OG Kush", 20);
		int debePoco = nuevoUsuario(10000, "poco@test.com");
		int debeMucho = nuevoUsuario(10000, "mucho@test.com");
		int noDebe = nuevoUsuario(10000, "nada@test.com");
		comprar(debePoco, producto, 1, false);
		comprar(debeMucho, producto, 5, false);
		comprar(noDebe, producto, 1, true);

		List<Integer> ids = new ArrayList<>();
		for (Usuario usuario : service.deudores(admin)) {
			ids.add(usuario.getId());
		}

		assertTrue(ids.contains(debePoco));
		assertTrue(ids.indexOf(debeMucho) < ids.indexOf(debePoco));
		assertFalse(ids.contains(noDebe));
	}

	// Los eventos pendientes son los de todos los indoors, aunque no tengan empleado asignado; los atendidos no
	@Test
	public void eventosPendientesDeTodosLosIndoors() {
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		int planta = plantar(indoor);
		Indoor recargado = em.find(Indoor.class, indoor);
		EventoRegado pendiente = new EventoRegado(recargado.buscarPlanta(planta));
		EventoRegado atendido = new EventoRegado(recargado.buscarPlanta(planta));
		recargado.recibirEvento(pendiente);
		recargado.recibirEvento(atendido);
		atendido.setRealizado(true);
		recargar();

		List<Integer> ids = new ArrayList<>();
		for (Evento evento : service.eventosPendientes(admin)) {
			ids.add(evento.getId());
		}

		assertTrue(ids.contains(pendiente.getId()));
		assertFalse(ids.contains(atendido.getId()));
	}

	// --- el administrador: datos invalidos ---

	// Un administrador con email invalido o contrasenia corta se rechaza y no queda guardado
	@Test
	public void unAdministradorConDatosInvalidosNoSeGuarda() {
		long antes = contar("SELECT COUNT(*) FROM Administrador");

		assertThrows(IllegalArgumentException.class, () -> service.registrar("Otro", "Admin", "sin-arroba", "12345"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("Otro", "Admin", "otro@test.com", "123"));
		recargar();

		assertEquals(antes, contar("SELECT COUNT(*) FROM Administrador"));
	}

	// --- permisos: todas las acciones ---

	// Un usuario no puede usar ninguna de las acciones del administrador, una por una, y no cambia nada
	@Test
	public void unUsuarioNoPuedeUsarNingunaAccionDeAdministrador() {
		int usuario = nuevoUsuario(0);
		int producto = productoConStock("OG Kush", 10);

		for (Executable accion : todasLasAcciones(usuario, producto)) {
			assertThrows(RecursoNoEncontradoException.class, accion);
		}
		recargar();

		assertEquals(10, em.find(Producto.class, producto).getStock());
		assertEquals(1000, em.find(Producto.class, producto).getPrecio());
	}

	// Un empleado tampoco puede usar ninguna de las acciones del administrador
	@Test
	public void unEmpleadoNoPuedeUsarNingunaAccionDeAdministrador() {
		int empleado = nuevoEmpleado();
		int producto = productoConStock("OG Kush", 10);

		for (Executable accion : todasLasAcciones(empleado, producto)) {
			assertThrows(RecursoNoEncontradoException.class, accion);
		}
	}

	// Al reves tampoco: un administrador no es un usuario, asi que no puede comprar
	@Test
	public void unAdministradorNoPuedeComprarComoUsuario() {
		int producto = productoConStock("OG Kush", 10);

		assertThrows(RecursoNoEncontradoException.class,
				() -> compraService.realizarCompra(admin, List.of(new LineaCompra(producto, 1)), true));
		recargar();

		assertEquals(10, em.find(Producto.class, producto).getStock());
	}

	// --- altas: validaciones ---

	// Un empleado con el email de otra persona se rechaza y queda una sola persona con ese email
	@Test
	public void noSePuedeRegistrarUnEmpleadoConUnEmailUsado() {
		nuevoUsuario(0);

		assertThrows(EmailDuplicadoException.class,
				() -> service.registrarEmpleado(admin, "Pedro", "Gomez", "JUAN@test.com", "12345", 500000));
		recargar();

		assertEquals(1, contar("SELECT COUNT(*) FROM Persona WHERE email = 'juan@test.com'"));
	}

	// Un usuario con el email del administrador se rechaza
	@Test
	public void noSePuedeRegistrarUnUsuarioConElEmailDeUnAdministrador() {
		assertThrows(EmailDuplicadoException.class,
				() -> usuarioService.registrar("Juan", "Perez", "admin@test.com", "12345"));
	}

	// Un empleado con salario 0 no se registra
	@Test
	public void unEmpleadoSinSalarioNoSeRegistra() {
		assertThrows(IllegalArgumentException.class,
				() -> service.registrarEmpleado(admin, "Pedro", "Gomez", "pedro@test.com", "12345", 0));
		recargar();

		assertEquals(0, contar("SELECT COUNT(*) FROM Persona WHERE email = 'pedro@test.com'"));
	}

	// Un usuario o un empleado con email invalido no se registran
	@Test
	public void conEmailInvalidoNoSeRegistraNadie() {
		long antes = contar("SELECT COUNT(*) FROM Persona");

		assertThrows(IllegalArgumentException.class,
				() -> usuarioService.registrar("Juan", "Perez", "@", "12345"));
		assertThrows(IllegalArgumentException.class,
				() -> service.registrarEmpleado(admin, "Pedro", "Gomez", "pedro@", "12345", 500000));
		recargar();

		assertEquals(antes, contar("SELECT COUNT(*) FROM Persona"));
	}

	// Un usuario puede registrarse con tope 0: solo va a poder comprar pagando
	@Test
	public void unUsuarioSePuedeRegistrarConTopeCero() {
		int usuario = nuevoUsuario(0);

		assertEquals(0, em.find(Usuario.class, usuario).getTopeCredito());
	}

	// --- empleados: casos de error ---

	// Asignar dos veces el mismo indoor al mismo empleado se rechaza y queda una sola fila en Trabaja
	@Test
	public void noSePuedeAsignarDosVecesElMismoIndoor() {
		int empleado = nuevoEmpleado();
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		service.asignarEmpleado(admin, empleado, indoor);
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.asignarEmpleado(admin, empleado, indoor));
		recargar();

		assertEquals(1, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado));
	}

	// Desasignar un indoor que el empleado no tenia se rechaza
	@Test
	public void noSePuedeDesasignarUnIndoorNoAsignado() {
		int empleado = nuevoEmpleado();
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.desasignarEmpleado(admin, empleado, indoor));
	}

	// Un empleado puede estar en varios indoors y desasignarlo de uno no toca los otros
	@Test
	public void desasignarUnIndoorNoTocaLosOtros() {
		int empleado = nuevoEmpleado();
		int primero = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		int segundo = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		service.asignarEmpleado(admin, empleado, primero);
		service.asignarEmpleado(admin, empleado, segundo);
		recargar();

		service.desasignarEmpleado(admin, empleado, primero);
		recargar();

		assertEquals(1, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado
				+ " AND ID_INDOOR = " + segundo));
		assertEquals(1, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado));
	}

	// Asignar con un empleado o un indoor inexistentes falla con RecursoNoEncontradoException
	@Test
	public void asignarConIdsInexistentesFalla() {
		int empleado = nuevoEmpleado();
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		recargar();

		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarEmpleado(admin, INEXISTENTE, indoor));
		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarEmpleado(admin, empleado, INEXISTENTE));
	}

	// Cambiar el salario de un empleado inexistente falla, y un salario negativo se rechaza
	@Test
	public void cambiarSalarioConDatosInvalidosFalla() {
		int empleado = nuevoEmpleado();

		assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarSalario(admin, INEXISTENTE, 600000));
		assertThrows(IllegalArgumentException.class, () -> service.cambiarSalario(admin, empleado, -1));
		recargar();

		assertEquals(500000, em.find(EmpleadoIndoor.class, empleado).getSalarioMensual());
	}

	// El id de un usuario no sirve como empleado: no se le puede cambiar el salario
	@Test
	public void unUsuarioNoEsUnEmpleado() {
		int usuario = nuevoUsuario(0);

		assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarSalario(admin, usuario, 600000));
	}

	// --- productos: casos de error ---

	// Un precio de 0 se rechaza y queda el anterior
	@Test
	public void unPrecioDeCeroSeRechaza() {
		int producto = productoConStock("OG Kush", 10);

		assertThrows(IllegalArgumentException.class, () -> service.cambiarPrecio(admin, producto, 0));
		recargar();

		assertEquals(1000, em.find(Producto.class, producto).getPrecio());
	}

	// Cambiar el precio o ajustar el stock de un producto inexistente falla
	@Test
	public void operarSobreUnProductoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarPrecio(admin, INEXISTENTE, 1000));
		assertThrows(RecursoNoEncontradoException.class, () -> service.ajustarStock(admin, INEXISTENTE, 5));
	}

	// Un ajuste de stock de 0 se rechaza
	@Test
	public void unAjusteDeStockDeCeroSeRechaza() {
		int producto = productoConStock("OG Kush", 10);

		assertThrows(IllegalArgumentException.class, () -> service.ajustarStock(admin, producto, 0));
	}

	// Un producto nuevo con precio 0 o sin genetica no se crea
	@Test
	public void unProductoInvalidoNoSeCrea() {
		assertThrows(IllegalArgumentException.class, () -> service.crearProducto(admin, "Northern Lights", 0));
		assertThrows(IllegalArgumentException.class, () -> service.crearProducto(admin, "  ", 1000));
		recargar();

		assertEquals(0, contar("SELECT COUNT(*) FROM Producto WHERE genetica = 'Northern Lights'"));
	}

	// --- el stock del admin y el catalogo ---

	// Con el stock fijado en 0 el producto sale del catalogo, y al volver a cargarle stock reaparece
	@Test
	public void elStockQueFijaElAdminDecideSiElProductoSeVende() {
		int producto = productoConStock("OG Kush", 10);

		service.fijarStock(admin, producto, 0);
		recargar();
		assertFalse(idsDelCatalogo().contains(producto));

		service.ajustarStock(admin, producto, 4);
		recargar();
		assertTrue(idsDelCatalogo().contains(producto));
	}

	// Una compra respeta el stock que fijo el admin: no se puede comprar mas de lo que quedo
	@Test
	public void unaCompraRespetaElStockAjustado() {
		int usuario = nuevoUsuario(0);
		int producto = productoConStock("OG Kush", 10);
		service.fijarStock(admin, producto, 2);
		recargar();

		assertThrows(StockInsuficienteException.class,
				() -> compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, 3)), true));
	}

	// --- usuarios y compras: casos de error ---

	// Asignar tope a un usuario inexistente falla; un tope negativo se rechaza y queda el anterior
	@Test
	public void asignarTopeConDatosInvalidosFalla() {
		int usuario = nuevoUsuario(3000);

		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarTope(admin, INEXISTENTE, 5000));
		assertThrows(IllegalArgumentException.class, () -> service.asignarTope(admin, usuario, -1));
		recargar();

		assertEquals(3000, em.find(Usuario.class, usuario).getTopeCredito());
	}

	// El id de un empleado no sirve como usuario: no se le puede asignar tope
	@Test
	public void unEmpleadoNoEsUnUsuario() {
		int empleado = nuevoEmpleado();

		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarTope(admin, empleado, 5000));
	}

	// Anular una compra que no existe falla con RecursoNoEncontradoException
	@Test
	public void anularUnaCompraInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.anularCompra(admin, INEXISTENTE));
	}

	// Anular una de dos compras impagas deja la otra, y la deuda baja solo en lo anulado
	@Test
	public void anularUnaDeDosComprasDejaLaOtra() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int primera = comprar(usuario, producto, 2, false);
		int segunda = comprar(usuario, producto, 3, false);

		service.anularCompra(admin, segunda);
		recargar();

		assertNotNull(em.find(Compra.class, primera));
		assertEquals(2000, em.find(Usuario.class, usuario).getDeuda().getMonto());
		assertEquals(8, em.find(Producto.class, producto).getStock());
	}

	// --- reportes: casos que cambian ---

	// El registro de produccion guarda quien cosecho, que producto, en que indoor, cuanto y cuando
	@Test
	public void elRegistroDeProduccionTieneTodosSusDatos() {
		int empleado = nuevoEmpleado();
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		service.asignarEmpleado(admin, empleado, indoor);
		int producto = service.crearProducto(admin, "OG Kush", 1000).getId();
		int planta = plantar(indoor);
		int registro = registroProduccionService.registrarCosecha(empleado, indoor, planta, 40).getId();
		recargar();

		RegistroProduccion encontrado = null;
		for (RegistroProduccion r : service.registrosDeProduccion(admin)) {
			if (r.getId() == registro) {
				encontrado = r;
			}
		}

		assertNotNull(encontrado);
		assertEquals(empleado, encontrado.getEmpleadoIndoor().getId());
		assertEquals(producto, encontrado.getProducto().getId());
		assertEquals(indoor, encontrado.getIndoor().getId());
		assertEquals(40, encontrado.getCantidad());
		assertEquals(LocalDate.now(), encontrado.getFechaRegistro());
	}

	// Una compra impaga anulada nunca aparece en los registros de pago
	@Test
	public void unaCompraAnuladaNoApareceEnLosPagos() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int compra = comprar(usuario, producto, 2, false);

		service.anularCompra(admin, compra);
		recargar();

		assertFalse(idsDePagos().contains(compra));
	}

	// Al pagar su deuda el usuario deja de ser deudor
	@Test
	public void alPagarDejaDeSerDeudor() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int compra = comprar(usuario, producto, 2, false);
		assertTrue(idsDeDeudores().contains(usuario));

		compraService.registrarPago(compra);
		recargar();

		assertFalse(idsDeDeudores().contains(usuario));
	}

	// Al anularle la compra impaga el usuario deja de ser deudor
	@Test
	public void alAnularleLaCompraDejaDeSerDeudor() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int compra = comprar(usuario, producto, 2, false);

		service.anularCompra(admin, compra);
		recargar();

		assertFalse(idsDeDeudores().contains(usuario));
	}

	// Los eventos pendientes juntan los de varios indoors distintos
	@Test
	public void losEventosPendientesJuntanVariosIndoors() {
		int primero = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		int segundo = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		int eventoPrimero = eventoPendiente(primero);
		int eventoSegundo = eventoPendiente(segundo);

		List<Integer> ids = idsDeEventosPendientes();

		assertTrue(ids.contains(eventoPrimero));
		assertTrue(ids.contains(eventoSegundo));
	}

	// Los eventos de una planta cosechada se descartan y ya no aparecen como pendientes
	@Test
	public void losEventosDeUnaPlantaCosechadaNoQuedanPendientes() {
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		int evento = eventoPendiente(indoor);
		em.find(Indoor.class, indoor).getPlantas().get(0).cosechar();
		recargar();

		assertFalse(idsDeEventosPendientes().contains(evento));
	}

	// --- plantar y listados del panel ---

	// El administrador planta en cualquier indoor, aunque no tenga empleados
	@Test
	public void elAdministradorPlantaEnCualquierIndoor() {
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		recargar();

		int planta = service.plantar(admin, indoor, new Planta("OG Kush", LocalDate.now().minusDays(5),
				LocalDate.now().minusDays(10), 60, 120, 30)).getId();
		recargar();

		assertEquals(indoor, em.find(Planta.class, planta).getIndoor().getId());
	}

	// El catalogo del admin incluye los productos sin stock, que el de los clientes esconde
	@Test
	public void losProductosDelAdminIncluyenLosSinStock() {
		int sinStock = service.crearProducto(admin, "Northern Lights", 1200).getId();
		recargar();

		List<Integer> ids = new ArrayList<>();
		for (Producto producto : service.productos(admin)) {
			ids.add(producto.getId());
		}

		assertTrue(ids.contains(sinStock));
	}

	// Las compras impagas del panel son solo las que falta pagar
	@Test
	public void lasComprasImpagasNoIncluyenLasPagadas() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int impaga = comprar(usuario, producto, 1, false);
		int pagada = comprar(usuario, producto, 1, true);

		List<Integer> ids = new ArrayList<>();
		for (Compra compra : service.comprasImpagas(admin)) {
			ids.add(compra.getId());
		}

		assertTrue(ids.contains(impaga));
		assertFalse(ids.contains(pagada));
	}

	// Los listados de indoors, empleados y usuarios incluyen lo que se dio de alta
	@Test
	public void losListadosIncluyenLoDadoDeAlta() {
		int indoor = service.crearIndoor(admin, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		int empleado = nuevoEmpleado();
		int usuario = nuevoUsuario(0);

		assertTrue(service.indoors(admin).stream().anyMatch(i -> i.getId() == indoor));
		assertTrue(service.empleados(admin).stream().anyMatch(e -> e.getId() == empleado));
		assertTrue(service.usuarios(admin).stream().anyMatch(u -> u.getId() == usuario));
	}

	// --- compras por aprobar y limites ---

	// Un pedido aparece en las compras por aprobar y no en las impagas; al aprobarlo pasa a las impagas
	@Test
	public void unPedidoEsperaYAlAprobarloPasaALasImpagas() {
		int usuario = nuevoUsuario(10000);
		int producto = productoConStock("OG Kush", 10);
		int pedido = compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, 2)), false).getId();
		recargar();

		assertTrue(service.comprasPendientes(admin).stream().anyMatch(c -> c.getId() == pedido));
		assertFalse(service.comprasImpagas(admin).stream().anyMatch(c -> c.getId() == pedido));

		service.aprobarCompra(admin, pedido);
		recargar();

		assertFalse(service.comprasPendientes(admin).stream().anyMatch(c -> c.getId() == pedido));
		assertTrue(service.comprasImpagas(admin).stream().anyMatch(c -> c.getId() == pedido));
		assertEquals(2000, em.find(Usuario.class, usuario).getDeuda().getMonto());
	}

	// El administrador rechaza un pedido: queda rechazado y el stock vuelve
	@Test
	public void elAdministradorRechazaUnPedido() {
		int usuario = nuevoUsuario(0);
		int producto = productoConStock("OG Kush", 10);
		int pedido = compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, 4)), true).getId();
		recargar();

		service.rechazarCompra(admin, pedido);
		recargar();

		assertTrue(em.find(Compra.class, pedido).isRechazada());
		assertEquals(10, em.find(Producto.class, producto).getStock());
	}

	// Cambiar los limites queda guardado y rige para los pedidos siguientes
	@Test
	public void losLimitesQueCambiaElAdministradorRigenParaLosPedidos() {
		int usuario = nuevoUsuario(0);
		int producto = productoConStock("OG Kush", 50);

		service.cambiarLimites(admin, 10, 20);
		recargar();

		assertEquals(10, service.limitesDeCompra(admin).getMinimoGramos());
		assertEquals(20, service.limitesDeCompra(admin).getMaximoGramos());
		assertThrows(OperacionInvalidaException.class,
				() -> compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, 9)), true));
		assertThrows(OperacionInvalidaException.class,
				() -> compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, 21)), true));
		compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, 15)), true);
	}

	// Limites invalidos se rechazan y quedan los anteriores
	@Test
	public void limitesInvalidosNoCambianNada() {
		service.cambiarLimites(admin, 5, 40);
		recargar();

		assertThrows(IllegalArgumentException.class, () -> service.cambiarLimites(admin, 30, 10));
		recargar();

		assertEquals(5, service.limitesDeCompra(admin).getMinimoGramos());
		assertEquals(40, service.limitesDeCompra(admin).getMaximoGramos());
	}

	// --- helpers ---

	private int nuevoEmpleado() {
		int id = service.registrarEmpleado(admin, "Pedro", "Gomez", "pedro@test.com", "12345", 500000).getId();
		recargar();
		return id;
	}

	private int nuevoUsuario(int tope) {
		return nuevoUsuario(tope, "juan@test.com");
	}

	private int nuevoUsuario(int tope, String email) {
		int id = solicitud(email);
		service.aprobarUsuario(admin, id, tope);
		recargar();
		return id;
	}

	// la solicitud de registro que manda quien quiere una cuenta: queda pendiente
	private int solicitud(String email) {
		int id = usuarioService.registrar("Juan", "Perez", email, "12345").getId();
		recargar();
		return id;
	}

	private List<Integer> idsDe(List<Usuario> usuarios) {
		List<Integer> ids = new ArrayList<>();
		for (Usuario usuario : usuarios) {
			ids.add(usuario.getId());
		}
		return ids;
	}

	private int productoConStock(String genetica, int stock) {
		int id = service.crearProducto(admin, genetica, 1000).getId();
		service.fijarStock(admin, id, stock);
		recargar();
		return id;
	}

	// el usuario la pide y el administrador la aprueba
	private int comprar(int usuario, int producto, int cantidad, boolean pagado) {
		int id = compraService.realizarCompra(usuario, List.of(new LineaCompra(producto, cantidad)), pagado).getId();
		service.aprobarCompra(admin, id);
		recargar();
		return id;
	}

	private int plantar(int indoor) {
		Indoor recargado = em.find(Indoor.class, indoor);
		Planta planta = recargado.addPlanta(
				new Planta("OG Kush", LocalDate.now().minusDays(60), LocalDate.now().minusDays(70), 60, 120, 30));
		em.flush();
		int id = planta.getId();
		em.clear();
		return id;
	}

	// todas las acciones del administrador, pedidas por alguien que no lo es
	private List<Executable> todasLasAcciones(int quien, int producto) {
		return List.of(
				() -> service.crearIndoor(quien, DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD),
				() -> service.registrarEmpleado(quien, "Pedro", "Gomez", "otro@test.com", "12345", 500000),
				() -> service.asignarEmpleado(quien, INEXISTENTE, INEXISTENTE),
				() -> service.desasignarEmpleado(quien, INEXISTENTE, INEXISTENTE),
				() -> service.cambiarSalario(quien, INEXISTENTE, 600000),
				() -> service.crearProducto(quien, "Nueva", 1000),
				() -> service.cambiarPrecio(quien, producto, 5000),
				() -> service.fijarStock(quien, producto, 0),
				() -> service.ajustarStock(quien, producto, -5),
				() -> service.solicitudesPendientes(quien),
				() -> service.aprobarUsuario(quien, INEXISTENTE, 0),
				() -> service.rechazarUsuario(quien, INEXISTENTE),
				() -> service.asignarTope(quien, INEXISTENTE, 5000),
				() -> service.anularCompra(quien, INEXISTENTE),
				() -> service.registrosDeProduccion(quien),
				() -> service.registrosDePago(quien),
				() -> service.deudores(quien),
				() -> service.eventosPendientes(quien),
				() -> service.plantar(quien, INEXISTENTE, new Planta("OG Kush", LocalDate.now(), LocalDate.now(), 60, 120, 30)),
				() -> service.indoors(quien),
				() -> service.empleados(quien),
				() -> service.productos(quien),
				() -> service.usuarios(quien),
				() -> service.comprasImpagas(quien),
				() -> service.comprasPendientes(quien),
				() -> service.aprobarCompra(quien, INEXISTENTE),
				() -> service.rechazarCompra(quien, INEXISTENTE),
				() -> service.limitesDeCompra(quien),
				() -> service.cambiarLimites(quien, 1, 10));
	}

	private int eventoPendiente(int indoor) {
		int planta = plantar(indoor);
		Indoor recargado = em.find(Indoor.class, indoor);
		EventoRegado evento = new EventoRegado(recargado.buscarPlanta(planta));
		recargado.recibirEvento(evento);
		em.flush();
		int id = evento.getId();
		em.clear();
		return id;
	}

	private List<Integer> idsDeEventosPendientes() {
		List<Integer> ids = new ArrayList<>();
		for (Evento evento : service.eventosPendientes(admin)) {
			ids.add(evento.getId());
		}
		return ids;
	}

	private List<Integer> idsDelCatalogo() {
		return em.getEntityManager().createQuery("select p.id from Producto p where p.stock > 0", Integer.class)
				.getResultList();
	}

	private List<Integer> idsDeDeudores() {
		List<Integer> ids = new ArrayList<>();
		for (Usuario usuario : service.deudores(admin)) {
			ids.add(usuario.getId());
		}
		return ids;
	}

	private List<Integer> idsDePagos() {
		List<Integer> ids = new ArrayList<>();
		for (Compra compra : service.registrosDePago(admin)) {
			ids.add(compra.getId());
		}
		return ids;
	}

	private long contar(String sql) {
		return ((Number) em.getEntityManager().createNativeQuery(sql).getSingleResult()).longValue();
	}

	private void recargar() {
		em.flush();
		em.clear();
	}
}
