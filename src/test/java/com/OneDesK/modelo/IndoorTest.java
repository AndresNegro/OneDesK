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

	@Test
	public void noSePuedeQuitarUnaPlantaCosechada() {
		indoor.recibirEvento(new EventoLuz(kush));
		kush.cosechar();

		assertThrows(OperacionInvalidaException.class, () -> indoor.deletePlanta(kush));

		assertTrue(indoor.getPlantas().contains(kush));
		assertEquals(1, indoor.getColaEventos().size());
	}

	@Test
	public void losEventosAtendidosNoFiguranComoPendientes() {
		EventoLuz luz = new EventoLuz(kush);
		indoor.recibirEvento(luz);
		indoor.recibirEvento(new EventoRegado(amnesia));

		luz.setRealizado(true);

		assertEquals(1, indoor.getEventosPendientes().size());
		assertEquals(2, indoor.getColaEventos().size());
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
