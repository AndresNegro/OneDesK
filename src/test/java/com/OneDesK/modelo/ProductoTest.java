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

	// --- ajustes del administrador ---

	// Fijar el stock lo deja en el valor contado, sea mayor o menor que el anterior, incluso en 0
	@Test
	public void fijarStockLoDejaEnElValorContado() {
		kush.fijarStock(25);
		assertEquals(25, kush.getStock());

		kush.fijarStock(0);
		assertEquals(0, kush.getStock());
	}

	// Fijar un stock negativo se rechaza y el stock queda intacto
	@Test
	public void noSePuedeFijarUnStockNegativo() {
		assertThrows(IllegalArgumentException.class, () -> kush.fijarStock(-1));

		assertEquals(10, kush.getStock());
	}

	// Un ajuste positivo suma y uno negativo resta
	@Test
	public void ajustarStockSumaORestaSegunElSigno() {
		kush.ajustarStock(5);
		assertEquals(15, kush.getStock());

		kush.ajustarStock(-3);
		assertEquals(12, kush.getStock());
	}

	// Un ajuste negativo puede dejar el stock justo en 0
	@Test
	public void ajustarStockPuedeDejarloEnCero() {
		kush.ajustarStock(-10);

		assertEquals(0, kush.getStock());
	}

	// Un ajuste que dejaria el stock negativo se rechaza y el stock queda intacto
	@Test
	public void unAjusteQueDejariaStockNegativoSeRechaza() {
		assertThrows(StockInsuficienteException.class, () -> kush.ajustarStock(-11));

		assertEquals(10, kush.getStock());
	}

	// Un ajuste de 0 no tiene sentido y se rechaza
	@Test
	public void unAjusteDeCeroSeRechaza() {
		assertThrows(IllegalArgumentException.class, () -> kush.ajustarStock(0));
	}
}
