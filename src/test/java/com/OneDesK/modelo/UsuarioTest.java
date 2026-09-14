package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.OneDesK.excepciones.OperacionInvalidaException;

public class UsuarioTest {

	// --- datos personales ---

	@Test
	public void crearUnUsuarioValido() {
		Usuario usuario = nuevoUsuario("andres@test.com");

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
		Usuario usuario = nuevoUsuario("andres@test.com");

		assertThrows(IllegalArgumentException.class, () -> usuario.setEmail("sin-arroba"));

		assertEquals("andres@test.com", usuario.getEmail());
	}

	// --- tope de credito ---

	@Test
	public void elTopeDeCreditoNoPuedeSerNegativo() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		assertThrows(IllegalArgumentException.class, () -> usuario.setTopeCredito(-1));

		assertEquals(0, usuario.getTopeCredito());
	}

	@Test
	public void sePuedeBajarElTopePorDebajoDeLoQueDebe() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		usuario.agregarCompra(compraDe(usuario, false, 3));
		assertEquals(3000, usuario.getDeuda().getMonto());

		usuario.setTopeCredito(1000);

		assertEquals(1000, usuario.getTopeCredito());
	}

	// --- compras y deuda ---

	@Test
	public void unaCompraImpagaSumaALaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		usuario.agregarCompra(compraDe(usuario, false, 3));

		assertEquals(3000, usuario.getDeuda().getMonto());
		assertEquals(1, usuario.comprasImpagas().size());
	}

	@Test
	public void unaCompraPagadaNoSumaALaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		usuario.agregarCompra(compraDe(usuario, true, 3));

		assertEquals(0, usuario.getDeuda().getMonto());
		assertEquals(0, usuario.comprasImpagas().size());
	}

	@Test
	public void pagarUnaCompraLaSacaDeLaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Compra compra = compraDe(usuario, false, 3);
		usuario.agregarCompra(compra);

		usuario.registrarPago(compra);

		assertTrue(compra.isPagado());
		assertEquals(0, usuario.getDeuda().getMonto());
		assertEquals(0, usuario.comprasImpagas().size());
	}

	@Test
	public void noSePuedeAgregarLaCompraDeOtroUsuario() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Usuario otro = nuevoUsuario("otro@test.com");

		assertThrows(OperacionInvalidaException.class, () -> usuario.agregarCompra(compraDe(otro, false, 3)));

		assertEquals(0, usuario.getDeuda().getMonto());
	}

	@Test
	public void noSePuedeAgregarDosVecesLaMismaCompra() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Compra compra = compraDe(usuario, false, 3);
		usuario.agregarCompra(compra);

		assertThrows(OperacionInvalidaException.class, () -> usuario.agregarCompra(compra));

		assertEquals(3000, usuario.getDeuda().getMonto());
	}

	@Test
	public void noSePuedePagarUnaCompraAjena() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Usuario otro = nuevoUsuario("otro@test.com");
		Compra compraDelOtro = compraDe(otro, false, 3);
		otro.agregarCompra(compraDelOtro);

		assertThrows(OperacionInvalidaException.class, () -> usuario.registrarPago(compraDelOtro));

		assertFalse(compraDelOtro.isPagado());
	}

	@Test
	public void borrarUnaCompraRecalculaLaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Compra compra = compraDe(usuario, false, 3);
		usuario.agregarCompra(compra);

		usuario.deleteCompra(compra);

		assertEquals(0, usuario.getCompras().size());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	@Test
	public void lasComprasNoSeModificanDesdeAfuera() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Compra compra = compraDe(usuario, false, 3);

		assertThrows(UnsupportedOperationException.class, () -> usuario.getCompras().add(compra));

		assertEquals(0, usuario.getDeuda().getMonto());
	}

	private Usuario nuevoUsuario(String email) {
		return new Usuario("Andres", "Negro", email, "12345");
	}

	private Compra compraDe(Usuario usuario, boolean pagada, int cantidad) {
		Compra compra = new Compra(LocalDate.now(), pagada, usuario);
		compra.addItem(new ItemCompra(new Producto("OG Kush", 10, 1000), cantidad));
		return compra;
	}
}
