package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.OneDesK.evento.Evento;
import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoVentilador;

@DataJpaTest
public class MapeoRelacionesTest {

	@Autowired
	private TestEntityManager em;

	// --- Indoor / Planta ---

	@Test
	public void guardarUnIndoorGuardaSusPlantas() {
		Indoor indoor = new Indoor();
		indoor.addPlanta(nuevaPlanta("OG Kush"));
		indoor.addPlanta(nuevaPlanta("Amnesia"));
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();
		em.clear();

		Indoor recargado = em.find(Indoor.class, idIndoor);

		assertEquals(2, recargado.getPlantas().size());
	}

	@Test
	public void sacarUnaPlantaDeLaListaLaBorra() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		indoor.addPlanta(nuevaPlanta("Amnesia"));
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();

		indoor.deletePlanta(planta);
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, idIndoor);
		assertNotNull(recargado);
		assertEquals(1, recargado.getPlantas().size());
	}

	@Test
	public void borrarUnaPlantaNoBorraSuIndoor() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();

		// se borra la planta con el vinculo al indoor todavia puesto:
		// si Planta cascadea hacia Indoor, se lleva puesto el indoor entero
		em.remove(planta);
		em.flush();
		em.clear();

		assertNotNull(em.find(Indoor.class, idIndoor));
	}

	@Test
	public void borrarUnIndoorBorraSusPlantas() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		em.persistAndFlush(indoor);
		int idPlanta = planta.getId();

		em.remove(indoor);
		em.flush();
		em.clear();

		assertNull(em.find(Planta.class, idPlanta));
	}

	// --- Compra / ItemCompra / Producto ---

	@Test
	public void borrarUnItemNoBorraElProductoDelCatalogo() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		Producto producto = new Producto("OG Kush", 10, 1000);
		em.persist(usuario);
		em.persist(producto);

		Compra compra = new Compra(LocalDate.now(), false, usuario);
		ItemCompra item = new ItemCompra(producto, 2);
		compra.addItem(item);
		em.persistAndFlush(compra);
		int idProducto = producto.getId();

		compra.deleteItem(item);
		em.flush();
		em.clear();

		assertNotNull(em.find(Producto.class, idProducto));
	}

	@Test
	public void borrarUnaCompraNoBorraAlUsuario() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		em.persist(usuario);
		Compra compra = new Compra(LocalDate.now(), false, usuario);
		em.persistAndFlush(compra);
		int idUsuario = usuario.getId();

		em.remove(compra);
		em.flush();
		em.clear();

		assertNotNull(em.find(Usuario.class, idUsuario));
	}

	@Test
	public void laCompraGuardaSuUsuario() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		em.persist(usuario);
		Compra compra = new Compra(LocalDate.now(), false, usuario);
		em.persistAndFlush(compra);
		int idCompra = compra.getId();
		em.clear();

		Compra recargada = em.find(Compra.class, idCompra);

		assertNotNull(recargada.getUsuario());
		assertEquals("comprador@test.com", recargada.getUsuario().getEmail());
	}

	// --- Trabaja (ManyToMany) ---

	@Test
	public void unEmpleadoQuedaAsignadoASusIndoors() {
		Indoor indoor = new Indoor();
		EmpleadoIndoor empleado = new EmpleadoIndoor("Andres", "Negro", "empleado@test.com", "12345", 500000);
		em.persist(indoor);
		em.persist(empleado);
		empleado.addIndoor(indoor);
		em.flush();
		int idEmpleado = empleado.getId();
		em.clear();

		EmpleadoIndoor recargado = em.find(EmpleadoIndoor.class, idEmpleado);

		assertEquals(1, recargado.getSectoresACargo().size());
	}

	@Test
	public void desasignarUnIndoorNoBorraNiAlEmpleadoNiAlIndoor() {
		Indoor indoor = new Indoor();
		EmpleadoIndoor empleado = new EmpleadoIndoor("Andres", "Negro", "empleado@test.com", "12345", 500000);
		em.persist(indoor);
		em.persist(empleado);
		empleado.addIndoor(indoor);
		em.flush();
		int idIndoor = indoor.getId();
		int idEmpleado = empleado.getId();

		empleado.deleteIndoor(indoor);
		em.flush();
		em.clear();

		assertNotNull(em.find(Indoor.class, idIndoor));
		assertNotNull(em.find(EmpleadoIndoor.class, idEmpleado));
	}

	// --- Eventos ---

	@Test
	public void cadaTipoDeEventoSeRecuperaConSuClase() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		indoor.recibirEvento(new EventoLuz(planta));
		indoor.recibirEvento(new EventoVentilador(planta));
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();
		em.clear();

		Indoor recargado = em.find(Indoor.class, idIndoor);
		Evento primero = recargado.getColaEventos().get(0);
		Evento segundo = recargado.getColaEventos().get(1);

		assertInstanceOf(EventoLuz.class, primero);
		assertInstanceOf(EventoVentilador.class, segundo);
	}

	@Test
	public void borrarUnEventoNoBorraLaPlanta() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		indoor.recibirEvento(new EventoLuz(planta));
		em.persistAndFlush(indoor);
		int idPlanta = planta.getId();

		indoor.consumirEvento(0);
		em.flush();
		em.clear();

		assertNotNull(em.find(Planta.class, idPlanta));
	}

	// --- helpers ---

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now(), LocalDate.now(), 60, 60, 60);
	}

	private Usuario nuevoUsuario(String email) {
		return new Usuario("Andres", "Negro", email, "12345");
	}
}
