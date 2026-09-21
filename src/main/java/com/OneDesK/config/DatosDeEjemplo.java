package com.OneDesK.config;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.OneDesK.evento.Evento;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.AdministradorRepository;
import com.OneDesK.services.AdministradorService;
import com.OneDesK.services.CompraService;
import com.OneDesK.services.EmpleadoIndoorService;
import com.OneDesK.services.GeneradorDeEventosService;
import com.OneDesK.services.LineaCompra;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.RegistroProduccionService;
import com.OneDesK.services.UsuarioService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Carga un negocio de ejemplo para ver la aplicacion funcionando: indoors, empleados, geneticas, plantas,
 * cosechas, clientes y compras de las ultimas semanas. Corre al arrancar, una sola vez: solo si todavia
 * no hay cuentas de ejemplo. Convive con los datos que ya haya en la base. Se apaga con onedesk.demo.activo=false (los tests la tienen apagada).
 *
 * Todo se crea con los services, asi se cumplen las mismas reglas que en la aplicacion (stock, topes,
 * gramos, capacidad, aprobaciones). Los services fechan todo con el dia de hoy: al final se corren las
 * fechas hacia atras con SQL para que quede un historial de varias semanas.
 *
 * Las cuentas de ejemplo usan el email @onedesk.demo y la contrasenia "demo1".
 */
