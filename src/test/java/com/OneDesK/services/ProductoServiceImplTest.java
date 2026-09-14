package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Producto;

@DataJpaTest
@Import(ProductoServiceImpl.class)
public class ProductoServiceImplTest {

	@Autowired
	private ProductoService service;

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
}
