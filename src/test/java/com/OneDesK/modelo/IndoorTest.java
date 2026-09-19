package com.OneDesK.modelo;

import com.OneDesK.DatosDePrueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.evento.EventoVentilador;
import com.OneDesK.excepciones.OperacionInvalidaException;

public class IndoorTest {

	private Indoor indoor;
	private Planta kush;
	private Planta amnesia;

	@BeforeEach
	public void setUp() {
		indoor = DatosDePrueba.indoor();
		kush = indoor.addPlanta(nuevaPlanta("OG Kush"));
		amnesia = indoor.addPlanta(nuevaPlanta("Amnesia"));
	}

	// --- quitar plantas ---

	// Quitar una planta borra sus eventos y deja intactos los de las otras plantas
	@Test
	public void quitarUnaPlantaDescartaSoloSusEventos() {
		indoor.recibirEvento(new EventoLuz(kush));
		indoor.recibirEvento(new EventoRegado(amnesia));
		indoor.recibirEvento(new EventoVentilador(kush));

		indoor.deletePlanta(kush);

		assertEquals(1, indoor.getPlantas().size());
		assertEquals(1, indoor.getColaEventos().size());
		assertSame(amnesia, indoor.getColaEventos().get(0).getPlanta());
		assertNull(kush.getIndoor());
	}

	// Una planta cosechada no se puede quitar del indoor
	@Test
	public void noSePuedeQuitarUnaPlantaCosechada() {
		kush.cosechar();

		assertThrows(OperacionInvalidaException.class, () -> indoor.deletePlanta(kush));

		assertTrue(indoor.getPlantas().contains(kush));
	}

	// --- eventos ---

	// Un evento atendido sigue en el historial pero deja de figurar entre los pendientes
	@Test
	public void losEventosAtendidosNoFiguranComoPendientes() {
		EventoLuz luz = new EventoLuz(kush);
		indoor.recibirEvento(luz);
		indoor.recibirEvento(new EventoRegado(amnesia));

		luz.setRealizado(true);

		assertEquals(1, indoor.getEventosPendientes().size());
		assertEquals(2, indoor.getColaEventos().size());
	}

	// Cosechar descarta los pendientes de esa planta, conserva sus atendidos y no toca los eventos de otras plantas
	@Test
	public void cosecharUnaPlantaDescartaSusPendientesYConservaLosAtendidos() {
		EventoRegado atendido = new EventoRegado(kush);
		indoor.recibirEvento(atendido);
		atendido.setRealizado(true);
		indoor.recibirEvento(new EventoLuz(kush));
		EventoLuz deAmnesia = new EventoLuz(amnesia);
		indoor.recibirEvento(deAmnesia);

		kush.cosechar();

		assertEquals(2, indoor.getColaEventos().size());
		assertTrue(indoor.getColaEventos().contains(atendido));
		assertEquals(1, indoor.getEventosPendientes().size());
		assertSame(deAmnesia, indoor.getEventosPendientes().get(0));
	}

	// Un indoor rechaza eventos de una planta que pertenece a otro indoor
	@Test
	public void noSeAceptanEventosDeUnaPlantaDeOtroIndoor() {
		Indoor otroIndoor = DatosDePrueba.indoor();
		Planta ajena = otroIndoor.addPlanta(nuevaPlanta("Haze"));

		assertThrows(OperacionInvalidaException.class, () -> indoor.recibirEvento(new EventoLuz(ajena)));

		assertEquals(0, indoor.getColaEventos().size());
	}

	// Un indoor rechaza eventos nuevos para una planta que ya fue cosechada
	@Test
	public void noSeAceptanEventosDeUnaPlantaCosechada() {
		kush.cosechar();

		assertThrows(OperacionInvalidaException.class, () -> indoor.recibirEvento(new EventoVentilador(kush)));

		assertEquals(0, indoor.getColaEventos().size());
	}

	// --- nombre y capacidad ---

	// Un indoor nuevo guarda su nombre sin espacios de mas y su capacidad
	@Test
	public void unIndoorNuevoTieneNombreYCapacidad() {
		Indoor indoor = new Indoor("  Carpa grande ", 12);

		assertEquals("Carpa grande", indoor.getNombre());
		assertEquals(12, indoor.getCapacidad());
		assertEquals(0, indoor.plantasEnCultivo());
	}

	// Sin nombre o con capacidad 0 no se puede crear
	@Test
	public void elNombreYLaCapacidadSeValidan() {
		assertThrows(IllegalArgumentException.class, () -> new Indoor("  ", 12));
		assertThrows(IllegalArgumentException.class, () -> new Indoor(null, 12));
		assertThrows(IllegalArgumentException.class, () -> new Indoor("Carpa", 0));
	}

	// Con la capacidad completa no entra otra planta, y la que se rechaza no queda en el indoor
	@Test
	public void unIndoorLlenoNoRecibeMasPlantas() {
		Indoor chico = new Indoor("Carpa chica", 2);
		chico.addPlanta(nuevaPlanta("OG Kush"));
		chico.addPlanta(nuevaPlanta("Amnesia"));

		assertTrue(chico.isLleno());
		assertThrows(OperacionInvalidaException.class, () -> chico.addPlanta(nuevaPlanta("Haze")));
		assertEquals(2, chico.getPlantas().size());
	}

	// Una planta cosechada ya no ocupa lugar: despues de cosechar entra otra
	@Test
	public void cosecharLiberaLugar() {
		Indoor chico = new Indoor("Carpa chica", 1);
		Planta primera = chico.addPlanta(nuevaPlanta("OG Kush"));

		primera.cosechar();

		assertFalse(chico.isLleno());
		chico.addPlanta(nuevaPlanta("Amnesia"));
		assertEquals(1, chico.plantasEnCultivo());
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
