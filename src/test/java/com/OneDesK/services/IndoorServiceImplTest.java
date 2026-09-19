package com.OneDesK.services;

import com.OneDesK.DatosDePrueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
		Indoor indoor = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);

		assertTrue(indoor.getId() > 0);
		assertNotNull(em.find(Indoor.class, indoor.getId()));
	}

	// Plantar guarda la planta en la base y devuelve esa misma planta con su id asignado
	@Test
	public void plantarGuardaLaPlantaEnElIndoor() {
		Indoor indoor = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);

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
		Indoor uno = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
		Indoor dos = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
		Planta planta = service.plantar(uno.getId(), nuevaPlanta("OG Kush"));

		assertThrows(OperacionInvalidaException.class, () -> service.plantar(dos.getId(), planta));

		assertEquals(0, dos.getPlantas().size());
	}

	// --- quitar ---

	// Quitar una planta la borra de la base junto con sus eventos, sin tocar las otras plantas
	@Test
	public void quitarUnaPlantaLaBorraConSusEventos() {
		Indoor indoor = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
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
		Indoor indoor = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
		Planta planta = service.plantar(indoor.getId(), nuevaPlanta("OG Kush"));
		planta.cosechar();

		assertThrows(OperacionInvalidaException.class, () -> service.quitarPlanta(indoor.getId(), planta.getId()));

		assertEquals(1, indoor.getPlantas().size());
	}

	// Quitar una planta pidiendosela a un indoor que no la tiene falla con RecursoNoEncontradoException
	@Test
	public void quitarUnaPlantaDeOtroIndoorFalla() {
		Indoor uno = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
		Indoor dos = service.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD);
		Planta planta = service.plantar(uno.getId(), nuevaPlanta("OG Kush"));

		assertThrows(RecursoNoEncontradoException.class, () -> service.quitarPlanta(dos.getId(), planta.getId()));

		assertEquals(1, uno.getPlantas().size());
	}

	// --- nombre y capacidad ---

	// Crear un indoor lo guarda con su nombre y su capacidad
	@Test
	public void crearUnIndoorGuardaNombreYCapacidad() {
		int id = service.crearIndoor("Carpa del fondo", 8).getId();
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, id);
		assertEquals("Carpa del fondo", recargado.getNombre());
		assertEquals(8, recargado.getCapacidad());
	}

	// No se puede repetir el nombre de otro indoor, aunque cambien las mayusculas o los espacios
	@Test
	public void noSePuedeRepetirElNombre() {
		service.crearIndoor("Carpa del fondo", 8);
		em.flush();

		assertThrows(OperacionInvalidaException.class, () -> service.crearIndoor("  carpa DEL fondo ", 3));
	}

	// Plantar en un indoor lleno se rechaza y no se guarda la planta
	@Test
	public void plantarEnUnIndoorLlenoSeRechaza() {
		int id = service.crearIndoor("Carpa chica", 1).getId();
		service.plantar(id, nuevaPlanta("OG Kush"));
		em.flush();
		em.clear();

		assertThrows(OperacionInvalidaException.class, () -> service.plantar(id, nuevaPlanta("Amnesia")));
		em.flush();
		em.clear();

		assertEquals(1, em.find(Indoor.class, id).getPlantas().size());
	}

	// --- editar ---

	// Editar queda guardado en la base, sin save
	@Test
	public void editarQuedaGuardado() {
		int id = service.crearIndoor("Carpa del fondo", 8).getId();
		em.flush();
		em.clear();

		service.editarIndoor(id, "Carpa del frente", 15);
		em.flush();
		em.clear();

		Indoor recargado = em.find(Indoor.class, id);
		assertEquals("Carpa del frente", recargado.getNombre());
		assertEquals(15, recargado.getCapacidad());
	}

	// Se puede dejar el mismo nombre, o cambiarle solo las mayusculas, sin chocar consigo mismo
	@Test
	public void editarPuedeConservarSuPropioNombre() {
		int id = service.crearIndoor("Carpa del fondo", 8).getId();
		em.flush();

		service.editarIndoor(id, "Carpa del fondo", 12);
		service.editarIndoor(id, "CARPA DEL FONDO", 12);
		em.flush();
		em.clear();

		assertEquals("CARPA DEL FONDO", em.find(Indoor.class, id).getNombre());
	}

	// No se puede usar el nombre de otro indoor, y el indoor queda como estaba
	@Test
	public void editarNoPuedeUsarElNombreDeOtro() {
		service.crearIndoor("Carpa del fondo", 8);
		int otro = service.crearIndoor("Carpa del frente", 8).getId();
		em.flush();
		em.clear();

		assertThrows(OperacionInvalidaException.class, () -> service.editarIndoor(otro, " carpa DEL FONDO ", 8));
		em.flush();
		em.clear();

		assertEquals("Carpa del frente", em.find(Indoor.class, otro).getNombre());
	}

	// Bajar la capacidad por debajo de lo plantado se rechaza y queda la anterior
	@Test
	public void editarNoBajaLaCapacidadDeLoPlantado() {
		int id = service.crearIndoor("Carpa del fondo", 8).getId();
		service.plantar(id, nuevaPlanta("OG Kush"));
		service.plantar(id, nuevaPlanta("Amnesia"));
		em.flush();
		em.clear();

		assertThrows(OperacionInvalidaException.class, () -> service.editarIndoor(id, "Carpa del fondo", 1));
		em.flush();
		em.clear();

		assertEquals(8, em.find(Indoor.class, id).getCapacidad());
	}

	// Editar un indoor que no existe falla con RecursoNoEncontradoException
	@Test
	public void editarUnIndoorInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.editarIndoor(999999, "Carpa", 5));
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