@Component
@Order(2)
public class DatosDeEjemplo implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DatosDeEjemplo.class);

	private static final String CLAVE = "demo1";
	private static final int DIAS_DE_HISTORIAL = 42;

	@Autowired
	private AdministradorRepository administradorRepository;
	@Autowired
	private AdministradorService admin;
	@Autowired
	private EmpleadoIndoorService empleadoService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private CompraService compraService;
	@Autowired
	private RegistroProduccionService registroProduccionService;
	@Autowired
	private GeneradorDeEventosService generadorDeEventos;
	@Autowired
	private JdbcTemplate jdbc;

	@Value("${onedesk.demo.activo:false}")
	private boolean activo;

	@Autowired
	private PlatformTransactionManager transacciones;
	@PersistenceContext
	private EntityManager entityManager;

	// siempre la misma semilla: cada carga genera exactamente los mismos datos
	private final Random azar = new Random(42);
	private final List<CambioDeFecha> fechas = new ArrayList<>();

	@Override
	public void run(ApplicationArguments args) {
		// corre una sola vez: si ya hay cuentas de ejemplo, el ejemplo ya se cargo
		if (!activo || yaSeCargo()) {
			return;
		}
		if (administradorRepository.count() == 0) {
			log.warn("No hay ningun administrador: no se cargan los datos de ejemplo");
			return;
		}
		log.info("Cargando los datos de ejemplo...");
		try {
			cargar(administradorRepository.findAll().get(0).getId());
			log.info("Datos de ejemplo cargados. Cuentas de ejemplo: email @onedesk.demo, contrasenia {}", CLAVE);
		} catch (RuntimeException error) {
			// por ejemplo, si ya existe un indoor o un producto con el mismo nombre: no se carga nada
			// (la carga es una sola transaccion) y la aplicacion arranca igual
			log.warn("No se pudieron cargar los datos de ejemplo: {}", error.getMessage());
		}
	}

	private boolean yaSeCargo() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM Persona WHERE email LIKE '%@onedesk.demo'", Integer.class) > 0;
	}

	/**
	 * Carga todo el negocio de ejemplo en una sola transaccion: si algo falla no queda nada a medias.
	 * Las acciones de administracion las hace ese administrador.
	 */
	public void cargar(int adminId) {
		new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
			cargarProductos(adminId);
			List<IndoorDeEjemplo> indoors = cargarIndoorsYEmpleados(adminId);
			cargarPlantasYCosechas(indoors);
			generarEventos(indoors);
			List<Integer> clientes = cargarClientes(adminId);
			cargarCompras(adminId, clientes);
			aplicarFechas();
		});
	}

	// Las fechas se corren con SQL recien al final, despues de que Hibernate guardo todo: si se corrieran
	// antes, al guardar una compra aprobada Hibernate volveria a escribir la fecha de hoy que tiene en memoria.
	private void aplicarFechas() {
		entityManager.flush();
		for (CambioDeFecha cambio : fechas) {
			jdbc.update("UPDATE " + cambio.tabla + " SET " + cambio.columna + " = ? WHERE ID = ? AND "
					+ cambio.columna + " IS NOT NULL", cambio.fecha, cambio.id);
		}
		entityManager.clear();
	}

	private void correrFecha(String tabla, String columna, int id, LocalDate fecha) {
		fechas.add(new CambioDeFecha(tabla, columna, id, fecha));
	}

	// --- catalogo ---

	private void cargarProductos(int adminId) {
		producto(adminId, "OG Kush", 1200);
		producto(adminId, "Amnesia Haze", 1500);
		producto(adminId, "Blue Dream", 1400);
		producto(adminId, "Kush Mints", 1800);
		producto(adminId, "Gorilla Glue", 1600);
		producto(adminId, "Northern Lights", 1300);
		producto(adminId, "Gelato", 2000);
		producto(adminId, "White Widow", 1100);
	}

	private void producto(int adminId, String genetica, int precio) {
		admin.crearProducto(adminId, genetica, precio);
	}

	// --- indoors y empleados ---

	private List<IndoorDeEjemplo> cargarIndoorsYEmpleados(int adminId) {
		int lucia = admin.registrarEmpleado(adminId, "Lucía", "Díaz", "lucia@onedesk.demo", CLAVE, 520000).getId();
		int martin = admin.registrarEmpleado(adminId, "Martín", "Ruiz", "martin@onedesk.demo", CLAVE, 480000).getId();
		int sofia = admin.registrarEmpleado(adminId, "Sofía", "Paz", "sofia@onedesk.demo", CLAVE, 500000).getId();

		IndoorDeEjemplo grande = new IndoorDeEjemplo(admin.crearIndoor(adminId, "Carpa grande", 12).getId(), lucia);
		IndoorDeEjemplo chica = new IndoorDeEjemplo(admin.crearIndoor(adminId, "Carpa chica", 6).getId(), martin);
		IndoorDeEjemplo flora = new IndoorDeEjemplo(admin.crearIndoor(adminId, "Sala de flora", 10).getId(), sofia);

		admin.asignarEmpleado(adminId, lucia, grande.id);
		admin.asignarEmpleado(adminId, martin, chica.id);
		admin.asignarEmpleado(adminId, sofia, flora.id);
		admin.asignarEmpleado(adminId, sofia, grande.id);

		// las geneticas que se cosecharon (con sus gramos) y las que siguen en cultivo en cada indoor
		grande.cosechadas("OG Kush", 180, "Amnesia Haze", 140, "Gelato", 120);
		grande.enCultivo("OG Kush", "OG Kush", "OG Kush", "Gelato", "Gelato", "Kush Mints", "Kush Mints",
				"Blue Dream", "Blue Dream");
		chica.cosechadas("White Widow", 110, "Northern Lights", 100);
		chica.enCultivo("White Widow", "White Widow", "Northern Lights", "Northern Lights");
		flora.cosechadas("Gorilla Glue", 160, "Blue Dream", 140, "Kush Mints", 130);
		flora.enCultivo("Amnesia Haze", "Amnesia Haze", "Gorilla Glue", "Gorilla Glue", "Blue Dream", "Kush Mints");
		return List.of(grande, chica, flora);
	}

	// --- plantas y cosechas ---

	private void cargarPlantasYCosechas(List<IndoorDeEjemplo> indoors) {
		for (IndoorDeEjemplo indoor : indoors) {
			for (Cosecha cosecha : indoor.cosechadas) {
				int diasPlantada = 95 + azar.nextInt(30);
				int planta = plantar(indoor, cosecha.genetica, diasPlantada);
				int registro = registroProduccionService
						.registrarCosecha(indoor.empleadoId, indoor.id, planta, cosecha.gramos).getId();
				int diasCosechada = 3 + azar.nextInt(DIAS_DE_HISTORIAL);
				correrFecha("Planta", "fechaCosecha", planta, haceDias(diasCosechada));
				correrFecha("RegistroProduccion", "fechaRegistro", registro, haceDias(diasCosechada));
			}
			for (String genetica : indoor.enCultivo) {
				indoor.plantas.add(plantar(indoor, genetica, 5 + azar.nextInt(70)));
			}
		}
	}

	private int plantar(IndoorDeEjemplo indoor, String genetica, int diasPlantada) {
		LocalDate plantada = LocalDate.now().minusDays(diasPlantada);
		Planta planta = new Planta(genetica, plantada, plantada.minusDays(7 + azar.nextInt(7)),
				600 + azar.nextInt(4) * 120, 720, 180 + azar.nextInt(3) * 60);
		return empleadoService.plantar(indoor.empleadoId, indoor.id, planta).getId();
	}

	// --- eventos: algunas plantas quedan con tareas pendientes y otras ya se atendieron ---

	private void generarEventos(List<IndoorDeEjemplo> indoors) {
		// lo pendiente se guarda antes del SQL, y despues se vacia la memoria de Hibernate:
		// asi el generador lee de la base los horarios viejos y no los de hoy que tenia en memoria
		entityManager.flush();
		for (IndoorDeEjemplo indoor : indoors) {
			for (int i = 0; i < indoor.plantas.size(); i += 2) {
				jdbc.update("UPDATE Planta SET ultimoRegado = NOW() - INTERVAL 13 HOUR,"
						+ " ultimoVentilacion = NOW() - INTERVAL 5 HOUR WHERE ID = ?", indoor.plantas.get(i));
			}
		}
		entityManager.clear();
		generadorDeEventos.generarEventos();

		// cada empleado ya atendio uno de sus pendientes: quedan en el historial como realizados
		for (IndoorDeEjemplo indoor : indoors) {
			List<Evento> pendientes = empleadoService.eventosPendientes(indoor.empleadoId);
			for (Evento evento : pendientes) {
				if (evento.getPlanta().getIndoor().getId() == indoor.id) {
					empleadoService.atenderEvento(indoor.empleadoId, indoor.id, evento.getId());
					break;
				}
			}
		}
	}

	// --- clientes ---

	private List<Integer> cargarClientes(int adminId) {
		List<Integer> aprobados = new ArrayList<>();
		aprobados.add(cliente(adminId, "Valentina", "Gómez", "valentina", 40000));
		aprobados.add(cliente(adminId, "Tomás", "Ríos", "tomas", 25000));
		aprobados.add(cliente(adminId, "Camila", "Suárez", "camila", 60000));
		aprobados.add(cliente(adminId, "Joaquín", "Medina", "joaquin", 0));
		aprobados.add(cliente(adminId, "Micaela", "Ortiz", "micaela", 30000));
		aprobados.add(cliente(adminId, "Nicolás", "Herrera", "nicolas", 15000));
		aprobados.add(cliente(adminId, "Florencia", "Castro", "florencia", 50000));
		aprobados.add(cliente(adminId, "Agustín", "Molina", "agustin", 0));
		// dos personas que pidieron cuenta y todavia esperan respuesta
		usuarioService.registrar("Julieta", "Romero", "julieta@onedesk.demo", CLAVE);
		usuarioService.registrar("Bruno", "Acosta", "bruno@onedesk.demo", CLAVE);
		return aprobados;
	}

	private int cliente(int adminId, String nombre, String apellido, String usuario, int tope) {
		int id = usuarioService.registrar(nombre, apellido, usuario + "@onedesk.demo", CLAVE).getId();
		admin.aprobarUsuario(adminId, id, tope);
		return id;
	}

	// --- compras de las ultimas semanas ---

	private void cargarCompras(int adminId, List<Integer> clientes) {
		List<Integer> productos = jdbc.queryForList("SELECT ID FROM Producto ORDER BY ID", Integer.class);
		int total = 32;
		for (int i = 0; i < total; i++) {
			int cliente = clientes.get(i % clientes.size());
			// los clientes con tope 0 solo pueden pagar; los demas a veces dejan la compra en cuenta
			boolean pagaAlAprobar = tope(cliente) == 0 || azar.nextInt(3) > 0;
			List<LineaCompra> lineas = new ArrayList<>();
			lineas.add(new LineaCompra(productos.get(azar.nextInt(productos.size())), 5 + azar.nextInt(16)));
			if (azar.nextBoolean()) {
				lineas.add(new LineaCompra(productos.get(azar.nextInt(productos.size())), 3 + azar.nextInt(10)));
			}
			// de la mas vieja a la mas nueva: las ultimas quedan esperando aprobacion
			int dias = DIAS_DE_HISTORIAL - (i * DIAS_DE_HISTORIAL / total);
			// si una regla la frenaria (poco stock, tope), esa compra de ejemplo simplemente no se hace.
			// Se mira antes y no atajando el error: una excepcion adentro de la transaccion la deja
			// marcada para deshacerse, y se perderia toda la carga.
			if (sePuedeComprar(cliente, lineas, pagaAlAprobar)) {
				Compra compra = compraService.realizarCompra(cliente, lineas, pagaAlAprobar);
				responder(adminId, compra, i, total, dias);
			}
		}
	}

	private boolean sePuedeComprar(int cliente, List<LineaCompra> lineas, boolean pagaAlAprobar) {
		int total = 0;
		for (LineaCompra linea : lineas) {
			int pedido = 0;
			for (LineaCompra otra : lineas) {
				if (otra.productoId() == linea.productoId()) {
					pedido += otra.cantidad();
				}
			}
			Producto producto = productoService.buscar(linea.productoId());
			if (producto.getStock() < pedido) {
				return false;
			}
			total += producto.getPrecio() * linea.cantidad();
		}
		Usuario usuario = usuarioService.buscar(cliente);
		return pagaAlAprobar || usuario.deudaComprometida() + total <= usuario.getTopeCredito();
	}

	private void responder(int adminId, Compra compra, int indice, int total, int dias) {
		int id = compra.getId();
		correrFecha("Compra", "fechaCompra", id, haceDias(dias));
		if (indice >= total - 4) {
			return;
		}
		if (indice % 9 == 4) {
			admin.rechazarCompra(adminId, id);
			return;
		}
		compra = admin.aprobarCompra(adminId, id);
		// algunas en cuenta corriente se pagaron despues; las mas nuevas siguen impagas
		if (!compra.isPagado() && indice < total / 2) {
			compraService.registrarPago(id);
		}
		// el pago quedo fechado hoy: se lo corre a unos dias despues de la compra (solo si se pago)
		correrFecha("Compra", "fechaPago", id, haceDias(Math.max(0, dias - azar.nextInt(4))));
	}

	private int tope(int usuarioId) {
		return usuarioService.buscar(usuarioId).getTopeCredito();
	}

	private LocalDate haceDias(int dias) {
		return LocalDate.now().minusDays(dias);
	}

	/** Un indoor del ejemplo con su empleado, lo que se cosecho y lo que sigue en cultivo. */
	private static class IndoorDeEjemplo {

		private final int id;
		private final int empleadoId;
		private final List<Cosecha> cosechadas = new ArrayList<>();
		private final List<String> enCultivo = new ArrayList<>();
		private final List<Integer> plantas = new ArrayList<>();

		IndoorDeEjemplo(int id, int empleadoId) {
			this.id = id;
			this.empleadoId = empleadoId;
		}

		// de a pares: genetica, gramos, genetica, gramos
		void cosechadas(Object... geneticaYGramos) {
			for (int i = 0; i < geneticaYGramos.length; i += 2) {
				cosechadas.add(new Cosecha((String) geneticaYGramos[i], (Integer) geneticaYGramos[i + 1]));
			}
		}

		void enCultivo(String... geneticas) {
			enCultivo.addAll(List.of(geneticas));
		}
	}

	/** Una fecha que se corre hacia atras al final de la carga. */
	private static class CambioDeFecha {

		private final String tabla;
		private final String columna;
		private final int id;
		private final LocalDate fecha;

		CambioDeFecha(String tabla, String columna, int id, LocalDate fecha) {
			this.tabla = tabla;
			this.columna = columna;
			this.id = id;
			this.fecha = fecha;
		}
	}

	/** Una planta que ya se cosecho: su genetica y cuantos gramos dio. */
	private static class Cosecha {

		private final String genetica;
		private final int gramos;

		Cosecha(String genetica, int gramos) {
			this.genetica = genetica;
			this.gramos = gramos;
		}
	}
}
