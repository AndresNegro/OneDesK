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

	// Descontar stock resta la cantidad pedida
	@Test
	public void descontarStockLoBaja() {
		kush.descontarStock(3);

		assertEquals(7, kush.getStock());
	}

	// Se puede descontar exactamente todo el stock disponible, que queda en 0
	@Test
	public void sePuedeDescontarTodoElStock() {
		kush.descontarStock(10);

		assertEquals(0, kush.getStock());
	}

	// Descontar mas de lo que hay se rechaza y el stock queda intacto
	@Test
	public void noSePuedeDescontarMasDelStockDisponible() {
		assertThrows(StockInsuficienteException.class, () -> kush.descontarStock(11));

		assertEquals(10, kush.getStock());
	}

	// Reponer stock suma la cantidad devuelta
	@Test
	public void reponerStockLoSuma() {
		kush.descontarStock(4);

		kush.reponerStock(4);

		assertEquals(10, kush.getStock());
	}

	// Descontar 0 o una cantidad negativa se rechaza y el stock queda intacto
	@Test
	public void noSeDescuentanCantidadesCeroONegativas() {
		assertThrows(IllegalArgumentException.class, () -> kush.descontarStock(0));
		assertThrows(IllegalArgumentException.class, () -> kush.descontarStock(-3));

		assertEquals(10, kush.getStock());
	}

	// Reponer 0 o una cantidad negativa se rechaza, asi reponer -50 no puede dejar el stock negativo
	@Test
	public void noSeReponenCantidadesCeroONegativas() {
		assertThrows(IllegalArgumentException.class, () -> kush.reponerStock(0));
		assertThrows(IllegalArgumentException.class, () -> kush.reponerStock(-50));

		assertEquals(10, kush.getStock());
	}
}
