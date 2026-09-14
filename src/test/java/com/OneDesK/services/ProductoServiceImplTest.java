package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Producto;

@DataJpaTest
@Import(ProductoServiceImpl.class)
public class ProductoServiceImplTest {

	@Autowired
	private ProductoService service;
	@Autowired
	private TestEntityManager em;

	// --- crearProducto ---

	@Test
	public void unProductoNuevoArrancaSinStock() {
		Producto producto = service.crearProducto("OG Kush", 1000);

		assertTrue(producto.getId() > 0);
		assertEquals(0, producto.getStock());
		assertEquals(1000, producto.getPrecio());
	}

	@Test
	public void noSePuedeRepetirLaGeneticaAunqueCambienMayusculasOEspacios() {
		service.crearProducto("OG Kush", 1000);

		assertThrows(OperacionInvalidaException.class, () -> service.crearProducto("  og kush ", 900));
	}

	@Test
	public void elPrecioTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class, () -> service.crearProducto("OG Kush", 0));
	}

	@Test
	public void laGeneticaNoPuedeEstarVacia() {
		assertThrows(IllegalArgumentException.class, () -> service.crearProducto("   ", 1000));
	}

	// --- cambiarPrecio ---

	@Test
	public void cambiarElPrecioLoActualiza() {
		Producto producto = service.crearProducto("OG Kush", 1000);

		service.cambiarPrecio(producto.getId(), 1500);

		assertEquals(1500, producto.getPrecio());
	}

	@Test
	public void elPrecioNuevoTambienTieneQueSerMayorACero() {
		Producto producto = service.crearProducto("OG Kush", 1000);

		assertThrows(IllegalArgumentException.class, () -> service.cambiarPrecio(producto.getId(), 0));

		assertEquals(1000, producto.getPrecio());
	}

	@Test
	public void cambiarElPrecioDeUnProductoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarPrecio(9999, 1500));
	}

	// --- catalogo ---

	@Test
	public void elCatalogoSoloMuestraProductosConStock() {
		guardar("OG Kush", 10, 1000);
		guardar("Amnesia", 0, 800);
		guardar("Haze", 5, 1200);

		assertEquals(List.of("Haze", "OG Kush"), geneticas(service.listarProductos()));
	}

	@Test
	public void buscarPorGeneticaEncuentraPartesSinDistinguirMayusculas() {
		guardar("OG Kush", 10, 1000);
		guardar("Kush Mints", 3, 1500);
		guardar("Purple Kush", 0, 900);
		guardar("Amnesia", 8, 800);

		assertEquals(List.of("Kush Mints", "OG Kush"), geneticas(service.buscarPorGenetica("kUsH")));
	}

	@Test
	public void buscarSinTextoDevuelveTodoElCatalogo() {
		guardar("OG Kush", 10, 1000);
		guardar("Amnesia", 0, 800);

		assertEquals(List.of("OG Kush"), geneticas(service.buscarPorGenetica("   ")));
	}

	@Test
	public void listarPorPrecioOrdenaDelMasBaratoAlMasCaro() {
		guardar("OG Kush", 1, 3000);
		guardar("Amnesia", 2, 1000);
		guardar("Haze", 0, 500);

		assertEquals(List.of("Amnesia", "OG Kush"), geneticas(service.listarPorPrecio()));
	}

	private void guardar(String genetica, int stock, int precio) {
		em.persist(new Producto(genetica, stock, precio));
		em.flush();
	}

	private List<String> geneticas(List<Producto> productos) {
		List<String> nombres = new ArrayList<>();
		for (Producto producto : productos) {
			nombres.add(producto.getGenetica());
		}
		return nombres;
	}
}
