package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.evento.EventoLuz;
import com.OneDesK.excepciones.StockInsuficienteException;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;

// Recorre el sistema entero contra MySQL con los services reales, en el orden en que pasarian las cosas:
// se registran las personas, se planta, se cosecha, se vende, se cobra y se anula.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ UsuarioServiceImpl.class, EmpleadoIndoorServiceImpl.class, IndoorServiceImpl.class,
		ProductoServiceImpl.class, RegistroProduccionServiceImpl.class, CompraServiceImpl.class })
public class FlujoCompletoTest {

	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private EmpleadoIndoorService empleadoService;
	@Autowired
	private IndoorService indoorService;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private RegistroProduccionService registroService;
	@Autowired
	private CompraService compraService;
	@Autowired
	private TestEntityManager em;

	// Del alta del indoor hasta la venta: cosechar carga el stock y comprar lo descuenta
	@Test
	public void deLaCosechaALaVenta() {
		Usuario cliente = usuarioService.registrar("Ana", "Perez", "cliente@test.com", "12345");
		usuarioService.asignarTopeCredito(cliente.getId(), 20000);
		EmpleadoIndoor empleado = empleadoService.registrar("Andres", "Negro", "cultivador@test.com", "12345", 500000);

		Indoor indoor = indoorService.crearIndoor();
		empleadoService.asignarIndoor(empleado.getId(), indoor.getId());
		Planta planta = indoorService.plantar(indoor.getId(), nuevaPlanta("OG Kush"));
		Producto kush = productoService.crearProducto("OG Kush", 1000);

		registroService.registrarCosecha(empleado.getId(), indoor.getId(), planta.getId(), 50);
		assertEquals(50, kush.getStock());
		assertTrue(planta.isCosechada());

		Compra compra = compraService.realizarCompra(cliente.getId(),
				List.of(new LineaCompra(kush.getId(), 3)), false);

		assertEquals(47, kush.getStock());
		assertEquals(3000, compra.getPrecio());
		assertEquals(3000, cliente.getDeuda().getMonto());

		em.flush();
		em.clear();

		Usuario recargado = em.find(Usuario.class, cliente.getId());
		assertEquals(3000, recargado.getDeuda().getMonto());
		assertEquals(1, recargado.getCompras().size());
		assertEquals(47, em.find(Producto.class, kush.getId()).getStock());
	}

	// Pagar una compra y anular otra: la deuda vuelve a cero y el stock anulado se recupera
	@Test
	public void pagarUnaCompraYAnularOtraDejanTodoEnOrden() {
		Usuario cliente = usuarioService.registrar("Ana", "Perez", "cliente@test.com", "12345");
		usuarioService.asignarTopeCredito(cliente.getId(), 20000);
		Producto kush = productoService.crearProducto("OG Kush", 1000);
		cargarStock(kush, 50);

		Compra pagada = compraService.realizarCompra(cliente.getId(),
				List.of(new LineaCompra(kush.getId(), 3)), false);
		Compra anulada = compraService.realizarCompra(cliente.getId(),
				List.of(new LineaCompra(kush.getId(), 5)), false);
		assertEquals(8000, cliente.getDeuda().getMonto());
		assertEquals(42, kush.getStock());

		compraService.registrarPago(pagada.getId());
		compraService.anularCompra(anulada.getId());
		em.flush();
		em.clear();

		Usuario recargado = em.find(Usuario.class, cliente.getId());
		assertEquals(0, recargado.getDeuda().getMonto());
		assertEquals(1, recargado.getCompras().size());
		assertTrue(recargado.getCompras().get(0).isPagado());
		assertEquals(47, em.find(Producto.class, kush.getId()).getStock());
	}

	// El empleado atiende un evento de su indoor: queda como realizado y el efecto se guarda en la planta
	@Test
	public void elEmpleadoAtiendeUnEventoDeSuIndoor() {
		EmpleadoIndoor empleado = empleadoService.registrar("Andres", "Negro", "cultivador@test.com", "12345", 500000);
		Indoor indoor = indoorService.crearIndoor();
		empleadoService.asignarIndoor(empleado.getId(), indoor.getId());
		Planta planta = indoorService.plantar(indoor.getId(), nuevaPlanta("Amnesia Haze"));

		EventoLuz luz = new EventoLuz(planta);
		em.find(Indoor.class, indoor.getId()).recibirEvento(luz);
		em.flush();
		assertEquals(1, empleadoService.eventosPendientes(empleado.getId()).size());

		empleadoService.atenderEvento(empleado.getId(), indoor.getId(), luz.getId());
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, indoor.getId());
		assertTrue(recargado.getColaEventos().get(0).getRealizado());
		assertTrue(recargado.getEventosPendientes().isEmpty());
		assertTrue(recargado.getPlantas().get(0).isLuz());
		assertTrue(empleadoService.eventosPendientes(empleado.getId()).isEmpty());
	}

	// Sin stock no hay venta: la compra se rechaza entera y no toca ni el stock ni la deuda
	@Test
	public void sinStockSuficienteNoSeVende() {
		Usuario cliente = usuarioService.registrar("Ana", "Perez", "cliente@test.com", "12345");
		usuarioService.asignarTopeCredito(cliente.getId(), 20000);
		Producto kush = productoService.crearProducto("OG Kush", 1000);
		cargarStock(kush, 2);

		assertThrows(StockInsuficienteException.class, () -> compraService.realizarCompra(cliente.getId(),
				List.of(new LineaCompra(kush.getId(), 3)), false));

		assertEquals(2, kush.getStock());
		assertEquals(0, cliente.getDeuda().getMonto());
		assertTrue(cliente.getCompras().isEmpty());
	}

	// El stock solo entra por cosecha, asi que para probar ventas hay que cosechar primero
	private void cargarStock(Producto producto, int cantidad) {
		EmpleadoIndoor empleado = empleadoService.registrar("Bruno", "Diaz", "cosechador@test.com", "12345", 400000);
		Indoor indoor = indoorService.crearIndoor();
		empleadoService.asignarIndoor(empleado.getId(), indoor.getId());
		Planta planta = indoorService.plantar(indoor.getId(), nuevaPlanta(producto.getGenetica()));
		registroService.registrarCosecha(empleado.getId(), indoor.getId(), planta.getId(), cantidad);
		em.flush();
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(80), LocalDate.now().minusDays(90), 60, 120, 30);
	}
}
