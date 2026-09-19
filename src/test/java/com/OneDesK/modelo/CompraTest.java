package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	// Una compra recien creada no tiene items y su total es 0
	@Test
	public void unaCompraNuevaNoValeNada() {
		assertEquals(0, compra.getPrecio());
		assertEquals(0, compra.getItems().size());
	}

	// El total de la compra es la suma de cantidad por precio de cada item
	@Test
	public void elTotalEsLaSumaDeLosItems() {
		compra.addItem(new ItemCompra(kush, 2));
		compra.addItem(new ItemCompra(haze, 3));

		assertEquals(3500, compra.getPrecio());
	}

	// El item guarda el precio del momento de la compra: si despues sube el producto, ni el item ni la compra cambian
	@Test
	public void elItemConservaElPrecioAunqueCambieElDelProducto() {
		ItemCompra item = new ItemCompra(kush, 2);
		compra.addItem(item);

		kush.setPrecio(5000);

		assertEquals(1000, item.getPrecioUnitario());
		assertEquals(2000, item.getPrecio());
		assertEquals(2000, compra.getPrecio());
	}

	// Un item con cantidad 0 o negativa se rechaza
	@Test
	public void laCantidadDeUnItemTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class, () -> new ItemCompra(kush, 0));
		assertThrows(IllegalArgumentException.class, () -> new ItemCompra(kush, -1));
	}

	// Marcar una compra como pagada la deja pagada
	@Test
	public void marcarComoPagadaLaDejaPagada() {
		compra.marcarComoPagada();

		assertTrue(compra.isPagado());
	}

	// Marcar como pagada una compra que ya lo esta se rechaza, porque una compra pagada no vuelve atras
	@Test
	public void noSePuedeMarcarDosVecesComoPagada() {
		compra.marcarComoPagada();

		assertThrows(OperacionInvalidaException.class, compra::marcarComoPagada);
	}

	// --- fecha de pago ---

	// Una compra impaga no tiene fecha de pago
	@Test
	public void unaCompraImpagaNoTieneFechaDePago() {
		assertNull(compra.getFechaPago());
	}

	// Una compra pagada al comprar tiene como fecha de pago la misma fecha de la compra
	@Test
	public void pagarAlComprarUsaLaFechaDeLaCompra() {
		LocalDate ayer = LocalDate.now().minusDays(1);
		Compra pagada = new Compra(ayer, true, usuario);

		assertEquals(ayer, pagada.getFechaPago());
	}

	// Pagar despues una compra impaga le pone como fecha de pago el dia en que se paga
	@Test
	public void pagarDespuesUsaLaFechaDelPago() {
		Compra deAyer = new Compra(LocalDate.now().minusDays(1), false, usuario);

		deAyer.marcarComoPagada();

		assertEquals(LocalDate.now(), deAyer.getFechaPago());
	}

	// --- aprobacion del administrador ---

	// Una compra pedida queda pendiente, sin pagar y sin fecha de pago
	@Test
	public void unaCompraPedidaQuedaPendiente() {
		Compra pedida = Compra.pedida(LocalDate.now(), usuario, true);

		assertTrue(pedida.isPendiente());
		assertFalse(pedida.isPagado());
		assertNull(pedida.getFechaPago());
	}

	// Si el cliente eligio pagar, al aprobarla queda pagada con la fecha de la aprobacion
	@Test
	public void aprobarUnaCompraQuePagaLaDejaPagada() {
		Compra pedida = Compra.pedida(LocalDate.now().minusDays(2), usuario, true);

		pedida.aprobar();

		assertTrue(pedida.isAprobada());
		assertTrue(pedida.isPagado());
		assertEquals(LocalDate.now(), pedida.getFechaPago());
	}

	// Si el cliente eligio cuenta corriente, al aprobarla queda aprobada pero impaga
	@Test
	public void aprobarUnaCompraEnCuentaLaDejaImpaga() {
		Compra pedida = Compra.pedida(LocalDate.now(), usuario, false);

		pedida.aprobar();

		assertTrue(pedida.isAprobada());
		assertFalse(pedida.isPagado());
	}

	// Rechazarla la deja rechazada y sin pagar
	@Test
	public void rechazarLaDejaRechazada() {
		Compra pedida = Compra.pedida(LocalDate.now(), usuario, true);

		pedida.rechazar();

		assertTrue(pedida.isRechazada());
		assertFalse(pedida.isPagado());
	}

	// Una compra ya aprobada o rechazada no se vuelve a aprobar ni a rechazar
	@Test
	public void soloSeApruebaORechazaUnaVez() {
		Compra aprobada = Compra.pedida(LocalDate.now(), usuario, false);
		aprobada.aprobar();
		Compra rechazada = Compra.pedida(LocalDate.now(), usuario, false);
		rechazada.rechazar();

		assertThrows(OperacionInvalidaException.class, aprobada::aprobar);
		assertThrows(OperacionInvalidaException.class, aprobada::rechazar);
		assertThrows(OperacionInvalidaException.class, rechazada::aprobar);
		assertThrows(OperacionInvalidaException.class, rechazada::rechazar);
	}

	// Una compra pendiente o rechazada no se puede marcar como pagada
	@Test
	public void noSePagaUnaCompraSinAprobar() {
		Compra pendiente = Compra.pedida(LocalDate.now(), usuario, false);
		Compra rechazada = Compra.pedida(LocalDate.now(), usuario, false);
		rechazada.rechazar();

		assertThrows(OperacionInvalidaException.class, pendiente::marcarComoPagada);
		assertThrows(OperacionInvalidaException.class, rechazada::marcarComoPagada);
	}

	// Los gramos de la compra son la suma de los de todas sus lineas
	@Test
	public void losGramosSumanTodasLasLineas() {
		compra.addItem(new ItemCompra(kush, 12));
		compra.addItem(new ItemCompra(haze, 8));

		assertEquals(20, compra.getGramos());
	}
}
