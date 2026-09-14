package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.OneDesK.excepciones.OperacionInvalidaException;

public class CompraTest {

	private Usuario usuario;
	private Producto kush;
	private Producto haze;
	private Compra compra;

	@BeforeEach
	public void setUp() {
		usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		kush = new Producto("OG Kush", 10, 1000);
		haze = new Producto("Amnesia", 10, 500);
		compra = new Compra(LocalDate.now(), false, usuario);
	}

	@Test
	public void unaCompraNuevaNoValeNada() {
		assertEquals(0, compra.getPrecio());
		assertEquals(0, compra.getItems().size());
	}

	@Test
	public void elTotalEsLaSumaDeLosItems() {
		compra.addItem(new ItemCompra(kush, 2));
		compra.addItem(new ItemCompra(haze, 3));

		assertEquals(3500, compra.getPrecio());
	}

	@Test
	public void elItemConservaElPrecioAunqueCambieElDelProducto() {
		ItemCompra item = new ItemCompra(kush, 2);
		compra.addItem(item);

		kush.setPrecio(5000);

		assertEquals(1000, item.getPrecioUnitario());
		assertEquals(2000, item.getPrecio());
		assertEquals(2000, compra.getPrecio());
	}

	@Test
	public void laCantidadDeUnItemTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class, () -> new ItemCompra(kush, 0));
		assertThrows(IllegalArgumentException.class, () -> new ItemCompra(kush, -1));
	}

	@Test
	public void marcarComoPagadaLaDejaPagada() {
		compra.marcarComoPagada();

		assertTrue(compra.isPagado());
	}

	@Test
	public void noSePuedeMarcarDosVecesComoPagada() {
		compra.marcarComoPagada();

		assertThrows(OperacionInvalidaException.class, compra::marcarComoPagada);
	}
}
