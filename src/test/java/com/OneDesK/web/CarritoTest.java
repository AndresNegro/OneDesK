package com.OneDesK.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.OneDesK.services.LineaCompra;

// El carrito de la sesion: junta cantidades por producto antes de convertirse en una compra
public class CarritoTest {

	private Carrito carrito;

	@BeforeEach
	public void setUp() {
		carrito = new Carrito();
	}

	// Un carrito nuevo esta vacio y no tiene unidades
	@Test
	public void unCarritoNuevoEstaVacio() {
		assertTrue(carrito.estaVacio());
		assertEquals(0, carrito.unidades());
	}

	// Agregar dos veces el mismo producto suma en una sola linea
	@Test
	public void agregarElMismoProductoSumaEnUnaLinea() {
		carrito.agregar(7, 2);
		carrito.agregar(7, 3);

		assertEquals(1, carrito.getLineas().size());
		assertEquals(5, carrito.getLineas().get(0).getCantidad());
		assertEquals(5, carrito.unidades());
	}

	// Cambiar la cantidad la reemplaza, no la suma
	@Test
	public void cambiarLaCantidadLaReemplaza() {
		carrito.agregar(7, 2);

		carrito.cambiarCantidad(7, 6);

		assertEquals(6, carrito.getLineas().get(0).getCantidad());
	}

	// Una cantidad 0 o negativa se rechaza al agregar y al cambiar
	@Test
	public void lasCantidadesTienenQueSerPositivas() {
		carrito.agregar(7, 2);

		assertThrows(IllegalArgumentException.class, () -> carrito.agregar(8, 0));
		assertThrows(IllegalArgumentException.class, () -> carrito.cambiarCantidad(7, -1));
		assertEquals(2, carrito.unidades());
	}

	// Quitar saca solo ese producto, y vaciar saca todo
	@Test
	public void quitarYVaciar() {
		carrito.agregar(7, 2);
		carrito.agregar(8, 1);

		carrito.quitar(7);
		assertEquals(1, carrito.getLineas().size());
		assertEquals(8, carrito.getLineas().get(0).getProductoId());

		carrito.vaciar();
		assertTrue(carrito.estaVacio());
	}

	// Las lineas se convierten en las LineaCompra que pide el service, una por producto
	@Test
	public void seConvierteEnLineasDeCompra() {
		carrito.agregar(7, 2);
		carrito.agregar(8, 1);

		List<LineaCompra> lineas = carrito.comoLineasDeCompra();

		assertEquals(List.of(new LineaCompra(7, 2), new LineaCompra(8, 1)), lineas);
	}
}
