package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;

@DataJpaTest
@Import(IndoorServiceImpl.class)
public class IndoorServiceImplTest {

	@Autowired
	private IndoorService service;
	@Autowired
	private TestEntityManager em;

	// --- crear y plantar ---

	// Crear un indoor lo guarda en la base con su id
	@Test
	public void crearUnIndoorLoGuarda() {
		Indoor indoor = service.crearIndoor();

		assertTrue(indoor.getId() > 0);
		assertNotNull(em.find(Indoor.class, indoor.getId()));
	}

	// Plantar guarda la planta en la base y devuelve esa misma planta con su id asignado
	@Test
	public void plantarGuardaLaPlantaEnElIndoor() {
		Indoor indoor = service.crearIndoor();

		Planta planta = service.plantar(indoor.getId(), nuevaPlanta("OG Kush"));
		em.clear();

		assertTrue(planta.getId() > 0);
		Indoor recargado = em.find(Indoor.class, indoor.getId());
		assertEquals(1, recargado.getPlantas().size());
		assertEquals("OG Kush", recargado.getPlantas().get(0).getGenetica());
	}

	// Plantar en un indoor que no existe falla con RecursoNoEncontradoException
	@Test
	public void plantarEnUnIndoorInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.plantar(9999, nuevaPlanta("OG Kush")));
	}

	// Una planta que ya esta en un indoor no se puede plantar en otro
	@Test
	public void unaPlantaYaPlantadaNoSePuedePlantarEnOtroIndoor() {
		Indoor uno = service.crearIndoor();
		Indoor dos = service.crearIndoor();
		Planta planta = service.plantar(uno.getId(), nuevaPlanta("OG Kush"));

		assertThrows(OperacionInvalidaException.class, () -> service.plantar(dos.getId(), planta));

		assertEquals(0, dos.getPlantas().size());
	}

	// --- quitar ---

	// Quitar una planta la borra de la base junto con sus eventos, sin tocar las otras plantas
	@Test
	public void quitarUnaPlantaLaBorraConSusEventos() {
		Indoor indoor = service.crearIndoor();
		Planta muerta = service.plantar(indoor.getId(), nuevaPlanta("OG Kush"));
		Planta sana = service.plantar(indoor.getId(), nuevaPlanta("Amnesia"));
		indoor.recibirEvento(new EventoRegado(muerta));
		indoor.recibirEvento(new EventoLuz(sana));
		em.flush();
		int idMuerta = muerta.getId();

		service.quitarPlanta(indoor.getId(), idMuerta);
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, indoor.getId());
		assertNull(em.find(Planta.class, idMuerta));
		assertEquals(1, recargado.getPlantas().size());
		assertEquals("Amnesia", recargado.getPlantas().get(0).getGenetica());
		assertEquals(1, recargado.getColaEventos().size());
	}

	// Una planta cosechada no se puede quitar y sigue en el indoor
	@Test
	public void noSePuedeQuitarUnaPlantaCosechada() {
		Indoor indoor = service.crearIndoor();
		Planta planta = service.plantar(indoor.getId(), nuevaPlanta("OG Kush"));
		planta.cosechar();

		assertThrows(OperacionInvalidaException.class, () -> service.quitarPlanta(indoor.getId(), planta.getId()));

		assertEquals(1, indoor.getPlantas().size());
	}

	// Quitar una planta pidiendosela a un indoor que no la tiene falla con RecursoNoEncontradoException
	@Test
	public void quitarUnaPlantaDeOtroIndoorFalla() {
		Indoor uno = service.crearIndoor();
		Indoor dos = service.crearIndoor();
		Planta planta = service.plantar(uno.getId(), nuevaPlanta("OG Kush"));

		assertThrows(RecursoNoEncontradoException.class, () -> service.quitarPlanta(dos.getId(), planta.getId()));

		assertEquals(1, uno.getPlantas().size());
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
