package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.excepciones.StockInsuficienteException;
import com.OneDesK.excepciones.TopeCreditoExcedidoException;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.ItemCompra;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.CompraRepository;
import com.OneDesK.repositories.ProductoRepository;
import com.OneDesK.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class CompraServiceImplTest {

	private static final int ID_USUARIO = 1;
	private static final int ID_KUSH = 10;
	private static final int ID_COMPRA = 7;

	@Mock
	private CompraRepository compraRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private ProductoRepository productoRepository;

	@InjectMocks
	private CompraServiceImpl service;

	private Usuario usuario;
	private Producto kush;

	@BeforeEach
	public void setUp() {
		usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		kush = new Producto("OG Kush", 10, 1000);
	}

	// --- realizarCompra: camino feliz ---

	// Una compra pagada descuenta el stock, calcula el total y no genera deuda
	@Test
	public void compraPagadaDescuentaStockYNoGeneraDeuda() {
		existeUsuario();
		existeKush();
		guardaLaCompra();

		Compra compra = service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), true);

		assertTrue(compra.isPagado());
		assertEquals(3000, compra.getPrecio());
		assertEquals(7, kush.getStock());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// Una compra impaga dentro del tope descuenta el stock y suma su total a la deuda
	@Test
	public void compraImpagaDentroDelTopeGeneraDeuda() {
		usuario.setTopeCredito(5000);
		existeUsuario();
		existeKush();
		guardaLaCompra();

		Compra compra = service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), false);

		assertFalse(compra.isPagado());
		assertEquals(3000, usuario.getDeuda().getMonto());
		assertEquals(7, kush.getStock());
	}

	// --- realizarCompra: stock ---

	// Pedir mas de lo que hay rechaza toda la compra sin descontar nada del stock
	@Test
	public void stockInsuficienteRechazaLaCompraYNoTocaElStock() {
		existeUsuario();
		existeKush();

		assertThrows(StockInsuficienteException.class,
				() -> service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 11)), true));

		assertEquals(10, kush.getStock());
	}

	// Dos lineas del mismo producto se suman antes de validar el stock: 6 + 6 no entran en un stock de 10
	@Test
	public void elMismoProductoEnDosLineasSumaCantidadesParaValidarStock() {
		existeUsuario();
		existeKush();

		// 6 y 6 pasan por separado contra un stock de 10, pero suman 12
		assertThrows(StockInsuficienteException.class, () -> service.realizarCompra(ID_USUARIO,
				List.of(new LineaCompra(ID_KUSH, 6), new LineaCompra(ID_KUSH, 6)), true));

		assertEquals(10, kush.getStock());
	}

	// Dos lineas del mismo producto se juntan en un unico item con la cantidad total
	@Test
	public void elMismoProductoEnDosLineasGeneraUnSoloItem() {
		existeUsuario();
		existeKush();
		guardaLaCompra();

		Compra compra = service.realizarCompra(ID_USUARIO,
				List.of(new LineaCompra(ID_KUSH, 2), new LineaCompra(ID_KUSH, 3)), true);

		assertEquals(1, compra.getItems().size());
		assertEquals(5, compra.getItems().get(0).getCantidad());
		assertEquals(5, kush.getStock());
	}

	// --- realizarCompra: tope de credito ---

	// Una compra impaga que deja la deuda por encima del tope se rechaza sin tocar el stock ni la deuda
	@Test
	public void topeExcedidoRechazaLaCompra() {
		usuario.setTopeCredito(2000);
		existeUsuario();
		existeKush();

		assertThrows(TopeCreditoExcedidoException.class,
				() -> service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), false));

		assertEquals(10, kush.getStock());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// Una compra que deja la deuda justo en el tope se permite
	@Test
	public void topeExactoPermiteLaCompra() {
		usuario.setTopeCredito(3000);
		existeUsuario();
		existeKush();
		guardaLaCompra();

		service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), false);

		assertEquals(3000, usuario.getDeuda().getMonto());
	}

	// Una compra pagada no se controla contra el tope, aunque el usuario tenga tope 0
	@Test
	public void laCompraPagadaNoValidaElTope() {
		// el usuario nace con tope 0 y aun asi puede comprar pagando
		existeUsuario();
		existeKush();
		guardaLaCompra();

		Compra compra = service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), true);

		assertEquals(3000, compra.getPrecio());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// Con el tope por debajo de lo que debe, el usuario no puede hacer compras impagas
	@Test
	public void conElTopeBajadoPorDebajoDeLaDeudaNoPuedeComprarImpago() {
		compraImpaga();
		usuario.setTopeCredito(1000);

		assertThrows(TopeCreditoExcedidoException.class,
				() -> service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 1)), false));
	}

	// Con el tope por debajo de lo que debe, el usuario todavia puede comprar pagando
	@Test
	public void conElTopeBajadoPorDebajoDeLaDeudaPuedeComprarPagando() {
		compraImpaga();
		usuario.setTopeCredito(1000);

		Compra compra = service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 1)), true);

		assertTrue(compra.isPagado());
	}

	// --- realizarCompra: entradas invalidas ---

	// Una compra sin lineas se rechaza
	@Test
	public void laCompraVaciaEsInvalida() {
		assertThrows(OperacionInvalidaException.class, () -> service.realizarCompra(ID_USUARIO, List.of(), true));
	}

	// Una lista de lineas nula se rechaza sin tirar NullPointerException
	@Test
	public void laListaNulaEsInvalida() {
		assertThrows(OperacionInvalidaException.class, () -> service.realizarCompra(ID_USUARIO, null, true));
	}

	// Una linea con cantidad 0 se rechaza
	@Test
	public void laCantidadNoPuedeSerCero() {
		existeUsuario();

		assertThrows(OperacionInvalidaException.class,
				() -> service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 0)), true));
	}

	// Una linea con cantidad negativa se rechaza
	@Test
	public void laCantidadNoPuedeSerNegativa() {
		existeUsuario();

		assertThrows(OperacionInvalidaException.class,
				() -> service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, -2)), true));
	}

	// Comprar con un usuario que no existe falla con RecursoNoEncontradoException
	@Test
	public void usuarioInexistenteRechazaLaCompra() {
		when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class,
				() -> service.realizarCompra(99, List.of(new LineaCompra(ID_KUSH, 1)), true));
	}

	// Comprar un producto que no existe falla con RecursoNoEncontradoException
	@Test
	public void productoInexistenteRechazaLaCompra() {
		existeUsuario();
		when(productoRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class,
				() -> service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(99, 1)), true));
	}

	// --- precio congelado ---

	// Subir el precio del producto despues de comprar no cambia el precio del item ya comprado
	@Test
	public void cambiarElPrecioDelProductoNoAlteraLosItemsYaComprados() {
		existeUsuario();
		existeKush();
		guardaLaCompra();

		Compra compra = service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 2)), true);
		ItemCompra item = compra.getItems().get(0);
		assertEquals(2000, item.getPrecio());

		kush.setPrecio(5000);

		// el item sigue valiendo lo que valia al comprarlo, y sigue cuadrando con el total
		assertEquals(2000, item.getPrecio());
		assertEquals(compra.getPrecio(), item.getPrecio());
	}

	// --- registrarPago ---

	// Registrar el pago de una compra impaga deja la deuda del usuario en 0
	@Test
	public void registrarPagoSaldaLaDeuda() {
		Compra compra = compraImpaga();
		assertEquals(3000, usuario.getDeuda().getMonto());
		existeLaCompra(compra);

		service.registrarPago(ID_COMPRA);

		assertTrue(compra.isPagado());
		assertEquals(0, usuario.getDeuda().getMonto());
	}

	// Pagar una compra que ya esta pagada se rechaza
	@Test
	public void noSePuedePagarDosVecesLaMismaCompra() {
		existeLaCompra(compraPagada());

		assertThrows(OperacionInvalidaException.class, () -> service.registrarPago(ID_COMPRA));
	}

	// Pagar una compra que no existe falla con RecursoNoEncontradoException
	@Test
	public void registrarPagoDeUnaCompraInexistenteFalla() {
		when(compraRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> service.registrarPago(99));
	}

	// --- anularCompra ---

	// Anular una compra impaga devuelve el stock, la saca del usuario y borra su deuda
	@Test
	public void anularCompraImpagaDevuelveElStockYBorraLaDeuda() {
		Compra compra = compraImpaga();
		assertEquals(7, kush.getStock());
		existeLaCompra(compra);

		service.anularCompra(ID_COMPRA);

		assertEquals(10, kush.getStock());
		assertEquals(0, usuario.getDeuda().getMonto());
		assertTrue(usuario.getCompras().isEmpty());
	}

	// Una compra pagada no se puede anular y el stock no se devuelve
	@Test
	public void noSePuedeAnularUnaCompraPagada() {
		existeLaCompra(compraPagada());

		assertThrows(OperacionInvalidaException.class, () -> service.anularCompra(ID_COMPRA));

		assertEquals(7, kush.getStock());
	}

	// Anular una compra que no existe falla con RecursoNoEncontradoException
	@Test
	public void anularUnaCompraInexistenteFalla() {
		when(compraRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> service.anularCompra(99));
	}

	// --- helpers ---

	private void existeUsuario() {
		when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
	}

	private void existeKush() {
		when(productoRepository.findById(ID_KUSH)).thenReturn(Optional.of(kush));
	}

	private void guardaLaCompra() {
		when(compraRepository.save(any(Compra.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
	}

	private void existeLaCompra(Compra compra) {
		when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra));
	}

	private Compra compraImpaga() {
		usuario.setTopeCredito(5000);
		existeUsuario();
		existeKush();
		guardaLaCompra();
		return service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), false);
	}

	private Compra compraPagada() {
		existeUsuario();
		existeKush();
		guardaLaCompra();
		return service.realizarCompra(ID_USUARIO, List.of(new LineaCompra(ID_KUSH, 3)), true);
	}
}
