package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.evento.Evento;
import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Usuario;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EmpleadoIndoorServiceImpl.class)
public class EmpleadoIndoorServiceImplTest {

	@Autowired
	private EmpleadoIndoorService service;
	@Autowired
	private TestEntityManager em;

	private EmpleadoIndoor empleado;
	private Indoor indoor;
	private Planta planta;

	@BeforeEach
	public void setUp() {
		empleado = service.registrar("Andres", "Negro", "empleado@test.com", "12345", 500000);
		indoor = new Indoor();
		planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		em.persistAndFlush(indoor);
	}

	// --- registrar ---

	// Registrar un empleado lo guarda en la base con su email y su salario
	@Test
	public void registrarGuardaAlEmpleado() {
		em.flush();
		em.clear();

		EmpleadoIndoor recargado = em.find(EmpleadoIndoor.class, empleado.getId());

		assertEquals("empleado@test.com", recargado.getEmail());
		assertEquals(500000, recargado.getSalarioMensual());
	}

	// Un empleado no puede registrarse con el email de un usuario existente, aunque cambien las mayusculas
	@Test
	public void noSePuedeRegistrarUnEmailQueYaUsaUnUsuario() {
		em.persistAndFlush(new Usuario("Otro", "Cliente", "cliente@test.com", "12345"));

		assertThrows(EmailDuplicadoException.class,
				() -> service.registrar("Andres", "Negro", "CLIENTE@test.com", "12345", 500000));
	}

	// --- salario ---

	// Cambiar el salario queda guardado en la base
	@Test
	public void cambiarElSalarioQuedaGuardado() {
		service.cambiarSalario(empleado.getId(), 650000);
		em.flush();
		em.clear();

		assertEquals(650000, em.find(EmpleadoIndoor.class, empleado.getId()).getSalarioMensual());
	}

	// Un salario de 0 se rechaza y en la base queda el anterior
	@Test
	public void unSalarioDeCeroNoSeGuarda() {
		em.flush();
		em.clear();

		assertThrows(IllegalArgumentException.class, () -> service.cambiarSalario(empleado.getId(), 0));
		em.flush();
		em.clear();

		assertEquals(500000, em.find(EmpleadoIndoor.class, empleado.getId()).getSalarioMensual());
	}

	// Cambiar el salario de un empleado inexistente falla con RecursoNoEncontradoException
	@Test
	public void cambiarElSalarioDeUnEmpleadoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarSalario(9999, 650000));
	}

	// --- asignaciones ---

	// La asignacion de un indoor al empleado queda guardada en la tabla Trabaja
	@Test
	public void asignarUnIndoorQuedaGuardado() {
		service.asignarIndoor(empleado.getId(), indoor.getId());
		em.flush();
		em.clear();

		EmpleadoIndoor recargado = em.find(EmpleadoIndoor.class, empleado.getId());

		assertEquals(1, recargado.getSectoresACargo().size());
	}

	// Asignar dos veces el mismo indoor al mismo empleado se rechaza
	@Test
	public void noSePuedeAsignarDosVecesElMismoIndoor() {
		service.asignarIndoor(empleado.getId(), indoor.getId());

		assertThrows(OperacionInvalidaException.class, () -> service.asignarIndoor(empleado.getId(), indoor.getId()));
	}

	// Desasignar borra la fila de Trabaja en la base, pero el indoor sigue existiendo
	@Test
	public void desasignarQuedaGuardadoSinBorrarElIndoor() {
		service.asignarIndoor(empleado.getId(), indoor.getId());
		em.flush();

		service.desasignarIndoor(empleado.getId(), indoor.getId());
		em.flush();
		em.clear();

		assertEquals(0, em.find(EmpleadoIndoor.class, empleado.getId()).getSectoresACargo().size());
		assertNotNull(em.find(Indoor.class, indoor.getId()));
	}

	// Desasignar un indoor que el empleado no tenia se rechaza
	@Test
	public void noSePuedeDesasignarUnIndoorNoAsignado() {
		assertThrows(OperacionInvalidaException.class,
				() -> service.desasignarIndoor(empleado.getId(), indoor.getId()));
	}

	// Asignar un indoor que no existe falla con RecursoNoEncontradoException
	@Test
	public void asignarUnIndoorInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarIndoor(empleado.getId(), 9999));
	}

	// Asignar un indoor a un empleado que no existe falla con RecursoNoEncontradoException
	@Test
	public void asignarAUnEmpleadoInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarIndoor(9999, indoor.getId()));
	}

	// --- eventos ---

	// Atender un evento lo guarda como realizado sin borrarlo y el efecto sobre la planta queda guardado
	@Test
	public void atenderUnEventoLoGuardaComoRealizado() {
		service.asignarIndoor(empleado.getId(), indoor.getId());
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(luz);
		em.flush();

		service.atenderEvento(empleado.getId(), indoor.getId(), luz.getId());
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, indoor.getId());
		assertEquals(1, recargado.getColaEventos().size());
		assertTrue(recargado.getColaEventos().get(0).getRealizado());
		assertTrue(recargado.getPlantas().get(0).isLuz());
	}

	// Los pendientes del empleado excluyen lo ya atendido y los eventos de indoors que no tiene a cargo
	@Test
	public void losPendientesSoloIncluyenLoNoAtendidoDeSusIndoors() {
		service.asignarIndoor(empleado.getId(), indoor.getId());
		EventoRegado riego = new EventoRegado(planta);
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(riego);
		indoor.recibirEvento(luz);

		Indoor otroIndoor = new Indoor();
		otroIndoor.recibirEvento(new EventoLuz(otroIndoor.addPlanta(nuevaPlanta("Amnesia"))));
		em.persist(otroIndoor);
		em.flush();

		service.atenderEvento(empleado.getId(), indoor.getId(), riego.getId());
		List<Evento> pendientes = service.eventosPendientes(empleado.getId());

		assertEquals(1, pendientes.size());
		assertSame(luz, pendientes.get(0));
	}

	// Un empleado no puede atender eventos de un indoor al que no esta asignado
	@Test
	public void noSePuedeAtenderUnEventoDeUnIndoorNoAsignado() {
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(luz);
		em.flush();

		assertThrows(OperacionInvalidaException.class,
				() -> service.atenderEvento(empleado.getId(), indoor.getId(), luz.getId()));

		assertFalse(luz.getRealizado());
	}

	// Atender dos veces el mismo evento se rechaza
	@Test
	public void noSePuedeAtenderDosVecesElMismoEvento() {
		service.asignarIndoor(empleado.getId(), indoor.getId());
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(luz);
		em.flush();
		service.atenderEvento(empleado.getId(), indoor.getId(), luz.getId());

		assertThrows(OperacionInvalidaException.class,
				() -> service.atenderEvento(empleado.getId(), indoor.getId(), luz.getId()));
	}

	// Buscar un evento en un indoor que no lo tiene falla con RecursoNoEncontradoException
	@Test
	public void unEventoQueNoEsDeEseIndoorNoSeEncuentra() {
		Indoor otroIndoor = new Indoor();
		em.persist(otroIndoor);
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(luz);
		em.flush();

		assertThrows(RecursoNoEncontradoException.class,
				() -> service.atenderEvento(empleado.getId(), otroIndoor.getId(), luz.getId()));
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
