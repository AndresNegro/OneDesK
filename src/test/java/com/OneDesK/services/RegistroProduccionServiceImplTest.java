package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.ProductoNoEncontradoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.RegistroProduccion;

// Contra H2 con repositorios reales: la cosecha cruza indoor, planta, empleado y producto
@DataJpaTest
@Import({ RegistroProduccionServiceImpl.class, ProductoServiceImpl.class })
public class RegistroProduccionServiceImplTest {

	@Autowired
	private RegistroProduccionService service;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private TestEntityManager em;

	private Indoor indoor;
	private Planta plantaKush;
	private EmpleadoIndoor empleado;
	private Producto kush;

	@BeforeEach
	public void setUp() {
		indoor = new Indoor();
		plantaKush = nuevaPlanta("OG Kush");
		indoor.addPlanta(plantaKush);
		em.persist(indoor);

		empleado = nuevoEmpleado("empleado@test.com");
		empleado.addIndoor(indoor);
		em.persist(empleado);

		kush = new Producto("OG Kush", 0, 1000);
		em.persist(kush);
		em.flush();
	}

	// --- camino feliz ---

	@Test
	public void cosecharSumaElStockYMarcaLaPlanta() {
		RegistroProduccion registro = cosecharKush(50);

		assertEquals(50, kush.getStock());
		assertTrue(plantaKush.isCosechada());
		assertEquals(LocalDate.now(), plantaKush.getFechaCosecha());
		assertEquals(LocalDate.now(), registro.getFechaRegistro());
		assertSame(indoor, registro.getIndoor());
	}

	@Test
	public void elRegistroQuedaGuardadoEnLaBase() {
		int idRegistro = cosecharKush(50).getId();
		em.flush();
		em.clear();

		RegistroProduccion recargado = em.find(RegistroProduccion.class, idRegistro);

		assertEquals(50, recargado.getCantidad());
		assertEquals(plantaKush.getId(), recargado.getPlanta().getId());
		assertEquals(empleado.getId(), recargado.getEmpleadoIndoor().getId());
		assertEquals(50, recargado.getProducto().getStock());
	}

	@Test
	public void laGeneticaDeLaPlantaSeBuscaSinImportarMayusculas() {
		Planta otra = plantarEnElIndoor("og KUSH");

		service.registrarCosecha(empleado.getId(), indoor.getId(), otra.getId(), 20);

		assertEquals(20, kush.getStock());
	}

	// --- genetica sin producto en el catalogo ---

	@Test
	public void cosecharUnaGeneticaSinProductoSeRechazaIndicandoCual() {
		Planta amnesia = plantarEnElIndoor("Amnesia");

		ProductoNoEncontradoException error = assertThrows(ProductoNoEncontradoException.class,
				() -> service.registrarCosecha(empleado.getId(), indoor.getId(), amnesia.getId(), 30));

		assertEquals("Amnesia", error.getGenetica());
		assertFalse(amnesia.isCosechada());
	}

	@Test
	public void despuesDeDarDeAltaElProductoLaCosechaFunciona() {
		Planta amnesia = plantarEnElIndoor("Amnesia");
		ProductoNoEncontradoException error = assertThrows(ProductoNoEncontradoException.class,
				() -> service.registrarCosecha(empleado.getId(), indoor.getId(), amnesia.getId(), 30));

		Producto creado = productoService.crearProducto(error.getGenetica(), 800);
		service.registrarCosecha(empleado.getId(), indoor.getId(), amnesia.getId(), 30);

		assertEquals(30, creado.getStock());
	}

	// --- reglas ---

	@Test
	public void unaPlantaNoSePuedeCosecharDosVeces() {
		cosecharKush(50);

		assertThrows(OperacionInvalidaException.class, () -> cosecharKush(10));

		assertEquals(50, kush.getStock());
	}

	@Test
	public void unEmpleadoNoAsignadoAlIndoorNoPuedeCosechar() {
		EmpleadoIndoor otro = nuevoEmpleado("otro@test.com");
		em.persistAndFlush(otro);

		assertThrows(OperacionInvalidaException.class,
				() -> service.registrarCosecha(otro.getId(), indoor.getId(), plantaKush.getId(), 50));

		assertEquals(0, kush.getStock());
		assertFalse(plantaKush.isCosechada());
	}

	@Test
	public void laPlantaTieneQueSerDelIndoorIndicado() {
		Indoor otroIndoor = new Indoor();
		em.persistAndFlush(otroIndoor);
		empleado.addIndoor(otroIndoor);

		assertThrows(RecursoNoEncontradoException.class,
				() -> service.registrarCosecha(empleado.getId(), otroIndoor.getId(), plantaKush.getId(), 50));
	}

	@Test
	public void laCantidadTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class, () -> cosecharKush(0));

		assertEquals(0, kush.getStock());
		assertFalse(plantaKush.isCosechada());
	}

	@Test
	public void unEmpleadoInexistenteNoPuedeCosechar() {
		assertThrows(RecursoNoEncontradoException.class,
				() -> service.registrarCosecha(9999, indoor.getId(), plantaKush.getId(), 50));
	}

	@Test
	public void noSePuedeCosecharEnUnIndoorInexistente() {
		assertThrows(RecursoNoEncontradoException.class,
				() -> service.registrarCosecha(empleado.getId(), 9999, plantaKush.getId(), 50));
	}

	@Test
	public void unaPlantaCosechadaNoSePuedeQuitar() {
		cosecharKush(50);

		assertThrows(OperacionInvalidaException.class, () -> indoor.deletePlanta(plantaKush));

		assertTrue(indoor.getPlantas().contains(plantaKush));
	}

	// --- helpers ---

	private RegistroProduccion cosecharKush(int cantidad) {
		return service.registrarCosecha(empleado.getId(), indoor.getId(), plantaKush.getId(), cantidad);
	}

	private Planta plantarEnElIndoor(String genetica) {
		Planta planta = nuevaPlanta(genetica);
		indoor.addPlanta(planta);
		em.flush();
		return planta;
	}

	private Planta nuevaPlanta(String genetica) {
		// germino hace 90 dias y se planto hace 80
		return new Planta(genetica, LocalDate.now().minusDays(80), LocalDate.now().minusDays(90), 60, 60, 60);
	}

	private EmpleadoIndoor nuevoEmpleado(String email) {
		return new EmpleadoIndoor("Andres", "Negro", email, "12345", 500000);
	}
}
