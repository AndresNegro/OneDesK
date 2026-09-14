package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	public void borrarUnItemRestaLoQueEseItemAporto() {
		compra.addItem(new ItemCompra(kush, 2));
		ItemCompra itemHaze = new ItemCompra(haze, 3);
		compra.addItem(itemHaze);

		compra.deleteItem(itemHaze);

		assertEquals(2000, compra.getPrecio());
		assertEquals(1, compra.getItems().size());
	}

	@Test
	public void cambiarElPrecioEntreAgregarYBorrarNoRompeElTotal() {
		ItemCompra itemKush = new ItemCompra(kush, 2);
		compra.addItem(itemKush);
		compra.addItem(new ItemCompra(haze, 1));
		assertEquals(2500, compra.getPrecio());

		// el catalogo sube de precio despues de hecha la compra
		kush.setPrecio(5000);
		compra.deleteItem(itemKush);

		// se resta lo que se habia sumado (2000), no lo que valdria hoy (10000)
		assertEquals(500, compra.getPrecio());
	}

	@Test
	public void borrarUnItemQueNoEstaNoCambiaElTotal() {
		compra.addItem(new ItemCompra(kush, 2));

		compra.deleteItem(new ItemCompra(haze, 3));

		assertEquals(2000, compra.getPrecio());
		assertEquals(1, compra.getItems().size());
	}
}
