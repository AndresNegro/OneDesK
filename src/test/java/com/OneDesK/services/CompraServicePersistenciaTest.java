package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;

// Complementa a CompraServiceImplTest, que usa repositorios mockeados: aca se verifica que todo quede guardado
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CompraServiceImpl.class)
public class CompraServicePersistenciaTest {

	@Autowired
	private CompraService service;
	@Autowired
	private TestEntityManager em;

	private Usuario usuario;
	private Producto kush;

	@BeforeEach
	public void setUp() {
		usuario = new Usuario("Andres", "Negro", "comprador@test.com", "12345");
		usuario.setTopeCredito(10000);
		em.persist(usuario);
		kush = new Producto("OG Kush", 10, 1000);
		em.persist(kush);
		em.flush();
	}

	// Una compra impaga queda guardada en la base con sus items, la deuda del usuario y el stock descontado
	@Test
	public void unaCompraImpagaQuedaGuardadaConSuDeudaYElStockDescontado() {
		int idCompra = comprarKush(3).getId();
		em.flush();
		em.clear();

		Compra compra = em.find(Compra.class, idCompra);
		assertFalse(compra.isPagado());
		assertEquals(3000, compra.getPrecio());
		assertEquals(1, compra.getItems().size());
		assertEquals(1000, compra.getItems().get(0).getPrecioUnitario());
		assertEquals(3000, em.find(Usuario.class, usuario.getId()).getDeuda().getMonto());
		assertEquals(7, em.find(Producto.class, kush.getId()).getStock());
	}

	// El pago queda guardado en la base y la deuda del usuario vuelve a 0
	@Test
	public void registrarElPagoQuedaGuardado() {
		int idCompra = comprarKush(3).getId();
		em.flush();

		service.registrarPago(idCompra);
		em.flush();
		em.clear();

		assertTrue(em.find(Compra.class, idCompra).isPagado());
		assertEquals(0, em.find(Usuario.class, usuario.getId()).getDeuda().getMonto());
	}

	// Anular borra de la base la compra y sus items, devuelve el stock y deja la deuda en 0
	@Test
	public void anularUnaCompraLaBorraConSusItemsYDevuelveElStock() {
		int idCompra = comprarKush(3).getId();
		em.flush();

		service.anularCompra(idCompra);
		em.flush();
		em.clear();

		assertNull(em.find(Compra.class, idCompra));
		long items = em.getEntityManager().createQuery("select count(i) from ItemCompra i", Long.class)
				.getSingleResult();
		assertEquals(0, items);
		assertEquals(10, em.find(Producto.class, kush.getId()).getStock());
		assertEquals(0, em.find(Usuario.class, usuario.getId()).getDeuda().getMonto());
	}

	// Anular una compra creada en la misma transaccion, sin flush en el medio: igual tiene que borrarse.
	// El orphanRemoval solo no alcanza, porque la coleccion termina como empezo y no ve ningun huerfano.
	@Test
	public void anularUnaCompraRecienCreadaTambienLaBorra() {
		Usuario cliente = new Usuario("Ana", "Perez", "otra@test.com", "12345");
		cliente.setTopeCredito(20000);
		em.persist(cliente);
		Producto otro = new Producto("Amnesia Haze", 50, 1000);
		em.persistAndFlush(otro);

		Compra anulada = service.realizarCompra(cliente.getId(), List.of(new LineaCompra(otro.getId(), 2)), false);
		int idAnulada = anulada.getId();
		service.anularCompra(idAnulada);
		em.flush();
		em.clear();

		assertNull(em.find(Compra.class, idAnulada));
	}

	// Pagar una compra y anular otra: el pago no tiene que impedir el borrado de la anulada
	@Test
	public void pagarUnaCompraNoImpideAnularOtra() {
		int idPagada = comprarKush(3).getId();
		int idAnulada = comprarKush(2).getId();
		em.flush();

		service.registrarPago(idPagada);
		service.anularCompra(idAnulada);
		em.flush();
		em.clear();

		assertNull(em.find(Compra.class, idAnulada));
		assertTrue(em.find(Compra.class, idPagada).isPagado());
	}

	// Anular una compra cuando el usuario tiene varias: solo se borra esa
	@Test
	public void anularUnaDeDosComprasBorraSoloEsa() {
		int idPrimera = comprarKush(3).getId();
		int idSegunda = comprarKush(2).getId();
		em.flush();

		service.anularCompra(idSegunda);
		em.flush();
		em.clear();

		assertNull(em.find(Compra.class, idSegunda));
		assertNotNull(em.find(Compra.class, idPrimera));
	}

	private Compra comprarKush(int cantidad) {
		return service.realizarCompra(usuario.getId(), List.of(new LineaCompra(kush.getId(), cantidad)), false);
	}
}
