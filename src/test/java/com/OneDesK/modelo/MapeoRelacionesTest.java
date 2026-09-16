package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.OneDesK.evento.Evento;
import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoVentilador;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MapeoRelacionesTest {

	@Autowired
	private TestEntityManager em;

	// --- Indoor / Planta ---

	// Guardar un indoor guarda tambien sus plantas por cascada
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

	// Sacar una planta de la lista del indoor la borra de la base (orphanRemoval)
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

	// Borrar una planta no borra su indoor: la cascada va del indoor a la planta, no al reves
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

	// Borrar un indoor borra sus plantas por cascada
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

	// Borrar una compra borra sus items, pero no los productos del catalogo a los que apuntan
	@Test
	public void borrarUnaCompraNoBorraLosProductosDeSusItems() {
		Usuario usuario = nuevoUsuario("comprador@test.com");
		Producto producto = new Producto("OG Kush", 10, 1000);
		em.persist(usuario);
		em.persist(producto);

		Compra compra = new Compra(LocalDate.now(), false, usuario);
		compra.addItem(new ItemCompra(producto, 2));
		em.persistAndFlush(compra);
		int idProducto = producto.getId();

		// los items se borran con la compra, pero el producto del catalogo tiene que seguir existiendo
		em.remove(compra);
		em.flush();
		em.clear();

		assertNotNull(em.find(Producto.class, idProducto));
	}

	// Borrar una compra no borra al usuario que la hizo
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

	// La FK de Compra hacia Usuario se guarda y el usuario vuelve al releer la compra
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

	// La relacion muchos a muchos de la tabla Trabaja se guarda desde el lado del empleado
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

	// Desasignar solo borra la fila de Trabaja, no al empleado ni al indoor
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

	// La columna tipo hace que cada evento vuelva de la base con su propia clase
	@Test
	public void cadaTipoDeEventoSeRecuperaConSuClase() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		EventoLuz luz = new EventoLuz(planta);
		EventoVentilador ventilador = new EventoVentilador(planta);
		indoor.recibirEvento(luz);
		indoor.recibirEvento(ventilador);
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();
		// se buscan por id: Hibernate inserta agrupando por clase, no en el orden de la cola
		int idLuz = luz.getId();
		int idVentilador = ventilador.getId();
		em.clear();

		Indoor recargado = em.find(Indoor.class, idIndoor);

		assertInstanceOf(EventoLuz.class, recargado.buscarEvento(idLuz));
		assertInstanceOf(EventoVentilador.class, recargado.buscarEvento(idVentilador));
	}

	// El campo realizado se guarda en la base y el evento deja de figurar como pendiente al releerlo
	@Test
	public void unEventoAtendidoQuedaGuardadoComoRealizado() {
		Indoor indoor = new Indoor();
		Planta planta = nuevaPlanta("OG Kush");
		indoor.addPlanta(planta);
		EventoVentilador evento = new EventoVentilador(planta);
		indoor.recibirEvento(evento);
		em.persistAndFlush(indoor);
		int idIndoor = indoor.getId();

		// realizado se mapea por separado en cada subclase de Evento, sobre la misma columna
		evento.setRealizado(true);
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, idIndoor);
		assertEquals(1, recargado.getColaEventos().size());
		assertEquals(true, recargado.getColaEventos().get(0).getRealizado());
		assertEquals(0, recargado.getEventosPendientes().size());
	}

	// --- Persona ---

	// El UNIQUE de email rechaza guardar un empleado con el mismo email que un usuario
	@Test
	public void laBaseNoPermiteDosPersonasConElMismoEmail() {
		em.persistAndFlush(nuevoUsuario("repetido@test.com"));
		EmpleadoIndoor empleado = new EmpleadoIndoor("Otro", "Empleado", "repetido@test.com", "12345", 500000);

		// un usuario y un empleado: la unicidad es sobre toda la tabla Persona
		assertThrows(PersistenceException.class, () -> em.persistAndFlush(empleado));
	}

	// --- helpers ---

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now(), LocalDate.now(), 60, 60, 60);
	}

	private Usuario nuevoUsuario(String email) {
		return new Usuario("Andres", "Negro", email, "12345");
	}
}
