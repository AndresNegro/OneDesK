package com.OneDesK.modelo;

import com.OneDesK.DatosDePrueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.OneDesK.evento.Evento;
import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.evento.EventoVentilador;

// Auditoria de persistencia: cada entidad se guarda en MySQL y se vuelve a leer con todos sus datos.
// Lo que no tiene getter, como varias columnas de Planta o el discriminador de Evento, se lee con SQL:
// es la unica forma de comprobar que el valor llego a la base y no solo al objeto en memoria.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PersistenciaDeEntidadesTest {

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private TestEntityManager em;

	// Persona, Usuario y Deuda: el usuario se guarda con sus datos y su deuda en las tres tablas
	@Test
	public void unUsuarioSeGuardaConSuPersonaYSuDeuda() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		usuario.setTopeCredito(20000);
		em.persistAndFlush(usuario);
		int id = usuario.getId();
		em.clear();

		Usuario recargado = em.find(Usuario.class, id);
		assertEquals("Andres", recargado.getNombre());
		assertEquals("Negro", recargado.getApellido());
		assertEquals("andres@test.com", recargado.getEmail());
		assertEquals("12345", recargado.getContrasenia());
		assertEquals(20000, recargado.getTopeCredito());
		assertEquals(0, recargado.getDeuda().getMonto());

		assertEquals(1, contar("SELECT COUNT(*) FROM Persona WHERE ID = " + id));
		assertEquals(1, contar("SELECT COUNT(*) FROM Usuario WHERE ID = " + id));
		assertEquals(1, contar("SELECT COUNT(*) FROM Deuda WHERE ID = " + recargado.getDeuda().getId()));
	}

	// La herencia JOINED escribe una fila en Persona, otra en Empleado y otra en EmpleadoIndoor
	@Test
	public void unEmpleadoIndoorSeGuardaEnLasTresTablasDeLaHerencia() {
		EmpleadoIndoor empleado = new EmpleadoIndoor("Andres", "Negro", "empleado@test.com", "12345", 500000);
		em.persistAndFlush(empleado);
		int id = empleado.getId();
		em.clear();

		EmpleadoIndoor recargado = em.find(EmpleadoIndoor.class, id);
		assertEquals("empleado@test.com", recargado.getEmail());
		assertEquals(500000, recargado.getSalarioMensual());

		assertEquals(1, contar("SELECT COUNT(*) FROM Persona WHERE ID = " + id));
		assertEquals(1, contar("SELECT COUNT(*) FROM Empleado WHERE ID = " + id));
		assertEquals(1, contar("SELECT COUNT(*) FROM EmpleadoIndoor WHERE ID = " + id));
	}

	// La herencia JOINED escribe una fila en Persona y otra en Administrador
	@Test
	public void unAdministradorSeGuardaEnPersonaYAdministrador() {
		Administrador admin = new Administrador("Ana", "Admin", "admin@test.com", "12345");
		em.persistAndFlush(admin);
		int id = admin.getId();
		em.clear();

		Administrador recargado = em.find(Administrador.class, id);
		assertEquals("admin@test.com", recargado.getEmail());
		assertEquals("Ana", recargado.getNombre());

		assertEquals(1, contar("SELECT COUNT(*) FROM Persona WHERE ID = " + id));
		assertEquals(1, contar("SELECT COUNT(*) FROM Administrador WHERE ID = " + id));
		assertEquals(0, contar("SELECT COUNT(*) FROM Usuario WHERE ID = " + id));
		assertEquals(0, contar("SELECT COUNT(*) FROM Empleado WHERE ID = " + id));
	}

	// Indoor y Planta: se guardan las 14 columnas de la planta, incluidas las que no tienen getter
	@Test
	public void unaPlantaSeGuardaConTodasSusColumnas() {
		Indoor indoor = DatosDePrueba.indoor();
		Planta planta = indoor.addPlanta(new Planta("OG Kush", HOY.minusDays(80), HOY.minusDays(90), 60, 120, 30));
		planta.regar();
		planta.setLuz(true);
		planta.setVentilador(true);
		planta.cosechar();
		em.persistAndFlush(indoor);
		int id = planta.getId();
		em.clear();

		Planta recargada = em.find(Planta.class, id);
		assertEquals("OG Kush", recargada.getGenetica());
		assertEquals(HOY, recargada.getFechaCosecha());
		assertTrue(recargada.isLuz());
		assertTrue(recargada.isVentilador());
		assertEquals(indoor.getId(), recargada.getIndoor().getId());

		Object[] fila = (Object[]) em.getEntityManager().createNativeQuery(
				"SELECT fechaPlantado, fechaGerminado, tiempoRegado, tiempoLuz, tiempoVentilacion,"
						+ " ultimoRegado, ultimoLuz, ultimoVentilacion FROM Planta WHERE ID = " + id)
				.getSingleResult();

		assertEquals(HOY.minusDays(80), ((Date) fila[0]).toLocalDate());
		assertEquals(HOY.minusDays(90), ((Date) fila[1]).toLocalDate());
		assertEquals(60, fila[2]);
		assertEquals(120, fila[3]);
		assertEquals(30, fila[4]);
		// los ultimo* son DATETIME: se guardan con la hora, no solo con el dia
		assertMismoSegundo(planta.getUltimoRegado(), aFechaHora(fila[5]));
		assertMismoSegundo(planta.getUltimoLuz(), aFechaHora(fila[6]));
		assertMismoSegundo(planta.getUltimoVentilacion(), aFechaHora(fila[7]));
	}

	// Los tres tipos de evento van a la misma tabla y se distinguen por la columna tipo.
	// Se consulta cada uno por su id porque Hibernate inserta agrupando por clase, no en el orden de la cola.
	@Test
	public void losTresTiposDeEventoSeGuardanConSuDiscriminador() {
		Indoor indoor = DatosDePrueba.indoor();
		Planta planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		EventoLuz luz = new EventoLuz(planta);
		EventoRegado riego = new EventoRegado(planta);
		EventoVentilador ventilador = new EventoVentilador(planta);
		indoor.recibirEvento(luz);
		indoor.recibirEvento(riego);
		indoor.recibirEvento(ventilador);
		riego.setRealizado(true);
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();
		int idLuz = luz.getId();
		int idRiego = riego.getId();
		int idVentilador = ventilador.getId();
		em.clear();

		assertEquals("LUZ", tipoDelEvento(idLuz));
		assertEquals("REGADO", tipoDelEvento(idRiego));
		assertEquals("VENTILADOR", tipoDelEvento(idVentilador));

		Indoor recargado = em.find(Indoor.class, idIndoor);
		assertInstanceOf(EventoLuz.class, recargado.buscarEvento(idLuz));
		assertInstanceOf(EventoRegado.class, recargado.buscarEvento(idRiego));
		assertInstanceOf(EventoVentilador.class, recargado.buscarEvento(idVentilador));
		assertTrue(recargado.buscarEvento(idRiego).getRealizado());
		assertEquals(2, recargado.getEventosPendientes().size());
	}

	// Producto: el catalogo se guarda con su genetica, su precio y su stock
	@Test
	public void unProductoSeGuardaConSuStockYSuPrecio() {
		Producto producto = new Producto("OG Kush", 12, 1000);
		em.persistAndFlush(producto);
		int id = producto.getId();
		em.clear();

		Producto recargado = em.find(Producto.class, id);
		assertEquals("OG Kush", recargado.getGenetica());
		assertEquals(12, recargado.getStock());
		assertEquals(1000, recargado.getPrecio());
	}

	// Compra e ItemCompra: se guardan la fecha, el total, el estado y el precio congelado del item
	@Test
	public void unaCompraSeGuardaConSusItems() {
		Usuario usuario = new Usuario("Andres", "Negro", "comprador@test.com", "12345");
		Producto producto = new Producto("OG Kush", 12, 1000);
		em.persist(usuario);
		em.persist(producto);

		Compra compra = new Compra(HOY, false, usuario);
		compra.addItem(new ItemCompra(producto, 3));
		em.persistAndFlush(compra);
		int id = compra.getId();
		em.clear();

		Compra recargada = em.find(Compra.class, id);
		assertEquals(HOY, recargada.getFechaCompra());
		assertEquals(3000, recargada.getPrecio());
		assertEquals(false, recargada.isPagado());
		assertEquals(usuario.getId(), recargada.getUsuario().getId());
		assertEquals(1, recargada.getItems().size());

		ItemCompra item = recargada.getItems().get(0);
		assertEquals(3, item.getCantidad());
		assertEquals(1000, item.getPrecioUnitario());
		assertEquals(producto.getId(), item.getProducto().getId());
		assertNull(recargada.getFechaPago());
	}

	// La fecha de pago se guarda en su columna: vacia si la compra esta impaga y con fecha al pagarla
	@Test
	public void laFechaDePagoSeGuardaEnLaCompra() {
		Usuario usuario = new Usuario("Andres", "Negro", "comprador@test.com", "12345");
		em.persist(usuario);
		Compra pagadaAlComprar = new Compra(HOY.minusDays(3), true, usuario);
		Compra pagadaDespues = new Compra(HOY.minusDays(3), false, usuario);
		em.persist(pagadaAlComprar);
		em.persist(pagadaDespues);
		em.flush();
		assertEquals(0, contar("SELECT COUNT(*) FROM Compra WHERE ID = " + pagadaDespues.getId()
				+ " AND fechaPago IS NOT NULL"));

		pagadaDespues.marcarComoPagada();
		em.flush();
		em.clear();

		Object pagoAlComprar = em.getEntityManager()
				.createNativeQuery("SELECT fechaPago FROM Compra WHERE ID = " + pagadaAlComprar.getId())
				.getSingleResult();
		Object pagoDespues = em.getEntityManager()
				.createNativeQuery("SELECT fechaPago FROM Compra WHERE ID = " + pagadaDespues.getId())
				.getSingleResult();
		assertEquals(HOY.minusDays(3), ((Date) pagoAlComprar).toLocalDate());
		assertEquals(HOY, ((Date) pagoDespues).toLocalDate());
	}

	// La deuda guardada es la suma de las compras impagas del usuario
	@Test
	public void laDeudaSeGuardaConElMontoDeLasComprasImpagas() {
		Usuario usuario = new Usuario("Andres", "Negro", "deudor@test.com", "12345");
		Producto producto = new Producto("OG Kush", 12, 1000);
		em.persist(usuario);
		em.persist(producto);

		Compra compra = new Compra(HOY, false, usuario);
		compra.addItem(new ItemCompra(producto, 3));
		usuario.agregarCompra(compra);
		em.persistAndFlush(compra);
		int idDeuda = usuario.getDeuda().getId();
		em.clear();

		assertEquals(3000, contar("SELECT monto FROM Deuda WHERE ID = " + idDeuda));
	}

	// RegistroProduccion: la cosecha queda guardada con su planta, su empleado, su producto y su fecha
	@Test
	public void unRegistroDeProduccionSeGuardaCompleto() {
		Indoor indoor = DatosDePrueba.indoor();
		Planta planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		EmpleadoIndoor empleado = new EmpleadoIndoor("Andres", "Negro", "cosechador@test.com", "12345", 500000);
		Producto producto = new Producto("OG Kush", 0, 1000);
		em.persist(indoor);
		em.persist(empleado);
		em.persist(producto);
		em.flush();

		RegistroProduccion registro = new RegistroProduccion(planta, empleado, producto, 50);
		em.persistAndFlush(registro);
		int id = registro.getId();
		em.clear();

		RegistroProduccion recargado = em.find(RegistroProduccion.class, id);
		assertEquals(50, recargado.getCantidad());
		assertEquals(HOY, recargado.getFechaRegistro());
		assertEquals(planta.getId(), recargado.getPlanta().getId());
		assertEquals(indoor.getId(), recargado.getIndoor().getId());
		assertEquals(empleado.getId(), recargado.getEmpleadoIndoor().getId());
		assertEquals(producto.getId(), recargado.getProducto().getId());
	}

	// Trabaja: la asignacion de un empleado a un indoor deja su fila en la tabla intermedia
	@Test
	public void laAsignacionDeUnEmpleadoAUnIndoorSeGuardaEnTrabaja() {
		Indoor indoor = DatosDePrueba.indoor();
		EmpleadoIndoor empleado = new EmpleadoIndoor("Andres", "Negro", "asignado@test.com", "12345", 500000);
		em.persist(indoor);
		em.persist(empleado);
		empleado.addIndoor(indoor);
		em.flush();
		em.clear();

		assertEquals(1, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + empleado.getId()
				+ " AND ID_INDOOR = " + indoor.getId()));
		assertEquals(1, em.find(EmpleadoIndoor.class, empleado.getId()).getSectoresACargo().size());
	}

	private String tipoDelEvento(int id) {
		return (String) em.getEntityManager()
				.createNativeQuery("SELECT tipo FROM Evento WHERE ID = " + id).getSingleResult();
	}

	private int contar(String sql) {
		return ((Number) em.getEntityManager().createNativeQuery(sql).getSingleResult()).intValue();
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, HOY.minusDays(80), HOY.minusDays(90), 60, 120, 30);
	}

	// DATETIME no guarda fracciones de segundo y MySQL las redondea: se tolera un segundo de diferencia
	private void assertMismoSegundo(LocalDateTime esperado, LocalDateTime guardado) {
		assertTrue(Duration.between(esperado, guardado).abs().getSeconds() <= 1,
				"Se esperaba " + esperado + " y se guardo " + guardado);
	}

	// segun la version del driver, una columna DATETIME llega como Timestamp o como LocalDateTime
	private LocalDateTime aFechaHora(Object valor) {
		if (valor instanceof Timestamp) {
			return ((Timestamp) valor).toLocalDateTime();
		}
		return (LocalDateTime) valor;
	}
}
