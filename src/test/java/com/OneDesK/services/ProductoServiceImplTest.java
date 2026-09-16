package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Producto;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductoServiceImpl.class)
public class ProductoServiceImplTest {

	@Autowired
	private ProductoService service;
	@Autowired
	private TestEntityManager em;

	// --- crearProducto ---

	// Un producto dado de alta se guarda con su precio y con stock 0
	@Test
	public void unProductoNuevoArrancaSinStock() {
		Producto producto = service.crearProducto("OG Kush", 1000);

		assertTrue(producto.getId() > 0);
		assertEquals(0, producto.getStock());
		assertEquals(1000, producto.getPrecio());
	}

	// No se puede crear un producto con una genetica que ya existe, aunque cambien mayusculas o espacios
	@Test
	public void noSePuedeRepetirLaGeneticaAunqueCambienMayusculasOEspacios() {
		service.crearProducto("OG Kush", 1000);

		assertThrows(OperacionInvalidaException.class, () -> service.crearProducto("  og kush ", 900));
	}

	// No se puede crear un producto con precio 0
	@Test
	public void elPrecioTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class, () -> service.crearProducto("OG Kush", 0));
	}

	// No se puede crear un producto con la genetica vacia
	@Test
	public void laGeneticaNoPuedeEstarVacia() {
		assertThrows(IllegalArgumentException.class, () -> service.crearProducto("   ", 1000));
	}

	// --- cambiarPrecio ---

	// Cambiar el precio de un producto existente lo actualiza
	@Test
	public void cambiarElPrecioLoActualiza() {
		Producto producto = service.crearProducto("OG Kush", 1000);

		service.cambiarPrecio(producto.getId(), 1500);

		assertEquals(1500, producto.getPrecio());
	}

	// Un precio nuevo de 0 se rechaza y el producto conserva el precio que tenia
	@Test
	public void elPrecioNuevoTambienTieneQueSerMayorACero() {
		Producto producto = service.crearProducto("OG Kush", 1000);

		assertThrows(IllegalArgumentException.class, () -> service.cambiarPrecio(producto.getId(), 0));

		assertEquals(1000, producto.getPrecio());
	}

	// Cambiar el precio de un producto que no existe falla con RecursoNoEncontradoException
	@Test
	public void cambiarElPrecioDeUnProductoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarPrecio(9999, 1500));
	}

	// --- catalogo ---

	// El catalogo deja afuera los productos sin stock y ordena el resto por genetica
	@Test
	public void elCatalogoSoloMuestraProductosConStock() {
		guardar("OG Kush", 10, 1000);
		guardar("Amnesia", 0, 800);
		guardar("Haze", 5, 1200);

		assertEquals(List.of("Haze", "OG Kush"), geneticas(service.listarProductos()));
	}

	// La busqueda encuentra una parte de la genetica sin importar mayusculas y deja afuera lo que no tiene stock
	@Test
	public void buscarPorGeneticaEncuentraPartesSinDistinguirMayusculas() {
		guardar("OG Kush", 10, 1000);
		guardar("Kush Mints", 3, 1500);
		guardar("Purple Kush", 0, 900);
		guardar("Amnesia", 8, 800);

		assertEquals(List.of("Kush Mints", "OG Kush"), geneticas(service.buscarPorGenetica("kUsH")));
	}

	// Buscar con texto vacio devuelve todo el catalogo con stock
	@Test
	public void buscarSinTextoDevuelveTodoElCatalogo() {
		guardar("OG Kush", 10, 1000);
		guardar("Amnesia", 0, 800);

		assertEquals(List.of("OG Kush"), geneticas(service.buscarPorGenetica("   ")));
	}

	// El listado por precio va del mas barato al mas caro y deja afuera lo que no tiene stock
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
