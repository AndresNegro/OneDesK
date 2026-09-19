package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.excepciones.StockInsuficienteException;
import com.OneDesK.excepciones.TopeCreditoExcedidoException;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;

// Reglas de negocio de las compras contra MySQL con el service real.
// Despues de cada operacion se hace flush y clear y se vuelve a leer de la base: asi, si el service
// hubiera tocado algo antes de rechazar la operacion, ese cambio aparece y el test falla.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ CompraServiceImpl.class, LimitesDeCompraServiceImpl.class })
public class CompraServiceImplTest {

	private static final int INEXISTENTE = 999999;

	@Autowired
	private CompraService service;
	@Autowired
	private LimitesDeCompraService limites;
	@Autowired
	private TestEntityManager em;

	private int idUsuario;
	private int idKush;

	@BeforeEach
	public void setUp() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		usuario.aprobar(0);
		Producto kush = new Producto("OG Kush", 10, 1000);
		em.persist(usuario);
		em.persist(kush);
		em.flush();
		em.clear();
		idUsuario = usuario.getId();
		idKush = kush.getId();
		// limites amplios: aca se prueban las demas reglas; los limites de gramos tienen sus propios tests
		limites.cambiar(1, 1000);
	}

	// --- realizarCompra: camino feliz ---

	// Una compra pagada descuenta el stock, calcula el total y no genera deuda
	@Test
	public void compraPagadaDescuentaStockYNoGeneraDeuda() {
		int idCompra = comprarKush(3, true).getId();
		recargar();

		Compra compra = em.find(Compra.class, idCompra);
		assertTrue(compra.isPagado());
		assertEquals(3000, compra.getPrecio());
		assertEquals(7, stockKush());
		assertEquals(0, deuda());
	}

	// Una compra impaga dentro del tope descuenta el stock y suma su total a la deuda
	@Test
	public void compraImpagaDentroDelTopeGeneraDeuda() {
		asignarTope(5000);

		int idCompra = comprarKush(3, false).getId();
		recargar();

		assertFalse(em.find(Compra.class, idCompra).isPagado());
		assertEquals(3000, deuda());
		assertEquals(7, stockKush());
	}

	// --- realizarCompra: stock ---

	// Pedir mas de lo que hay rechaza toda la compra sin descontar nada del stock
	@Test
	public void stockInsuficienteRechazaLaCompraYNoTocaElStock() {
		assertThrows(StockInsuficienteException.class, () -> comprarKush(11, true));
		recargar();

		assertEquals(10, stockKush());
		assertEquals(0, comprasGuardadas());
	}

	// Dos lineas del mismo producto se suman antes de validar el stock: 6 + 6 no entran en un stock de 10
	@Test
	public void elMismoProductoEnDosLineasSumaCantidadesParaValidarStock() {
		assertThrows(StockInsuficienteException.class, () -> service.realizarCompra(idUsuario,
				List.of(new LineaCompra(idKush, 6), new LineaCompra(idKush, 6)), true));
		recargar();

		assertEquals(10, stockKush());
	}

	// Dos lineas del mismo producto se guardan como un unico item con la cantidad total
	@Test
	public void elMismoProductoEnDosLineasGeneraUnSoloItem() {
		int idCompra = service.realizarCompra(idUsuario,
				List.of(new LineaCompra(idKush, 2), new LineaCompra(idKush, 3)), true).getId();
		recargar();

		Compra compra = em.find(Compra.class, idCompra);
		assertEquals(1, compra.getItems().size());
		assertEquals(5, compra.getItems().get(0).getCantidad());
		assertEquals(5, stockKush());
	}

	// --- realizarCompra: tope de credito ---

	// Una compra impaga que deja la deuda por encima del tope se rechaza sin tocar el stock ni la deuda
	@Test
	public void topeExcedidoRechazaLaCompra() {
		asignarTope(2000);

		assertThrows(TopeCreditoExcedidoException.class, () -> comprarKush(3, false));
		recargar();

		assertEquals(10, stockKush());
		assertEquals(0, deuda());
		assertEquals(0, comprasGuardadas());
	}

	// Una compra que deja la deuda justo en el tope se permite
	@Test
	public void topeExactoPermiteLaCompra() {
		asignarTope(3000);

		comprarKush(3, false);
		recargar();

		assertEquals(3000, deuda());
	}

	// Una compra pagada no se controla contra el tope: el usuario nace con tope 0 y aun asi puede comprar pagando
	@Test
	public void laCompraPagadaNoValidaElTope() {
		int idCompra = comprarKush(3, true).getId();
		recargar();

		assertEquals(3000, em.find(Compra.class, idCompra).getPrecio());
		assertEquals(0, deuda());
	}

	// Con el tope por debajo de lo que debe, el usuario no puede hacer compras impagas
	@Test
	public void conElTopeBajadoPorDebajoDeLaDeudaNoPuedeComprarImpago() {
		asignarTope(5000);
		comprarKush(3, false);
		asignarTope(1000);

		assertThrows(TopeCreditoExcedidoException.class, () -> comprarKush(1, false));
		recargar();

		assertEquals(3000, deuda());
		assertEquals(7, stockKush());
	}

	// Con el tope por debajo de lo que debe, el usuario todavia puede comprar pagando
	@Test
	public void conElTopeBajadoPorDebajoDeLaDeudaPuedeComprarPagando() {
		asignarTope(5000);
		comprarKush(3, false);
		asignarTope(1000);

		int idCompra = comprarKush(1, true).getId();
		recargar();

		assertTrue(em.find(Compra.class, idCompra).isPagado());
		assertEquals(6, stockKush());
	}

	// Un usuario que todavia no fue aprobado no puede comprar, aunque llegue al service
	@Test
	public void unUsuarioPendienteNoPuedeComprar() {
		Usuario pendiente = new Usuario("Ana", "Lopez", "pendiente@test.com", "12345");
		em.persist(pendiente);
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.realizarCompra(pendiente.getId(),
				List.of(new LineaCompra(idKush, 1)), true));
		recargar();

		assertEquals(10, stockKush());
	}

	// --- realizarCompra: entradas invalidas ---

	// Una compra sin lineas se rechaza
	@Test
	public void laCompraVaciaEsInvalida() {
		assertThrows(OperacionInvalidaException.class, () -> service.realizarCompra(idUsuario, List.of(), true));
	}

	// Una lista de lineas nula se rechaza sin tirar NullPointerException
	@Test
	public void laListaNulaEsInvalida() {
		assertThrows(OperacionInvalidaException.class, () -> service.realizarCompra(idUsuario, null, true));
	}

	// Una linea con cantidad 0 se rechaza y el stock no cambia
	@Test
	public void laCantidadNoPuedeSerCero() {
		assertThrows(OperacionInvalidaException.class, () -> comprarKush(0, true));
		recargar();

		assertEquals(10, stockKush());
	}

	// Una linea con cantidad negativa se rechaza y el stock no cambia
	@Test
	public void laCantidadNoPuedeSerNegativa() {
		assertThrows(OperacionInvalidaException.class, () -> comprarKush(-2, true));
		recargar();

		assertEquals(10, stockKush());
	}

	// Comprar con un usuario que no existe falla con RecursoNoEncontradoException
	@Test
	public void usuarioInexistenteRechazaLaCompra() {
		assertThrows(RecursoNoEncontradoException.class,
				() -> service.realizarCompra(INEXISTENTE, List.of(new LineaCompra(idKush, 1)), true));
	}

	// Comprar un producto que no existe falla con RecursoNoEncontradoException
	@Test
	public void productoInexistenteRechazaLaCompra() {
		assertThrows(RecursoNoEncontradoException.class,
				() -> service.realizarCompra(idUsuario, List.of(new LineaCompra(INEXISTENTE, 1)), true));
	}

	// --- precio congelado ---

	// Subir el precio del producto despues de comprar no cambia el precio guardado del item ya comprado
	@Test
	public void cambiarElPrecioDelProductoNoAlteraLosItemsYaComprados() {
		int idCompra = comprarKush(2, true).getId();
		recargar();

		em.find(Producto.class, idKush).setPrecio(5000);
		recargar();

		Compra compra = em.find(Compra.class, idCompra);
		assertEquals(5000, em.find(Producto.class, idKush).getPrecio());
		assertEquals(1000, compra.getItems().get(0).getPrecioUnitario());
		assertEquals(2000, compra.getItems().get(0).getPrecio());
		assertEquals(2000, compra.getPrecio());
	}

	// --- registrarPago ---

	// Registrar el pago de una compra impaga la deja pagada y la deuda del usuario en 0
	@Test
	public void registrarPagoSaldaLaDeuda() {
		asignarTope(5000);
		int idCompra = comprarKush(3, false).getId();
		recargar();
		assertEquals(3000, deuda());

		service.registrarPago(idCompra);
		recargar();

		assertTrue(em.find(Compra.class, idCompra).isPagado());
		assertEquals(0, deuda());
	}

	// Pagar una compra que ya esta pagada se rechaza
	@Test
	public void noSePuedePagarDosVecesLaMismaCompra() {
		int idCompra = comprarKush(3, true).getId();
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.registrarPago(idCompra));
	}

	// Pagar una compra que no existe falla con RecursoNoEncontradoException
	@Test
	public void registrarPagoDeUnaCompraInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.registrarPago(INEXISTENTE));
	}

	// --- anularCompra ---

	// Anular una compra impaga devuelve el stock, la borra de la base y deja la deuda en 0
	@Test
	public void anularCompraImpagaDevuelveElStockYBorraLaDeuda() {
		asignarTope(5000);
		int idCompra = comprarKush(3, false).getId();
		recargar();
		assertEquals(7, stockKush());

		service.anularCompra(idCompra);
		recargar();

		assertEquals(10, stockKush());
		assertEquals(0, deuda());
		assertEquals(0, comprasGuardadas());
		assertTrue(em.find(Usuario.class, idUsuario).getCompras().isEmpty());
	}

	// Una compra pagada no se puede anular: sigue guardada y el stock no se devuelve
	@Test
	public void noSePuedeAnularUnaCompraPagada() {
		int idCompra = comprarKush(3, true).getId();
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.anularCompra(idCompra));
		recargar();

		assertNotNull(em.find(Compra.class, idCompra));
		assertEquals(7, stockKush());
	}

	// Anular una compra que no existe falla con RecursoNoEncontradoException
	@Test
	public void anularUnaCompraInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.anularCompra(INEXISTENTE));
	}

	// --- fecha de pago ---

	// Una compra pagada al comprar queda guardada con la fecha de pago de hoy
	@Test
	public void unaCompraPagadaSeGuardaConSuFechaDePago() {
		int idCompra = comprarKush(1, true).getId();
		recargar();

		assertEquals(LocalDate.now(), em.find(Compra.class, idCompra).getFechaPago());
	}

	// Una compra impaga queda guardada sin fecha de pago, y al registrar el pago se guarda la de hoy
	@Test
	public void registrarElPagoGuardaLaFechaDePago() {
		asignarTope(5000);
		int idCompra = comprarKush(1, false).getId();
		recargar();
		assertNull(em.find(Compra.class, idCompra).getFechaPago());

		service.registrarPago(idCompra);
		recargar();

		assertEquals(LocalDate.now(), em.find(Compra.class, idCompra).getFechaPago());
	}

	// Pagar dos veces se rechaza y no cambia la fecha de pago guardada
	@Test
	public void pagarDosVecesNoCambiaLaFechaDePago() {
		int idCompra = comprarKush(1, true).getId();
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.registrarPago(idCompra));
		recargar();

		assertEquals(LocalDate.now(), em.find(Compra.class, idCompra).getFechaPago());
	}

	// --- compras del usuario (lo que usa Mis compras) ---

	// Las compras del usuario vienen de la mas reciente a la mas vieja, y no incluyen las de otro
	@Test
	public void comprasDeTraeSoloLasDelUsuarioDeLaMasNueva() {
		int otro = otroUsuario();
		int primera = comprarKush(1, true).getId();
		int segunda = comprarKush(1, true).getId();
		service.realizarCompra(otro, List.of(new LineaCompra(idKush, 1)), true);
		recargar();

		List<Compra> compras = service.comprasDe(idUsuario);

		assertEquals(2, compras.size());
		assertEquals(segunda, compras.get(0).getId());
		assertEquals(primera, compras.get(1).getId());
	}

	// El usuario paga su propia compra y queda guardada como pagada
	@Test
	public void registrarPagoDeSuPropiaCompra() {
		asignarTope(5000);
		int idCompra = comprarKush(2, false).getId();
		recargar();

		service.registrarPagoDe(idUsuario, idCompra);
		recargar();

		assertTrue(em.find(Compra.class, idCompra).isPagado());
		assertEquals(0, deuda());
	}

	// Otro usuario no puede pagar ni anular esa compra: se rechaza y no cambia nada
	@Test
	public void otroUsuarioNoPuedePagarNiAnularLaCompra() {
		asignarTope(5000);
		int idCompra = comprarKush(2, false).getId();
		int otro = otroUsuario();

		assertThrows(OperacionInvalidaException.class, () -> service.registrarPagoDe(otro, idCompra));
		assertThrows(OperacionInvalidaException.class, () -> service.anularCompraDe(otro, idCompra));
		recargar();

		assertFalse(em.find(Compra.class, idCompra).isPagado());
		assertEquals(8, stockKush());
		assertEquals(2000, deuda());
	}

	// El usuario anula su propia compra impaga: se borra y vuelve el stock
	@Test
	public void anularSuPropiaCompra() {
		asignarTope(5000);
		int idCompra = comprarKush(2, false).getId();
		recargar();

		service.anularCompraDe(idUsuario, idCompra);
		recargar();

		assertNull(em.find(Compra.class, idCompra));
		assertEquals(10, stockKush());
	}

	// --- aprobacion del administrador ---

	// Un pedido queda pendiente: el stock ya esta reservado, pero no hay cobro ni deuda
	@Test
	public void unPedidoQuedaPendienteConElStockReservado() {
		asignarTope(5000);
		int idCompra = pedirKush(3, false).getId();
		recargar();

		Compra compra = em.find(Compra.class, idCompra);
		assertTrue(compra.isPendiente());
		assertFalse(compra.isPagado());
		assertEquals(7, stockKush());
		assertEquals(0, deuda());
		assertTrue(idsDe(service.comprasPendientes()).contains(idCompra));
	}

	// Aprobar un pedido en cuenta corriente lo pasa a la deuda
	@Test
	public void aprobarUnPedidoEnCuentaGeneraDeuda() {
		asignarTope(5000);
		int idCompra = pedirKush(3, false).getId();
		recargar();

		service.aprobarCompra(idCompra);
		recargar();

		assertTrue(em.find(Compra.class, idCompra).isAprobada());
		assertEquals(3000, deuda());
		assertFalse(idsDe(service.comprasPendientes()).contains(idCompra));
	}

	// Aprobar un pedido que se paga lo deja pagado con fecha de hoy, sin deuda
	@Test
	public void aprobarUnPedidoQueSePagaLoCobra() {
		int idCompra = pedirKush(3, true).getId();
		recargar();

		service.aprobarCompra(idCompra);
		recargar();

		Compra compra = em.find(Compra.class, idCompra);
		assertTrue(compra.isPagado());
		assertEquals(LocalDate.now(), compra.getFechaPago());
		assertEquals(0, deuda());
	}

	// Rechazar un pedido lo deja rechazado en el historial y el stock vuelve
	@Test
	public void rechazarUnPedidoDevuelveElStock() {
		asignarTope(5000);
		int idCompra = pedirKush(4, false).getId();
		recargar();

		service.rechazarCompra(idCompra);
		recargar();

		Compra compra = em.find(Compra.class, idCompra);
		assertTrue(compra.isRechazada());
		assertEquals(10, stockKush());
		assertEquals(0, deuda());
		assertEquals(1, comprasGuardadas());
	}

	// Un pedido no se aprueba ni rechaza dos veces
	@Test
	public void unPedidoSeRespondeUnaSolaVez() {
		int aprobado = comprarKush(1, true).getId();
		int rechazado = pedirKush(1, true).getId();
		service.rechazarCompra(rechazado);
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.aprobarCompra(aprobado));
		assertThrows(OperacionInvalidaException.class, () -> service.rechazarCompra(rechazado));
		recargar();

		assertEquals(9, stockKush());
	}

	// Un pedido pendiente no se puede pagar desde Mis compras
	@Test
	public void unPedidoPendienteNoSePuedePagar() {
		asignarTope(5000);
		int idCompra = pedirKush(2, false).getId();
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> service.registrarPago(idCompra));
	}

	// El usuario puede anular su pedido pendiente y el stock vuelve; una compra rechazada no se anula
	@Test
	public void anularUnPendienteSiUnRechazadoNo() {
		int pendiente = pedirKush(2, true).getId();
		int rechazado = pedirKush(3, true).getId();
		service.rechazarCompra(rechazado);
		recargar();

		service.anularCompraDe(idUsuario, pendiente);
		assertThrows(OperacionInvalidaException.class, () -> service.anularCompraDe(idUsuario, rechazado));
		recargar();

		assertNull(em.find(Compra.class, pendiente));
		assertEquals(10, stockKush());
	}

	// El tope cuenta lo que ya pidio en cuenta corriente y espera aprobacion
	@Test
	public void elTopeCuentaLosPedidosPendientesEnCuenta() {
		asignarTope(5000);
		pedirKush(3, false);
		recargar();

		assertThrows(TopeCreditoExcedidoException.class, () -> pedirKush(3, false));
		pedirKush(3, true);
	}

	// Si el tope bajo entre el pedido y la aprobacion, aprobarlo se rechaza y sigue pendiente
	@Test
	public void alAprobarSeVuelveAMirarElTope() {
		asignarTope(5000);
		int idCompra = pedirKush(3, false).getId();
		asignarTope(1000);

		assertThrows(TopeCreditoExcedidoException.class, () -> service.aprobarCompra(idCompra));
		recargar();

		assertTrue(em.find(Compra.class, idCompra).isPendiente());
		assertEquals(0, deuda());
	}

	// Aprobar o rechazar un pedido que no existe falla con RecursoNoEncontradoException
	@Test
	public void responderUnPedidoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.aprobarCompra(INEXISTENTE));
		assertThrows(RecursoNoEncontradoException.class, () -> service.rechazarCompra(INEXISTENTE));
	}

	// --- limites de gramos ---

	// Con los limites de 5 g a 40 g, un pedido de 4 g o de 41 g se rechaza sin tocar el stock
	@Test
	public void unPedidoFueraDeLosLimitesSeRechaza() {
		limites.cambiar(5, 40);
		Producto grande = new Producto("Amnesia Haze", 100, 1000);
		em.persist(grande);
		recargar();

		assertThrows(OperacionInvalidaException.class, () -> pedirKush(4, true));
		assertThrows(OperacionInvalidaException.class,
				() -> service.realizarCompra(idUsuario, List.of(new LineaCompra(grande.getId(), 41)), true));
		recargar();

		assertEquals(10, stockKush());
		assertEquals(100, em.find(Producto.class, grande.getId()).getStock());
		assertEquals(0, comprasGuardadas());
	}

	// Justo en el minimo y justo en el maximo se aceptan
	@Test
	public void losBordesDeLosLimitesSeAceptan() {
		limites.cambiar(5, 10);

		pedirKush(5, true);
		limites.cambiar(1, 5);
		pedirKush(5, true);
		recargar();

		assertEquals(0, stockKush());
	}

	// Los gramos se suman entre geneticas: 3 g de una y 3 g de otra llegan al minimo de 5 g
	@Test
	public void losLimitesSeAplicanAlTotalDelPedido() {
		limites.cambiar(5, 40);
		Producto haze = new Producto("Amnesia Haze", 100, 1000);
		em.persist(haze);
		recargar();

		int idCompra = service.realizarCompra(idUsuario,
				List.of(new LineaCompra(idKush, 3), new LineaCompra(haze.getId(), 3)), true).getId();
		assertThrows(OperacionInvalidaException.class, () -> service.realizarCompra(idUsuario,
				List.of(new LineaCompra(idKush, 5), new LineaCompra(haze.getId(), 36)), true));
		recargar();

		assertEquals(6, em.find(Compra.class, idCompra).getGramos());
	}

	// --- helpers ---

	private List<Integer> idsDe(List<Compra> compras) {
		List<Integer> ids = new ArrayList<>();
		for (Compra compra : compras) {
			ids.add(compra.getId());
		}
		return ids;
	}

	private int otroUsuario() {
		Usuario otro = new Usuario("Ana", "Lopez", "otra@test.com", "12345");
		otro.aprobar(0);
		em.persist(otro);
		recargar();
		return otro.getId();
	}

	// el circuito completo: el usuario la pide y el administrador la aprueba
	private Compra comprarKush(int cantidad, boolean pagado) {
		return service.aprobarCompra(pedirKush(cantidad, pagado).getId());
	}

	// solo el pedido: queda pendiente de aprobacion
	private Compra pedirKush(int cantidad, boolean pagado) {
		return service.realizarCompra(idUsuario, List.of(new LineaCompra(idKush, cantidad)), pagado);
	}

	private void asignarTope(int tope) {
		em.find(Usuario.class, idUsuario).setTopeCredito(tope);
		recargar();
	}

	// manda a la base lo pendiente y vacia la memoria, para que la siguiente lectura venga de MySQL
	private void recargar() {
		em.flush();
		em.clear();
	}

	private int stockKush() {
		return em.find(Producto.class, idKush).getStock();
	}

	private int deuda() {
		return em.find(Usuario.class, idUsuario).getDeuda().getMonto();
	}

	// solo las compras del usuario del test: la base puede tener otras cargadas
	private long comprasGuardadas() {
		return em.getEntityManager()
				.createQuery("select count(c) from Compra c where c.usuario.id = :id", Long.class)
				.setParameter("id", idUsuario).getSingleResult();
	}
}
