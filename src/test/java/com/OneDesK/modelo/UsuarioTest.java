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

	// Un usuario recien creado guarda sus datos y arranca con tope de credito 0 y sin deuda
	@Test
	public void crearUnUsuarioValido() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		assertEquals("Andres", usuario.getNombre());
		assertEquals("andres@test.com", usuario.getEmail());
		assertEquals(0, usuario.getTopeCredito());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// El email se normaliza al guardarse, para que las mayusculas no permitan registrarlo dos veces
	@Test
	public void elEmailSeGuardaEnMinusculasYSinEspacios() {
		Usuario usuario = new Usuario("Andres", "Negro", "  Andres@Test.COM ", "12345");

		assertEquals("andres@test.com", usuario.getEmail());
	}

	// El constructor rechaza un email sin formato valido
	@Test
	public void rechazaUnEmailInvalido() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("Andres", "Negro", "@", "12345"));
	}

	// El constructor rechaza una contrasenia de menos de 5 caracteres
	@Test
	public void rechazaUnaContraseniaCorta() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("Andres", "Negro", "andres@test.com", "1234"));
	}

	// El constructor rechaza un nombre formado solo por espacios
	@Test
	public void rechazaUnNombreVacio() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("  ", "Negro", "andres@test.com", "12345"));
	}

	// El constructor rechaza un apellido nulo
	@Test
	public void rechazaUnApellidoVacio() {
		assertThrows(IllegalArgumentException.class, () -> new Usuario("Andres", null, "andres@test.com", "12345"));
	}

	// setEmail valida igual que el constructor y no pisa el email anterior si el nuevo es invalido
	@Test
	public void cambiarElEmailTambienSeValida() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		assertThrows(IllegalArgumentException.class, () -> usuario.setEmail("sin-arroba"));

		assertEquals("andres@test.com", usuario.getEmail());
	}

	// Cambiar nombre, apellido o contrasenia se valida igual que al crear, y no pisa el valor anterior
	@Test
	public void cambiarLosDatosPersonalesTambienSeValida() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		assertThrows(IllegalArgumentException.class, () -> usuario.setNombre("   "));
		assertThrows(IllegalArgumentException.class, () -> usuario.setApellido(null));
		assertThrows(IllegalArgumentException.class, () -> usuario.setContrasenia("1234"));

		assertEquals("Andres", usuario.getNombre());
		assertEquals("Negro", usuario.getApellido());
		assertEquals("12345", usuario.getContrasenia());
	}

	// --- tope de credito ---

	// Un tope de credito negativo se rechaza y el tope anterior queda intacto
	@Test
	public void elTopeDeCreditoNoPuedeSerNegativo() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		assertThrows(IllegalArgumentException.class, () -> usuario.setTopeCredito(-1));

		assertEquals(0, usuario.getTopeCredito());
	}

	// El tope se puede bajar aunque quede por debajo de la deuda actual
	@Test
	public void sePuedeBajarElTopePorDebajoDeLoQueDebe() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		usuario.agregarCompra(compraDe(usuario, false, 3));
		assertEquals(3000, usuario.getDeuda().getMonto());

		usuario.setTopeCredito(1000);

		assertEquals(1000, usuario.getTopeCredito());
	}

	// --- compras y deuda ---

	// Agregar una compra impaga suma su total a la deuda y la cuenta entre las impagas
	@Test
	public void unaCompraImpagaSumaALaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		usuario.agregarCompra(compraDe(usuario, false, 3));

		assertEquals(3000, usuario.getDeuda().getMonto());
		assertEquals(1, usuario.comprasImpagas().size());
	}

	// Una compra que nace pagada no genera deuda ni figura entre las impagas
	@Test
	public void unaCompraPagadaNoSumaALaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");

		usuario.agregarCompra(compraDe(usuario, true, 3));

		assertEquals(0, usuario.getDeuda().getMonto());
		assertEquals(0, usuario.comprasImpagas().size());
	}

	// Registrar el pago marca la compra como pagada y la descuenta de la deuda
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

	// Un usuario no acepta una compra hecha a nombre de otro usuario
	@Test
	public void noSePuedeAgregarLaCompraDeOtroUsuario() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Usuario otro = nuevoUsuario("otro@test.com");

		assertThrows(OperacionInvalidaException.class, () -> usuario.agregarCompra(compraDe(otro, false, 3)));

		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// Agregar dos veces la misma compra se rechaza, para no contar su deuda doble
	@Test
	public void noSePuedeAgregarDosVecesLaMismaCompra() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Compra compra = compraDe(usuario, false, 3);
		usuario.agregarCompra(compra);

		assertThrows(OperacionInvalidaException.class, () -> usuario.agregarCompra(compra));

		assertEquals(3000, usuario.getDeuda().getMonto());
	}

	// Un usuario no puede registrar el pago de una compra que no es suya
	@Test
	public void noSePuedePagarUnaCompraAjena() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Usuario otro = nuevoUsuario("otro@test.com");
		Compra compraDelOtro = compraDe(otro, false, 3);
		otro.agregarCompra(compraDelOtro);

		assertThrows(OperacionInvalidaException.class, () -> usuario.registrarPago(compraDelOtro));

		assertFalse(compraDelOtro.isPagado());
	}

	// Sacar una compra impaga la quita de la lista y recalcula la deuda
	@Test
	public void borrarUnaCompraRecalculaLaDeuda() {
		Usuario usuario = nuevoUsuario("andres@test.com");
		Compra compra = compraDe(usuario, false, 3);
		usuario.agregarCompra(compra);

		usuario.deleteCompra(compra);

		assertEquals(0, usuario.getCompras().size());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// La lista de compras es de solo lectura, asi nadie agrega compras salteando el recalculo de la deuda
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

	// --- aprobacion ---

	// Un usuario recien registrado queda pendiente, con tope 0
	@Test
	public void unUsuarioNuevoQuedaPendiente() {
		Usuario usuario = new Usuario("Andres", "Negro", "nuevo@test.com", "12345");

		assertFalse(usuario.isAprobado());
		assertEquals(0, usuario.getTopeCredito());
	}

	// Aprobarlo lo habilita y le asigna el tope en el mismo paso
	@Test
	public void aprobarLoHabilitaConSuTope() {
		Usuario usuario = new Usuario("Andres", "Negro", "nuevo@test.com", "12345");

		usuario.aprobar(7000);

		assertTrue(usuario.isAprobado());
		assertEquals(7000, usuario.getTopeCredito());
	}

	// Con un tope negativo no se aprueba: sigue pendiente y con el tope que tenia
	@Test
	public void conTopeNegativoSigueSinAprobar() {
		Usuario usuario = new Usuario("Andres", "Negro", "nuevo@test.com", "12345");

		assertThrows(IllegalArgumentException.class, () -> usuario.aprobar(-1));

		assertFalse(usuario.isAprobado());
		assertEquals(0, usuario.getTopeCredito());
	}

	// Un usuario ya aprobado no se vuelve a aprobar, y su tope no cambia por esa via
	@Test
	public void noSeApruebaDosVeces() {
		Usuario usuario = new Usuario("Andres", "Negro", "nuevo@test.com", "12345");
		usuario.aprobar(7000);

		assertThrows(OperacionInvalidaException.class, () -> usuario.aprobar(100));

		assertEquals(7000, usuario.getTopeCredito());
	}

	// --- compras pendientes y rechazadas ---

	// La deuda solo suma las compras aprobadas sin pagar: una pendiente o rechazada todavia no se debe
	@Test
	public void laDeudaNoCuentaPendientesNiRechazadas() {
		Usuario usuario = new Usuario("Andres", "Negro", "deudor@test.com", "12345");
		usuario.agregarCompra(compraDe(usuario, false, 3));
		Compra pendiente = pedidaDe(usuario, false, 2);
		Compra rechazada = pedidaDe(usuario, false, 4);
		usuario.agregarCompra(pendiente);
		usuario.agregarCompra(rechazada);
		rechazada.rechazar();
		usuario.recalcularDeuda();

		assertEquals(3000, usuario.getDeuda().getMonto());
		assertEquals(1, usuario.comprasImpagas().size());
	}

	// Lo comprometido suma lo que debe y lo pedido en cuenta corriente, no lo que paga al aprobar
	@Test
	public void laDeudaComprometidaSumaLoPedidoEnCuentaCorriente() {
		Usuario usuario = new Usuario("Andres", "Negro", "deudor@test.com", "12345");
		usuario.agregarCompra(compraDe(usuario, false, 3));
		usuario.agregarCompra(pedidaDe(usuario, false, 2));
		usuario.agregarCompra(pedidaDe(usuario, true, 5));

		assertEquals(5000, usuario.deudaComprometida());
	}

	// Al aprobar una pendiente en cuenta corriente, pasa a ser deuda
	@Test
	public void alAprobarlaPasaASerDeuda() {
		Usuario usuario = new Usuario("Andres", "Negro", "deudor@test.com", "12345");
		Compra pendiente = pedidaDe(usuario, false, 2);
		usuario.agregarCompra(pendiente);
		assertEquals(0, usuario.getDeuda().getMonto());

		pendiente.aprobar();
		usuario.recalcularDeuda();

		assertEquals(2000, usuario.getDeuda().getMonto());
	}

	private Compra pedidaDe(Usuario usuario, boolean pagaAlAprobar, int cantidad) {
		Compra compra = Compra.pedida(LocalDate.now(), usuario, pagaAlAprobar);
		compra.addItem(new ItemCompra(new Producto("OG Kush", 10, 1000), cantidad));
		return compra;
	}
}
