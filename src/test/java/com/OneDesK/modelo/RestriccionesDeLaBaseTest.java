package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import jakarta.persistence.PersistenceException;

// Lo que hace cumplir la base por su cuenta: claves foraneas y valores unicos.
// Todo se ejecuta con SQL directo, salteando JPA, porque es la unica forma de comprobar
// que la base frena la operacion aunque el codigo de la aplicacion no la frene.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RestriccionesDeLaBaseTest {

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private TestEntityManager em;

	// --- valores unicos ---

	// La base rechaza dos productos con la misma genetica, sin distinguir mayusculas
	@Test
	public void laBaseRechazaDosProductosConLaMismaGenetica() {
		em.persistAndFlush(new Producto("OG Kush", 10, 1000));
		em.clear();

		assertThrows(PersistenceException.class,
				() -> ejecutar("INSERT INTO Producto (genetica, precio, stock) VALUES ('og kush', 900, 5)"));
	}

	// --- borrados que la base tiene que frenar ---

	// Un producto que alguien compro no se puede borrar: su ItemCompra lo referencia
	@Test
	public void noSePuedeBorrarUnProductoQueYaSeCompro() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		Producto producto = new Producto("OG Kush", 10, 1000);
		em.persist(usuario);
		em.persist(producto);
		Compra compra = new Compra(HOY, false, usuario);
		compra.addItem(new ItemCompra(producto, 2));
		em.persistAndFlush(compra);
		int idProducto = producto.getId();
		em.clear();

		assertThrows(PersistenceException.class, () -> ejecutar("DELETE FROM Producto WHERE ID = " + idProducto));
	}

	// Un usuario con compras no se puede borrar: se perderia el historial de ventas
	@Test
	public void noSePuedeBorrarUnUsuarioConCompras() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		em.persist(usuario);
		em.persistAndFlush(new Compra(HOY, false, usuario));
		int idUsuario = usuario.getId();
		em.clear();

		assertThrows(PersistenceException.class, () -> ejecutar("DELETE FROM Usuario WHERE ID = " + idUsuario));
	}

	// Un empleado con cosechas cargadas no se puede borrar: su registro de produccion lo referencia
	@Test
	public void noSePuedeBorrarUnEmpleadoConRegistrosDeProduccion() {
		Indoor indoor = new Indoor();
		Planta planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		EmpleadoIndoor empleado = nuevoEmpleado("cosechador@test.com");
		Producto producto = new Producto("OG Kush", 0, 1000);
		em.persist(indoor);
		em.persist(empleado);
		em.persist(producto);
		em.flush();
		em.persistAndFlush(new RegistroProduccion(planta, empleado, producto, 50));
		int idEmpleado = empleado.getId();
		em.clear();

		assertThrows(PersistenceException.class,
				() -> ejecutar("DELETE FROM EmpleadoIndoor WHERE ID = " + idEmpleado));
	}

	// Una planta con eventos no se puede borrar: sus eventos quedarian sin planta
	@Test
	public void noSePuedeBorrarUnaPlantaConEventos() {
		Indoor indoor = new Indoor();
		Planta planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		indoor.recibirEvento(new com.OneDesK.evento.EventoLuz(planta));
		em.persistAndFlush(indoor);
		int idPlanta = planta.getId();
		em.clear();

		assertThrows(PersistenceException.class, () -> ejecutar("DELETE FROM Planta WHERE ID = " + idPlanta));
	}

	// --- borrados que la base arrastra en cadena ---

	// Borrar la persona borra tambien su fila de usuario
	@Test
	public void borrarUnaPersonaArrastraSuUsuario() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		em.persistAndFlush(usuario);
		int id = usuario.getId();
		em.clear();

		ejecutar("DELETE FROM Persona WHERE ID = " + id);

		assertEquals(0, contar("SELECT COUNT(*) FROM Usuario WHERE ID = " + id));
	}

	// Borrar un indoor arrastra sus plantas
	@Test
	public void borrarUnIndoorArrastraSusPlantas() {
		Indoor indoor = new Indoor();
		Planta planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();
		int idPlanta = planta.getId();
		em.clear();

		ejecutar("DELETE FROM Indoor WHERE ID = " + idIndoor);

		assertEquals(0, contar("SELECT COUNT(*) FROM Planta WHERE ID = " + idPlanta));
	}

	// Borrar una compra arrastra sus items
	@Test
	public void borrarUnaCompraArrastraSusItems() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		Producto producto = new Producto("OG Kush", 10, 1000);
		em.persist(usuario);
		em.persist(producto);
		Compra compra = new Compra(HOY, false, usuario);
		compra.addItem(new ItemCompra(producto, 2));
		em.persistAndFlush(compra);
		int idCompra = compra.getId();
		em.clear();

		ejecutar("DELETE FROM Compra WHERE ID = " + idCompra);

		assertEquals(0, contar("SELECT COUNT(*) FROM ItemCompra WHERE ID_COMPRA = " + idCompra));
	}

	// Borrar un empleado arrastra sus asignaciones a indoors
	@Test
	public void borrarUnEmpleadoArrastraSusAsignaciones() {
		Indoor indoor = new Indoor();
		EmpleadoIndoor empleado = nuevoEmpleado("asignado@test.com");
		em.persist(indoor);
		em.persist(empleado);
		empleado.addIndoor(indoor);
		em.flush();
		int idEmpleado = empleado.getId();
		em.clear();

		ejecutar("DELETE FROM EmpleadoIndoor WHERE ID = " + idEmpleado);

		assertEquals(0, contar("SELECT COUNT(*) FROM Trabaja WHERE ID_EMPLEADO_INDOOR = " + idEmpleado));
	}

	private void ejecutar(String sql) {
		em.getEntityManager().createNativeQuery(sql).executeUpdate();
	}

	private int contar(String sql) {
		return ((Number) em.getEntityManager().createNativeQuery(sql).getSingleResult()).intValue();
	}

	private Usuario nuevoUsuario(String email) {
		return new Usuario("Andres", "Negro", email, "12345");
	}

	private EmpleadoIndoor nuevoEmpleado(String email) {
		return new EmpleadoIndoor("Andres", "Negro", email, "12345", 500000);
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, HOY.minusDays(80), HOY.minusDays(90), 60, 120, 30);
	}
}
