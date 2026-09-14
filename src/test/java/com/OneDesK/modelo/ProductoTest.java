package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.OneDesK.excepciones.StockInsuficienteException;

public class ProductoTest {

	private Producto kush;

	@BeforeEach
	public void setUp() {
		kush = new Producto("OG Kush", 10, 1000);
	}

	@Test
	public void descontarStockLoBaja() {
		kush.descontarStock(3);

		assertEquals(7, kush.getStock());
	}

	@Test
	public void sePuedeDescontarTodoElStock() {
		kush.descontarStock(10);

		assertEquals(0, kush.getStock());
	}

	@Test
	public void noSePuedeDescontarMasDelStockDisponible() {
		assertThrows(StockInsuficienteException.class, () -> kush.descontarStock(11));

		assertEquals(10, kush.getStock());
	}

	@Test
	public void reponerStockLoSuma() {
		kush.descontarStock(4);

		kush.reponerStock(4);

		assertEquals(10, kush.getStock());
	}
}
