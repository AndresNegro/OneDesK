package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		indoor = new Indoor();
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
		Indoor otroIndoor = new Indoor();
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

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
