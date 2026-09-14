package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class UsuarioTest {

	@Test
	public void crearUnUsuarioValido() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");

		assertEquals("Andres", usuario.getNombre());
		assertEquals("andres@test.com", usuario.getEmail());
		assertEquals(0, usuario.getTopeCredito());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	@Test
	public void elEmailSeGuardaEnMinusculasYSinEspacios() {
		Usuario usuario = new Usuario("Andres", "Negro", "  Andres@Test.COM ", "12345");

		assertEquals("andres@test.com", usuario.getEmail());
	}

	@Test
	public void rechazaUnEmailInvalido() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("Andres", "Negro", "@", "12345"));
	}

	@Test
	public void rechazaUnaContraseniaCorta() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("Andres", "Negro", "andres@test.com", "1234"));
	}

	@Test
	public void rechazaUnNombreVacio() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("  ", "Negro", "andres@test.com", "12345"));
	}

	@Test
	public void rechazaUnApellidoVacio() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("Andres", null, "andres@test.com", "12345"));
	}

	@Test
	public void cambiarElEmailTambienSeValida() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");

		assertThrows(IllegalArgumentException.class, () -> usuario.setEmail("sin-arroba"));

		assertEquals("andres@test.com", usuario.getEmail());
	}

	@Test
	public void elTopeDeCreditoNoPuedeSerNegativo() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");

		assertThrows(IllegalArgumentException.class, () -> usuario.setTopeCredito(-1));

		assertEquals(0, usuario.getTopeCredito());
	}

	@Test
	public void sePuedeBajarElTopePorDebajoDeLoQueDebe() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		Compra compra = new Compra(LocalDate.now(), false, usuario);
		compra.addItem(new ItemCompra(new Producto("OG Kush", 10, 1000), 3));
		usuario.agregarCompra(compra);
		assertEquals(3000, usuario.getDeuda().getMonto());

		usuario.setTopeCredito(1000);

		assertEquals(1000, usuario.getTopeCredito());
	}
}
