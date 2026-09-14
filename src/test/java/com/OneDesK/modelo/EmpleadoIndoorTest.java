package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class EmpleadoIndoorTest {

	private EmpleadoIndoor empleado;
	private Indoor indoor;
	private Planta planta;

	@BeforeEach
	public void setUp() {
		empleado = new EmpleadoIndoor("Andres", "Negro", "empleado@test.com", "12345", 500000);
		indoor = new Indoor();
		planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		empleado.addIndoor(indoor);
	}

	// --- salario ---

	@Test
	public void elSalarioTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class,
				() -> new EmpleadoIndoor("Andres", "Negro", "otro@test.com", "12345", 0));
	}

	@Test
	public void cambiarElSalarioTambienSeValida() {
		assertThrows(IllegalArgumentException.class, () -> empleado.setSalarioMensual(-1));

		assertEquals(500000, empleado.getSalarioMensual());
	}

	// --- asignaciones ---

	@Test
	public void noSePuedeAsignarDosVecesElMismoIndoor() {
		assertThrows(OperacionInvalidaException.class, () -> empleado.addIndoor(indoor));

		assertEquals(1, empleado.getSectoresACargo().size());
	}

	@Test
	public void desasignarLoSacaDeSusIndoors() {
		empleado.deleteIndoor(indoor);

		assertFalse(empleado.estaAsignadoA(indoor));
	}

	@Test
	public void noSePuedeDesasignarUnIndoorQueNoTiene() {
		assertThrows(OperacionInvalidaException.class, () -> empleado.deleteIndoor(new Indoor()));
	}

	// --- eventos ---

	@Test
	public void atenderUnRiegoLoDejaComoRealizadoSinBorrarlo() {
		EventoRegado riego = new EventoRegado(planta);
		indoor.recibirEvento(riego);

		empleado.atenderEvento(riego);

		assertTrue(riego.getRealizado());
		assertEquals(0, indoor.getEventosPendientes().size());
		assertEquals(1, indoor.getColaEventos().size());
	}

	@Test
	public void cadaEventoDeLuzCambiaElEstadoDeLaLuz() {
		EventoLuz prender = new EventoLuz(planta);
		EventoLuz apagar = new EventoLuz(planta);
		indoor.recibirEvento(prender);
		indoor.recibirEvento(apagar);

		empleado.atenderEvento(prender);
		assertTrue(planta.isLuz());

		empleado.atenderEvento(apagar);
		assertFalse(planta.isLuz());
	}

	@Test
	public void unEventoDeVentiladorCambiaElEstadoDelVentilador() {
		EventoVentilador ventilador = new EventoVentilador(planta);
		indoor.recibirEvento(ventilador);

		empleado.atenderEvento(ventilador);

		assertTrue(planta.isVentilador());
	}

	@Test
	public void noSePuedeAtenderDosVecesElMismoEvento() {
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(luz);
		empleado.atenderEvento(luz);

		assertThrows(OperacionInvalidaException.class, () -> empleado.atenderEvento(luz));

		assertTrue(planta.isLuz());
	}

	@Test
	public void noSePuedeAtenderUnEventoDeUnIndoorNoAsignado() {
		Indoor otroIndoor = new Indoor();
		Planta otraPlanta = otroIndoor.addPlanta(nuevaPlanta("Amnesia"));
		EventoLuz luz = new EventoLuz(otraPlanta);
		otroIndoor.recibirEvento(luz);

		assertThrows(OperacionInvalidaException.class, () -> empleado.atenderEvento(luz));

		assertFalse(luz.getRealizado());
		assertFalse(otraPlanta.isLuz());
	}

	@Test
	public void losPendientesSonLosNoAtendidosDeSusIndoors() {
		EventoRegado riego = new EventoRegado(planta);
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(riego);
		indoor.recibirEvento(luz);

		Indoor otroIndoor = new Indoor();
		otroIndoor.recibirEvento(new EventoLuz(otroIndoor.addPlanta(nuevaPlanta("Amnesia"))));

		empleado.atenderEvento(riego);

		assertEquals(1, empleado.eventosPendientes().size());
		assertSame(luz, empleado.eventosPendientes().get(0));
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
